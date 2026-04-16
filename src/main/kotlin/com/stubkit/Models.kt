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
    @SerialName("id") val id: String,
    @SerialName("status") val status: EntitlementStatus,
    @SerialName("source") val source: EntitlementSource,
    @SerialName("platform") val platform: Platform,
    @SerialName("product_id") val productId: String,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class OfferingProduct(
    @SerialName("product_id") val productId: String,
    @SerialName("platform") val platform: Platform,
    @SerialName("period_days") val periodDays: Int? = null,
    @SerialName("price_usd_cents") val priceUsdCents: Int? = null,
    @SerialName("entitlement") val entitlement: String,
)

@Serializable
data class Offering(
    @SerialName("slug") val slug: String,
    @SerialName("title") val title: String,
    @SerialName("subtitle") val subtitle: String? = null,
    @SerialName("features") val features: List<String> = emptyList(),
    @SerialName("cta_label") val ctaLabel: String,
    @SerialName("locale") val locale: String? = null,
    @SerialName("products") val products: List<OfferingProduct> = emptyList(),
)

@Serializable
data class TrackResult(
    @SerialName("matched_rule_id") val matchedRuleId: String? = null,
    @SerialName("show_paywall") val showPaywall: Offering? = null,
)

class StubkitError(
    val code: String,
    override val message: String,
) : Exception("[$code] $message")
