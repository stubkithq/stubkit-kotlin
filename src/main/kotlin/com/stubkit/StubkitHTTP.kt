package com.stubkit

import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Two auth surfaces:
 *  - Publishable key (pk_live_...): /v1/offerings, /v1/events/track
 *  - Tenant JWT: /v1/entitlement, /v1/purchases, refresh
 * The caller picks which to use per request.
 */
internal class StubkitHTTP(
    private val publishableKey: String,
    private val getAuthToken: suspend () -> String,
    private val baseUrl: String,
) {
    private val client: HttpClient = HttpClient.newHttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    enum class Auth {
        PUBLISHABLE,
        TENANT_JWT,
    }

    companion object {
        private const val USER_AGENT = "stubkit-kotlin/1.0.1"
        private const val MAX_RETRIES = 2
        private val BACKOFF_MS = longArrayOf(250L, 500L)
    }

    suspend fun get(path: String, auth: Auth): JsonObject {
        return requestWithRetry("GET", path, body = null, auth = auth)
    }

    suspend fun post(path: String, body: String?, auth: Auth): JsonObject {
        return requestWithRetry("POST", path, body, auth)
    }

    private suspend fun bearer(auth: Auth): String = when (auth) {
        Auth.PUBLISHABLE -> publishableKey
        Auth.TENANT_JWT -> getAuthToken()
    }

    private suspend fun requestWithRetry(
        method: String,
        path: String,
        body: String?,
        auth: Auth,
    ): JsonObject {
        var lastException: Exception? = null

        for (attempt in 0..MAX_RETRIES) {
            try {
                val response = executeRequest(method, path, body, bearer(auth))
                val statusCode = response.statusCode()
                val responseBody = response.body() ?: ""

                if (statusCode in 500..599 && attempt < MAX_RETRIES) {
                    delay(BACKOFF_MS[attempt])
                    continue
                }

                return parseEnvelope(responseBody)
            } catch (e: StubkitError) {
                throw e
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES) {
                    delay(BACKOFF_MS[attempt])
                    continue
                }
            }
        }

        throw lastException ?: StubkitError("unknown", "Request failed after retries")
    }

    private fun executeRequest(
        method: String,
        path: String,
        body: String?,
        token: String,
    ): HttpResponse<String> {
        val url = "${baseUrl}${path}"
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)

        val request = when (method) {
            "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body ?: "{}")).build()
            else -> builder.GET().build()
        }

        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun parseEnvelope(responseBody: String): JsonObject {
        val envelope = json.parseToJsonElement(responseBody).jsonObject
        val success = envelope["success"]?.jsonPrimitive?.boolean ?: false

        if (!success) {
            val error = envelope["error"]?.jsonObject
            val code = error?.get("code")?.jsonPrimitive?.content ?: "unknown"
            val message = error?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
            throw StubkitError(code, message)
        }

        return envelope["data"]?.jsonObject
            ?: throw StubkitError("parse_error", "Missing data in response")
    }

    fun encodePath(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)
}
