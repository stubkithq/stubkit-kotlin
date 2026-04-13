package com.stubkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Platform {
    @SerialName("ios") IOS,
    @SerialName("android") ANDROID,
    @SerialName("web") WEB,
}

@Serializable
enum class EntitlementStatus {
    @SerialName("active") ACTIVE,
    @SerialName("grace") GRACE,
    @SerialName("expired") EXPIRED,
    @SerialName("cancelled") CANCELLED,
    @SerialName("refunded") REFUNDED,
}

@Serializable
enum class EntitlementSource {
    @SerialName("iap") IAP,
    @SerialName("stripe") STRIPE,
    @SerialName("admin_grant") ADMIN_GRANT,
    @SerialName("trial") TRIAL,
}

@Serializable
data class Entitlement(
    @SerialName("entitlement") val entitlement: String,
    @SerialName("status") val status: EntitlementStatus,
    @SerialName("source") val source: EntitlementSource,
    @SerialName("product_id") val productId: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("renewed_at") val renewedAt: String? = null,
    @SerialName("grace_expires_at") val graceExpiresAt: String? = null,
)

class StubkitError(
    val code: String,
    override val message: String,
) : Exception("[$code] $message")
