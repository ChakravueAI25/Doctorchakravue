# Play Store Deployment Requirements — DoctorChakravue

All items below must be resolved before submitting the `.aab` to the Play Store.
Items marked **BLOCKER** will cause rejection or a non-functional app in production.

---

## 1. Security — Must Fix Before Any Release

### 1.1 Rotate Firebase API Key and Remove from Git `[BLOCKER]`
- `composeApp/google-services.json` is committed to version control with a live API key.
- **Action:**
  1. Go to [Firebase Console → Project Settings → Service Accounts](https://console.firebase.google.com) and regenerate the Android API key.
  2. Add `google-services.json` to `.gitignore`.
  3. Inject the file at build time via CI environment secret (GitHub Actions, Bitrise, etc.).

```gitignore
# .gitignore — add this line
google-services.json
```

---

### 1.2 Add Authentication Headers to All API Calls `[BLOCKER]`
- **File:** `composeApp/src/commonMain/kotlin/com/org/doctorchakravue/data/ApiRepository.kt:34`
- Currently no `Authorization` header is attached to any request. All patient/submission/adherence data endpoints are publicly accessible.
- **Action:** Backend must return a signed JWT on login. Store it in `SessionManager` and attach it on every request:

```kotlin
// SessionManager.kt — add
fun saveToken(token: String) { settings["authToken"] = token }
fun getToken(): String = settings.getString("authToken", "")

// ApiRepository.kt — update defaultRequest block
defaultRequest {
    url(BuildConfig.API_BASE_URL)
    header(HttpHeaders.ContentType, ContentType.Application.Json)
    val token = sessionManager.getToken()
    if (token.isNotBlank()) header(HttpHeaders.Authorization, "Bearer $token")
}
```

---

### 1.3 Replace Hardcoded ngrok URL with Production URL `[BLOCKER]`
- **File:** `composeApp/src/commonMain/kotlin/com/org/doctorchakravue/data/ApiRepository.kt:35`
- `https://grovelingly-stey-armani.ngrok-free.dev` is a temporary development tunnel. It will stop working when the tunnel closes.
- **Action:** Use `BuildConfig` per build type:

```kotlin
// composeApp/build.gradle.kts
android {
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "API_BASE_URL", "\"https://your-ngrok-url.ngrok-free.dev\"")
        }
        getByName("release") {
            buildConfigField("String", "API_BASE_URL", "\"https://api.chakravue.co.in\"")
        }
    }
}

// ApiRepository.kt
url(BuildConfig.API_BASE_URL)
```

---

### 1.4 Disable Android Backup `[BLOCKER for Healthcare]`
- **File:** `composeApp/src/androidMain/AndroidManifest.xml:13`
- `android:allowBackup="true"` allows ADB/cloud backup of SharedPreferences, which contains the doctor's session credentials. Restoring this backup on another device grants authenticated access without login.

```xml
<!-- AndroidManifest.xml -->
<application
    android:allowBackup="false"
    ...>
```

---

### 1.5 Remove `println` Debug Statements That Leak Patient PII `[BLOCKER for Healthcare]`
- **File:** `composeApp/src/commonMain/kotlin/com/org/doctorchakravue/data/ApiRepository.kt:118–128`
- ProGuard strips `android.util.Log.d` but does **not** strip `println`. Patient names and emails are printed to logcat in release builds.
- **Action:** Delete all `println("DEBUG: ...")` calls in `ApiRepository.kt`. Replace any necessary logging with `Log.d(TAG, ...)`.

---

### 1.6 Encrypt Session Storage `[HIGH]`
- **File:** `composeApp/src/commonMain/kotlin/com/org/doctorchakravue/data/SessionManager.kt`
- Doctor ID, name, and email are stored in unencrypted `SharedPreferences`. Readable on rooted devices.
- **Action:** Switch to `multiplatform-settings-secure` which wraps `EncryptedSharedPreferences` on Android:

```toml
# libs.versions.toml
multiplatform-settings-secure = { module = "com.russhwolf:multiplatform-settings-secure", version.ref = "multiplatformSettings" }
```

```kotlin
// SessionManager.kt — Android actual
import com.russhwolf.settings.coroutines.SuspendSettings
import com.russhwolf.settings.secure.AndroidSecureSettings
private val settings = AndroidSecureSettings(context, masterKeyAlias, "doctor_prefs")
```

---

## 2. Build & Signing — Must Fix Before Upload

### 2.1 Add Release Signing Configuration `[BLOCKER]`
- **File:** `composeApp/build.gradle.kts`
- No `signingConfigs` block exists. Unsigned APK/AAB cannot be uploaded to the Play Store.
- **Action:** Generate a release keystore once and configure signing:

```bash
# Generate keystore (run once, store securely — NOT in git)
keytool -genkey -v -keystore release.keystore -alias doctorchakravue -keyalg RSA -keysize 2048 -validity 10000
```

```kotlin
// composeApp/build.gradle.kts
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS") ?: "doctorchakravue"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

> **Never commit the keystore or its passwords to git.** Add to `.gitignore`:
> ```
> *.keystore
> *.jks
> keystore.properties
> ```

---

### 2.2 Build AAB, Not APK `[BLOCKER]`
- Play Store requires Android App Bundle (`.aab`) for all new app submissions since August 2021.

```bash
# Correct command
./gradlew :composeApp:bundleRelease

# Output location
composeApp/build/outputs/bundle/release/composeApp-release.aab
```

---

### 2.3 Increment `versionCode` for Every Upload `[BLOCKER]`
- **File:** `composeApp/build.gradle.kts:107`
- `versionCode = 1` is hardcoded. Every Play Store upload requires a strictly incrementing integer. Uploading the same `versionCode` twice will be rejected.
- **Action:** Drive this from CI:

```kotlin
// build.gradle.kts
defaultConfig {
    versionCode = System.getenv("VERSION_CODE")?.toInt() ?: 1
    versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
}
```

---

### 2.4 Upgrade Navigation Compose from Alpha to Stable `[BLOCKER]`
- **File:** `gradle/libs.versions.toml`
- `navigation-compose` is pinned to `2.8.0-alpha10`. Alpha libraries have unstable APIs and known bugs. Play Store does not reject based on this, but alpha dependencies are not production-safe.

```toml
# libs.versions.toml — update to stable
androidx-navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version = "2.8.4" }
```

---

## 3. Login Flow — Fix Before Release

### 3.1 Add Email Format Validation
- **File:** `composeApp/src/commonMain/kotlin/com/org/doctorchakravue/ui/LoginScreen.kt:165`

```kotlin
// LoginScreen.kt — inside onClick, before viewModel.login(...)
if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
    showSnackbar("Please enter a valid email address")
    return@Button
}
```

---

### 3.2 Do Not Trim the Password
- **File:** `composeApp/src/commonMain/kotlin/com/org/doctorchakravue/ui/LoginScreen.kt:169`
- `password.trim()` silently strips whitespace and will fail for passwords that begin or end with a space.

```kotlin
// Before
viewModel.login(email.trim(), password.trim())

// After
viewModel.login(email.trim(), password)
```

---

### 3.3 Add Minimum Password Length Check

```kotlin
if (password.length < 6) {
    showSnackbar("Password must be at least 6 characters")
    return@Button
}
```

---

### 3.4 Add Login Attempt Rate Limiting
- After 3 consecutive failures, disable the login button for 30 seconds to prevent brute-force.

```kotlin
// LoginState — add
data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val failureCount: Int = 0,
    val lockedUntilMs: Long = 0L
)
```

---

### 3.5 Add Forgot Password Screen / Link
- There is no password recovery path. Add a "Forgot password?" `TextButton` below the login button that navigates to a reset flow or opens an email/webview to your support portal.

---

## 4. User Flow — Fix Before Release

### 4.1 Fix Navigation Route JSON Encoding `[BLOCKER — Crash Risk]`
- **File:** `composeApp/src/commonMain/kotlin/com/org/doctorchakravue/app/App.kt:92`
- Only `/` is escaped. Patient names or notes with `?`, `#`, `&`, `+`, or `%` will corrupt the navigation route and crash/silently drop navigation.

```kotlin
// App.kt — replace all manual .replace("/", "%2F") with:
import java.net.URLEncoder
import java.net.URLDecoder

// Encode
val encodedJson = URLEncoder.encode(Json.encodeToString(submission), "UTF-8")
navController.navigate("submission/$encodedJson")

// Decode (in composable)
val data = URLDecoder.decode(backStackEntry.arguments?.getString("data") ?: "", "UTF-8")
```

---

### 4.2 Fix Image URL Domain Inconsistency
- **File:** `composeApp/src/commonMain/kotlin/com/org/doctorchakravue/ui/DashboardScreen.kt:439`
- Image URLs are hardcoded to `https://doctor.chakravue.co.in/files/...` while the API base URL is a different domain (ngrok).

```kotlin
// DashboardScreen.kt — use BuildConfig
val imageUrl = "${BuildConfig.API_BASE_URL}/files/${submission.imageId}"
```

---

### 4.3 Fix Timezone Bug in "Today" Calculation
- **File:** `composeApp/src/commonMain/kotlin/com/org/doctorchakravue/ui/DashboardScreen.kt:635`
- The hand-rolled date calculation uses UTC epoch arithmetic. In IST (UTC+5:30), counts will be wrong after midnight UTC (5:30 AM IST) until midnight IST.
- **Action:** Replace with `kotlinx-datetime` (already a dependency):

```kotlin
import kotlinx.datetime.*

private fun getTodayDateString(): String =
    Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
```

---

### 4.4 Wire Up FCM Notifications for Video Calls and Submissions
- **File:** `composeApp/src/androidMain/kotlin/com/org/doctorchakravue/DoctorFirebaseMessagingService.kt:56`
- Incoming video call and new submission notifications are received but never shown to the user.
- **Action:** Post a `NotificationCompat.Builder` notification with a deep-link `PendingIntent`:

```kotlin
// DoctorFirebaseMessagingService.kt
private fun handleDataMessage(data: Map<String, String>) {
    val type = data["type"]
    val title = data["title"] ?: "ChakraVue"
    val body = data["body"] ?: ""

    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        if (type == "video_call") putExtra("deep_link", "video_call_list")
    }
    val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.app_icon)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
}
```

Also create a notification channel in `MainActivity.onCreate()` targeting Android 8+.

---

### 4.5 Fix Coroutine Scope Leaks
- **Files:** `DashboardScreen.kt:54`, `LoginScreen.kt:46`
- Both ViewModels create `CoroutineScope(Dispatchers.Main)` that is never cancelled.

```kotlin
// DashboardViewModel — update stopAutoRefresh
fun stopAutoRefresh() {
    autoRefreshJob?.cancel()
    autoRefreshJob = null
    scope.cancel() // ← add this
}
```

```kotlin
// LoginViewModel — add cancel method
fun onCleared() {
    scope.cancel()
}
// LoginScreen.kt — call in DisposableEffect
DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }
```

---

### 4.6 Fix "Today's Consultations" Hardcoded to Zero
- **File:** `composeApp/src/commonMain/kotlin/com/org/doctorchakravue/ui/DashboardScreen.kt:247`
- Either connect it to real API data or remove the card to avoid misleading doctors.

---

## 5. Manifest & Permissions — Fix Before Release

### 5.1 Add Network Security Config
- Create `composeApp/src/androidMain/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.chakravue.co.in</domain>
    </domain-config>
</network-security-config>
```

- Reference in `AndroidManifest.xml`:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

---

### 5.2 Declare `FOREGROUND_SERVICE_CAMERA` for Android 14+
- `targetSdk = 36`. Agora SDK video calling on Android 14+ requires this permission.

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
```

---

### 5.3 Add `VIBRATE` Permission for Notifications

```xml
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

`POST_NOTIFICATIONS` is required on Android 13+ (API 33+) for showing push notifications. Request it at runtime on first launch.

---

## 6. Play Store Listing Requirements

### 6.1 Privacy Policy `[BLOCKER]`
- Google requires a privacy policy URL for apps that handle personal/health data. You must publish one and link it in both the Play Console listing and the app itself (accessible without login).

### 6.2 App Content Rating `[BLOCKER]`
- Complete the IARC rating questionnaire in Play Console. Healthcare apps must answer accurately.

### 6.3 Data Safety Form `[BLOCKER]`
- Declare all data types collected (doctor name, email, patient submissions, FCM token) in the Play Console "Data safety" section.

### 6.4 Screenshots and Store Listing Assets
- Minimum: 2 phone screenshots (1080×1920 px minimum), 1 feature graphic (1024×500 px), high-res icon (512×512 px).
- App icon adaptive version is already configured.

### 6.5 Add Firebase Crashlytics
- No crash reporting is integrated. Production crashes will be invisible.

```toml
# libs.versions.toml
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics-ktx", version = "19.0.3" }
```

```kotlin
// build.gradle.kts plugins
id("com.google.firebase.crashlytics")
```

---

## 7. Quick Reference — Priority Order

| Priority | Item | File |
|----------|------|------|
| P0 | Rotate Firebase key, add to .gitignore | `google-services.json` |
| P0 | Add auth headers to all API calls | `ApiRepository.kt:34` |
| P0 | Replace ngrok URL with production URL | `ApiRepository.kt:35` |
| P0 | Add signing config + build AAB | `build.gradle.kts` |
| P0 | Fix nav route JSON encoding | `App.kt:92` |
| P1 | Disable allowBackup | `AndroidManifest.xml` |
| P1 | Remove println PII leaks | `ApiRepository.kt:118` |
| P1 | Encrypt session storage | `SessionManager.kt` |
| P1 | Wire FCM notifications | `DoctorFirebaseMessagingService.kt` |
| P1 | Upgrade navigation-compose to stable | `libs.versions.toml` |
| P1 | Add Privacy Policy + Data Safety form | Play Console |
| P2 | Email/password validation | `LoginScreen.kt` |
| P2 | Fix timezone date calculation | `DashboardScreen.kt:635` |
| P2 | Network security config | `AndroidManifest.xml` |
| P2 | FOREGROUND_SERVICE_CAMERA permission | `AndroidManifest.xml` |
| P2 | POST_NOTIFICATIONS permission | `AndroidManifest.xml` |
| P2 | Add Crashlytics | `build.gradle.kts` |
| P3 | Fix coroutine scope leaks | `DashboardScreen.kt`, `LoginScreen.kt` |
| P3 | Fix hardcoded image URL domain | `DashboardScreen.kt:439` |
| P3 | Wire Today's Consultations stat | `DashboardScreen.kt:247` |
| P3 | Add forgot password flow | `LoginScreen.kt` |
| P3 | versionCode from CI env var | `build.gradle.kts:107` |
