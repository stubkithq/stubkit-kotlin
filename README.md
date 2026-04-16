# Stubkit Kotlin SDK

Kotlin SDK for [Stubkit](https://stubkit.com) subscription validation API. Verify in-app purchases and manage entitlements from your Android or JVM backend.

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.stubkit:stubkit:1.0.1")
}
```

Or in Groovy `build.gradle`:

```gradle
implementation 'com.stubkit:stubkit:1.0.1'
```

## Quick Start

The SDK uses two auth inputs: a **publishable key** (safe to ship in the
app) for offering + event calls, and a **tenant JWT** callback that returns
the end-user's identity token for entitlement + purchase calls.

### 1. Configure

```kotlin
import com.stubkit.Stubkit

Stubkit.configure(
    publishableKey = "pk_live_xxxxxxxxxxxxxxxxxxxxxxxx",
    appId = "your-app-id",
    getAuthToken = {
        // Return your tenant JWT. Supabase access token / Clerk JWT /
        // Firebase ID token / your custom RS256 token.
        authProvider.currentAccessToken()
    }
)
```

### 2. Check entitlement

```kotlin
val isPro = Stubkit.shared.isActive("user_123", "pro")
if (isPro) {
    // Unlock pro features
}
```

### 3. Fetch paywall config

```kotlin
val offering = Stubkit.shared.getOffering()
offering.title          // "Unlock Pro"
offering.features       // ["Unlimited projects", "Priority support", ...]
offering.products       // [OfferingProduct(productId = ...), ...]
```

### 4. Sync purchase (after BillingClient purchase)

```kotlin
import com.stubkit.Platform

val entitlements = Stubkit.shared.syncPurchase(
    userId = "user_123",
    platform = Platform.ANDROID,
    productId = "com.myapp.pro",
    receipt = purchase.purchaseToken,
    purchaseToken = purchase.purchaseToken,
)
```

### 5. Track behavioural events (gets paywall suggestion back)

```kotlin
import kotlinx.serialization.json.JsonPrimitive

val result = Stubkit.shared.track(
    event = "hit_export_limit",
    userId = "user_123",
    properties = mapOf("count" to JsonPrimitive(5)),
)
result.showPaywall?.let { presentPaywall(it) }
```

### 6. Force refresh entitlements

```kotlin
val refreshed = Stubkit.shared.refresh("user_123")
```

## API Reference

### `Stubkit.configure(publishableKey, appId, getAuthToken, baseUrl?)`

Initialize the SDK. Call once at app startup.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `publishableKey` | `String` | Yes | `pk_live_…` / `pk_test_…` |
| `appId` | `String` | Yes | Your stubkit app identifier |
| `getAuthToken` | `suspend () -> String` | Yes | Returns your tenant JWT |
| `baseUrl` | `String` | No | API base URL (default: `https://api.stubkit.com`) |

### Entitlement & purchases (uses tenant JWT)

- `isActive(userId, entitlement): Boolean`
- `getEntitlements(userId): List<Entitlement>`
- `syncPurchase(userId, platform, productId, receipt, transactionId?, purchaseToken?): List<Entitlement>`
- `refresh(userId): List<Entitlement>`

### Paywalls & events (uses publishable key)

- `getOffering(slug = "default", locale = null): Offering`
- `track(event, userId, properties = emptyMap()): TrackResult`

## Models

### `Platform`
`IOS`, `ANDROID`, `WEB`

### `EntitlementStatus`
`ACTIVE`, `GRACE`, `EXPIRED`, `CANCELLED`, `REFUNDED`

### `Entitlement`
```kotlin
data class Entitlement(
    val id: String,
    val status: EntitlementStatus,
    val source: EntitlementSource,
    val platform: Platform,
    val productId: String,
    val expiresAt: String? = null,
)
```

### `Offering`
```kotlin
data class Offering(
    val slug: String,
    val title: String,
    val subtitle: String?,
    val features: List<String>,
    val ctaLabel: String,
    val locale: String?,
    val products: List<OfferingProduct>,
)
```

## Error handling

All methods throw `StubkitError` on failure:

```kotlin
try {
    val entitlements = Stubkit.shared.getEntitlements("user_123")
} catch (e: StubkitError) {
    println("Error: ${e.code} - ${e.message}")
}
```

`isActive` returns `false` instead of throwing on errors.

## Migrating from 1.0.0

The single-key `configure(apiKey, appId)` is gone. Replace with:

```kotlin
Stubkit.configure(
    publishableKey = "pk_live_x",
    appId = "app",
    getAuthToken = { token() },
)
```

Offering and event calls now use the publishable key automatically;
entitlement and purchase calls use the JWT from your `getAuthToken` closure.

## Requirements

- Kotlin 1.9+
- Java 11+
- Android API 21+ (when used on Android)

## Links

- [Documentation](https://docs.stubkit.com)
- [Tenant JWT setup](https://docs.stubkit.com/getting-started/tenant-jwt)
- [Dashboard](https://app.stubkit.com)

## License

MIT — Cryptosam LLC
