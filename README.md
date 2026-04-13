# Stubkit Kotlin SDK

Kotlin SDK for [Stubkit](https://stubkit.com) subscription validation API. Verify in-app purchases and manage entitlements from your Android or JVM backend.

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.stubkit:stubkit:1.0.0")
}
```

Or in Groovy `build.gradle`:

```gradle
implementation 'com.stubkit:stubkit:1.0.0'
```

## Quick Start

### 1. Configure

```kotlin
import com.stubkit.Stubkit

// In your Application.onCreate() or entry point
Stubkit.configure(apiKey = "pk_live_xxx", appId = "myapp")
```

### 2. Check entitlement

```kotlin
import com.stubkit.Stubkit

val isPro = Stubkit.shared.isActive("user_123", "pro")
if (isPro) {
    // Unlock pro features
}
```

### 3. Sync purchase (after BillingClient purchase)

```kotlin
import com.stubkit.Platform
import com.stubkit.Stubkit

val entitlements = Stubkit.shared.syncPurchase(
    userId = "user_123",
    platform = Platform.ANDROID,
    productId = "com.myapp.pro",
    receipt = purchase.purchaseToken
)
```

### 4. Get all entitlements

```kotlin
val entitlements = Stubkit.shared.getEntitlements("user_123")
for (e in entitlements) {
    println("${e.entitlement}: ${e.status}")
}
```

### 5. Force refresh

```kotlin
val refreshed = Stubkit.shared.refresh("user_123")
```

## API Reference

### `Stubkit.configure(apiKey, appId, baseUrl?)`

Initialize the SDK. Call once at app startup.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | `String` | Yes | Your Stubkit API key |
| `appId` | `String` | Yes | Your app identifier |
| `baseUrl` | `String` | No | API base URL (default: `https://api.stubkit.com`) |

### `Stubkit.shared.isActive(userId, entitlement): Boolean`

Returns `true` if the user has an active (or grace period) entitlement.

### `Stubkit.shared.getEntitlements(userId): List<Entitlement>`

Returns all entitlements for a user.

### `Stubkit.shared.syncPurchase(userId, platform, productId, receipt, transactionId?): List<Entitlement>`

Syncs a purchase receipt with Stubkit. Returns updated entitlements.

### `Stubkit.shared.refresh(userId): List<Entitlement>`

Force-refreshes entitlements from the server.

## Models

### `Platform`
`IOS`, `ANDROID`, `WEB`

### `EntitlementStatus`
`ACTIVE`, `GRACE`, `EXPIRED`, `CANCELLED`, `REFUNDED`

### `EntitlementSource`
`IAP`, `STRIPE`, `ADMIN_GRANT`, `TRIAL`

### `Entitlement`
```kotlin
data class Entitlement(
    val entitlement: String,
    val status: EntitlementStatus,
    val source: EntitlementSource,
    val productId: String,
    val expiresAt: String?,
    val renewedAt: String?,
    val graceExpiresAt: String?,
)
```

## Error Handling

All methods throw `StubkitError` on failure:

```kotlin
try {
    val entitlements = Stubkit.shared.getEntitlements("user_123")
} catch (e: StubkitError) {
    println("Error: ${e.code} - ${e.message}")
}
```

`isActive` returns `false` instead of throwing on errors.

## Requirements

- Kotlin 1.9+
- Java 11+
- Android API 21+ (when used on Android)

## License

MIT - Cryptosam LLC
