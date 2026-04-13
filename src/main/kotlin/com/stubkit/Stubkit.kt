package com.stubkit

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

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

        fun configure(
            apiKey: String,
            appId: String,
            baseUrl: String = "https://api.stubkit.com",
        ) {
            instance = Stubkit(
                http = StubkitHTTP(apiKey = apiKey, baseUrl = baseUrl),
                appId = appId,
            )
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun isActive(userId: String, entitlement: String): Boolean {
        return try {
            val data = http.get("/v1/apps/$appId/users/$userId/entitlements/$entitlement")
            val status = data["status"]?.let {
                json.decodeFromJsonElement(EntitlementStatus.serializer(), it)
            }
            status == EntitlementStatus.ACTIVE || status == EntitlementStatus.GRACE
        } catch (_: StubkitError) {
            false
        }
    }

    suspend fun getEntitlements(userId: String): List<Entitlement> {
        val data = http.get("/v1/apps/$appId/users/$userId/entitlements")
        val entitlementsJson = data["entitlements"]?.jsonArray ?: JsonArray(emptyList())
        return entitlementsJson.map { element ->
            json.decodeFromJsonElement(Entitlement.serializer(), element)
        }
    }

    suspend fun syncPurchase(
        userId: String,
        platform: Platform,
        productId: String,
        receipt: String,
        transactionId: String? = null,
    ): List<Entitlement> {
        val bodyMap = buildMap {
            put("user_id", userId)
            put("platform", json.encodeToString(platform).trim('"'))
            put("product_id", productId)
            put("receipt", receipt)
            if (transactionId != null) put("transaction_id", transactionId)
        }
        val body = json.encodeToString(bodyMap)
        val data = http.post("/v1/apps/$appId/purchases", body)
        val entitlementsJson = data["entitlements"]?.jsonArray ?: JsonArray(emptyList())
        return entitlementsJson.map { element ->
            json.decodeFromJsonElement(Entitlement.serializer(), element)
        }
    }

    suspend fun refresh(userId: String): List<Entitlement> {
        val data = http.post("/v1/apps/$appId/users/$userId/refresh", "{}")
        val entitlementsJson = data["entitlements"]?.jsonArray ?: JsonArray(emptyList())
        return entitlementsJson.map { element ->
            json.decodeFromJsonElement(Entitlement.serializer(), element)
        }
    }
}
