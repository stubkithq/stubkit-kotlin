package com.stubkit

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import java.util.Date
import java.text.SimpleDateFormat
import java.util.TimeZone

class Stubkit private constructor(
    private val http: StubkitHTTP,
    private val appId: String,
) {
    companion object {
        @Volatile
        private var instance: Stubkit? = null

        val shared: Stubkit
            get() = instance
                ?: throw StubkitError("not_configured", "Call Stubkit.configure() before accessing Stubkit.shared")

        /**
         * Configure the SDK. Call once at app launch.
         *
         * @param publishableKey pk_live_... or pk_test_... — safe to ship in the app.
         * @param appId Your stubkit app identifier.
         * @param getAuthToken Suspend function returning your tenant JWT. Called per request.
         * @param baseUrl Override for staging / self-hosted.
         */
        fun configure(
            publishableKey: String,
            appId: String,
            getAuthToken: suspend () -> String,
            baseUrl: String = "https://api.stubkit.com",
        ) {
            instance = Stubkit(
                http = StubkitHTTP(
                    publishableKey = publishableKey,
                    getAuthToken = getAuthToken,
                    baseUrl = baseUrl,
                ),
                appId = appId,
            )
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val iso: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** Check if a user has an active entitlement. */
    suspend fun isActive(userId: String, entitlement: String): Boolean {
        val match = getEntitlements(userId).firstOrNull { it.id == entitlement } ?: return false
        return when (match.status) {
            EntitlementStatus.ACTIVE, EntitlementStatus.GRACE -> true
            EntitlementStatus.CANCELLED -> {
                val exp = match.expiresAt?.let { runCatching { iso.parse(it) }.getOrNull() }
                exp != null && exp.after(Date())
            }
            else -> false
        }
    }

    /** Get all entitlements for a user. Uses the tenant JWT. */
    suspend fun getEntitlements(userId: String): List<Entitlement> {
        val path = "/v1/entitlement/$appId/${http.encodePath(userId)}"
        val data = http.get(path, StubkitHTTP.Auth.TENANT_JWT)
        val list = data["entitlements"]?.jsonArray ?: JsonArray(emptyList())
        return list.map { json.decodeFromJsonElement(Entitlement.serializer(), it) }
    }

    /** Force refresh entitlements (bypass cache). Uses the tenant JWT. */
    suspend fun refresh(userId: String): List<Entitlement> {
        val path = "/v1/entitlement/$appId/${http.encodePath(userId)}/refresh"
        val data = http.post(path, body = null, auth = StubkitHTTP.Auth.TENANT_JWT)
        val list = data["entitlements"]?.jsonArray ?: JsonArray(emptyList())
        return list.map { json.decodeFromJsonElement(Entitlement.serializer(), it) }
    }

    /** Submit a purchase receipt. Uses the tenant JWT. */
    suspend fun syncPurchase(
        userId: String,
        platform: Platform,
        productId: String,
        receipt: String,
        transactionId: String? = null,
        purchaseToken: String? = null,
    ): List<Entitlement> {
        val body = buildJsonObject {
            put("app_id", appId)
            put("user_id", userId)
            put("platform", json.encodeToJsonElement(Platform.serializer(), platform))
            put("product_id", productId)
            put("receipt", receipt)
            if (transactionId != null) put("transaction_id", transactionId)
            if (purchaseToken != null) put("purchase_token", purchaseToken)
        }
        val data = http.post(
            path = "/v1/purchases",
            body = json.encodeToString(body),
            auth = StubkitHTTP.Auth.TENANT_JWT,
        )
        val list = data["entitlements"]?.jsonArray ?: JsonArray(emptyList())
        return list.map { json.decodeFromJsonElement(Entitlement.serializer(), it) }
    }

    /** Fetch paywall config. Uses the publishable key. */
    suspend fun getOffering(slug: String = "default", locale: String? = null): Offering {
        val query = if (locale != null) "?locale=${http.encodePath(locale)}" else ""
        val path = "/v1/offerings/$appId/$slug$query"
        val data = http.get(path, StubkitHTTP.Auth.PUBLISHABLE)
        return json.decodeFromJsonElement(Offering.serializer(), data)
    }

    /** Record a behavioural event. Uses the publishable key. */
    suspend fun track(
        event: String,
        userId: String,
        properties: Map<String, JsonElement> = emptyMap(),
    ): TrackResult {
        val body = buildJsonObject {
            put("event", event)
            put("user_id", userId)
            put("properties", JsonObject(properties))
        }
        val data = http.post(
            path = "/v1/events/track",
            body = json.encodeToString(body),
            auth = StubkitHTTP.Auth.PUBLISHABLE,
        )
        return json.decodeFromJsonElement(TrackResult.serializer(), data)
    }
}
