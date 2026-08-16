# CacheSweep — Technical Specification & Android Studio Architecture

**Document version:** 1.0
**Product:** CacheSweep
**Platform:** Android 11+
**Primary implementation:** Kotlin + Jetpack Compose
**Privileged backend:** Shizuku / ADB shell
**Distribution:** Personal sideloaded APK
**Status:** Implementation-ready technical specification

---

# 1. Technical Objective

CacheSweep is an Android storage utility that:

1. Enumerates installed applications.
2. Measures application cache/storage usage.
3. Identifies applications consuming large amounts of cache.
4. Uses Shizuku to perform privileged cache-cleaning operations.
5. Supports selective per-app cache clearing when the device exposes Android's `--cache-only` package-manager command.
6. Falls back to Android's global cache-trimming operation when selective cleaning is unavailable.
7. Measures storage before and after cleaning.
8. Never intentionally clears full application data.
9. Never exposes arbitrary shell execution.
10. Works without root.

The architecture must assume Android behavior differs by Android version and OEM.

Therefore, privileged capabilities must be **detected at runtime**, not assumed from Android version alone.

---

# 2. Important Change From the PRD

The original PRD treated automatic per-app cache deletion as a possible future feature.

The technical investigation changes that decision.

Current AOSP Android 16 exposes:

```text
pm clear --cache-only PACKAGE
```

The corresponding package-manager implementation invokes cache-file deletion rather than the full application-data clearing operation.

Therefore CacheSweep will support two cleaning modes.

## Mode A — Selective cache clear

Preferred when supported:

```text
pm clear --user <USER_ID> --cache-only <PACKAGE>
```

This allows CacheSweep to clear cache for selected applications.

## Mode B — Global cache trimming

Fallback:

```text
pm trim-caches <DESIRED_FREE_SPACE>
```

Android defines `trim-caches` as an operation that removes cache files in an attempt to reach the requested amount of free storage.

## Runtime rule

CacheSweep must **never infer support solely from Android version**.

At runtime:

```text
Shizuku connected
        |
        v
Inspect package-manager capabilities
        |
        +-- --cache-only available
        |       |
        |       v
        |  Enable selective cleaning
        |
        +-- not available
                |
                v
          Enable global trim only
```

---

# 3. Supported Android Versions

Initial product support:

```text
minSdk       = 30    // Android 11
targetSdk    = 36    // Android 16
compileSdk   = 37
```

`compileSdk 37` is recommended because the current Compose 1.12 generation requires API 37 compilation, while `targetSdk 36` keeps the application's runtime target aligned with Android 16 for the initial release.

Minimum supported Android:

**Android 11 / API 30**

This also aligns well with Shizuku's non-root Wireless Debugging setup, which is available directly on-device from Android 11 onward.

---

# 4. Current Build Toolchain

Recommended initial toolchain:

```text
Android Gradle Plugin     9.3.1
Gradle                    9.5
JDK                       17
Kotlin                    2.3.21
Compose BOM               2026.08.00
Shizuku API               13.1.5
```

AGP 9.3 supports API 37 and requires JDK 17; its documented Gradle baseline is 9.5. AGP 9+ also provides built-in Kotlin support, so the traditional `org.jetbrains.kotlin.android` plugin is no longer required.

The current Shizuku repository identifies API version 13.1.5.

---

# 5. Primary Dependencies

Recommended runtime dependencies:

```text
androidx.core:core-ktx:1.18.0
androidx.activity:activity-compose:1.13.0

androidx.lifecycle:lifecycle-runtime-compose:2.11.0
androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0

androidx.navigation:navigation-compose:2.9.8

androidx.datastore:datastore-preferences:1.2.1

org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0

dev.rikka.shizuku:api:13.1.5
dev.rikka.shizuku:provider:13.1.5

Compose BOM: 2026.08.00
```

These versions reflect the stable AndroidX/Compose releases current for this specification.

---

# 6. Root `build.gradle.kts`

```kotlin
plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
```

No separate:

```kotlin
id("org.jetbrains.kotlin.android")
```

is required with the proposed AGP 9.x configuration.

---

# 7. Application `build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.cachesweep.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.cachesweep.app"

        minSdk = 30
        targetSdk = 36

        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(
        platform("androidx.compose:compose-bom:2026.08.00")
    )

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.11.0"
    )
    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0"
    )

    implementation(
        "androidx.navigation:navigation-compose:2.9.8"
    )

    implementation(
        "androidx.datastore:datastore-preferences:1.2.1"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0"
    )

    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    testImplementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0"
    )

    androidTestImplementation(
        platform("androidx.compose:compose-bom:2026.08.00")
    )

    androidTestImplementation(
        "androidx.compose.ui:ui-test-junit4"
    )

    debugImplementation(
        "androidx.compose.ui:ui-test-manifest"
    )
}
```

---

# 8. Android Manifest

Proposed `AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>

<manifest
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Required for StorageStats access to other apps. -->
    <uses-permission
        android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions" />

    <!-- Personal/sideloaded utility needs broad app discovery. -->
    <uses-permission
        android:name="android.permission.QUERY_ALL_PACKAGES" />

    <application
        android:name=".CacheSweepApp"
        android:allowBackup="false"
        android:label="CacheSweep"
        android:supportsRtl="true"
        android:theme="@style/Theme.CacheSweep">

        <activity
            android:name=".MainActivity"
            android:exported="true">

            <intent-filter>

                <action
                    android:name="android.intent.action.MAIN" />

                <category
                    android:name="android.intent.category.LAUNCHER" />

            </intent-filter>

        </activity>

        <provider
            android:name="rikka.shizuku.ShizukuProvider"
            android:authorities="${applicationId}.shizuku"
            android:multiprocess="false"
            android:enabled="true"
            android:exported="true"
            android:permission=
                "android.permission.INTERACT_ACROSS_USERS_FULL" />

    </application>

</manifest>
```

Shizuku's official integration uses its provider plus runtime Binder/permission APIs.

Android also filters installed-package visibility for apps targeting Android 11+, which is why package visibility must be explicitly addressed.

For this personal sideloaded tool, `QUERY_ALL_PACKAGES` is the simplest approach.

A future Play Store version would require reconsidering that choice.

---

# 9. No Internet Permission

The manifest deliberately must not contain:

```xml
<uses-permission
    android:name="android.permission.INTERNET" />
```

The MVP requires no network access.

This makes the privacy claim technically verifiable:

> CacheSweep cannot send package/storage information over the network using normal Android networking APIs because the application does not request internet access.

---

# 10. Project Structure

Recommended initial repository:

```text
CacheSweep/
│
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   └── libs.versions.toml
│
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    │
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   │
        │   ├── aidl/
        │   │   └── dev/cachesweep/app/shizuku/
        │   │       └── ICacheOpsService.aidl
        │   │
        │   └── java/dev/cachesweep/app/
        │       │
        │       ├── CacheSweepApp.kt
        │       ├── MainActivity.kt
        │       │
        │       ├── di/
        │       │   └── AppContainer.kt
        │       │
        │       ├── model/
        │       │   ├── AppCacheInfo.kt
        │       │   ├── DeviceStorageInfo.kt
        │       │   ├── ScanResult.kt
        │       │   ├── CleanerCapabilities.kt
        │       │   └── CleanupResult.kt
        │       │
        │       ├── permissions/
        │       │   └── UsageAccessManager.kt
        │       │
        │       ├── scanner/
        │       │   ├── CacheScanner.kt
        │       │   └── AndroidCacheScanner.kt
        │       │
        │       ├── storage/
        │       │   ├── DeviceStorageRepository.kt
        │       │   └── StorageStatsRepository.kt
        │       │
        │       ├── shizuku/
        │       │   ├── ShizukuManager.kt
        │       │   ├── CacheOpsUserService.kt
        │       │   └── CapabilityProbe.kt
        │       │
        │       ├── cleaner/
        │       │   ├── CacheCleaner.kt
        │       │   ├── ShizukuCacheCleaner.kt
        │       │   └── CleanupCoordinator.kt
        │       │
        │       ├── data/
        │       │   ├── SettingsRepository.kt
        │       │   └── CleanupHistoryRepository.kt
        │       │
        │       ├── util/
        │       │   ├── ByteFormatter.kt
        │       │   └── PackageValidator.kt
        │       │
        │       └── ui/
        │           ├── navigation/
        │           ├── components/
        │           ├── theme/
        │           └── screens/
        │
        ├── test/
        └── androidTest/
```

A single Gradle module is intentionally recommended for v1.

The logical layers can become physical modules later if needed.

---

# 11. Architecture

Use:

```text
Compose UI
    |
    v
ViewModel
    |
    v
Use case / coordinator
    |
    +------------------------+
    |                        |
    v                        v
Storage repositories    CacheCleaner
                             |
                             v
                       Shizuku bridge
                             |
                             v
                        UserService
                             |
                             v
                  Android package manager
```

Architectural principles:

* UI never executes privileged commands.
* ViewModels never construct shell commands.
* Application-facing APIs are strongly typed.
* Shizuku implementation is replaceable.
* Cache scanning and cache cleaning are independent.
* Cleaning remains unavailable if privilege checks fail.
* Failure to measure one app must not fail the entire scan.

---

# 12. Dependency Injection

For the MVP, use simple manual dependency injection rather than Hilt.

Example:

```kotlin
class AppContainer(
    context: Context
) {

    val usageAccessManager =
        UsageAccessManager(context)

    val storageStatsRepository =
        StorageStatsRepository(context)

    val deviceStorageRepository =
        DeviceStorageRepository()

    val shizukuManager =
        ShizukuManager(context)

    val cacheScanner: CacheScanner =
        AndroidCacheScanner(
            context = context,
            storageStatsRepository =
                storageStatsRepository
        )

    val cacheCleaner: CacheCleaner =
        ShizukuCacheCleaner(
            shizukuManager = shizukuManager
        )
}
```

This keeps the MVP small while preserving testability.

---

# 13. Core Models

## Application cache information

```kotlin
data class AppCacheInfo(
    val packageName: String,
    val appName: String,

    val cacheBytes: Long,
    val appBytes: Long,
    val dataBytes: Long,

    val isSystemApp: Boolean,
    val measurementAvailable: Boolean
) {
    val totalBytes: Long
        get() = cacheBytes + appBytes + dataBytes
}
```

---

## Device storage

```kotlin
data class DeviceStorageInfo(
    val totalBytes: Long,
    val availableBytes: Long
) {
    val usedBytes: Long
        get() = totalBytes - availableBytes
}
```

---

## Scan result

```kotlin
data class ScanResult(
    val apps: List<AppCacheInfo>,
    val attemptedApps: Int,
    val successfulApps: Int,
    val totalReportedCacheBytes: Long,
    val durationMillis: Long
)
```

---

# 14. Cleaner Capabilities

Capabilities must be modeled explicitly.

```kotlin
data class CleanerCapabilities(
    val shizukuAvailable: Boolean,
    val shizukuAuthorized: Boolean,
    val privilegedUid: Int?,
    val supportsSelectiveCacheClear: Boolean,
    val supportsGlobalTrim: Boolean
)
```

Expected Shizuku UID:

```text
2000 = ADB shell
0    = root
```

Shizuku exposes the identity of its backend; on the non-root configuration used by CacheSweep, the service runs with shell identity.

CacheSweep does not require UID 0.

---

# 15. Cleanup Modes

```kotlin
enum class CleanupMode {
    SELECTIVE,
    GLOBAL_TRIM
}
```

Plan model:

```kotlin
data class CleanupPlan(
    val mode: CleanupMode,
    val selectedPackages: List<String>,
    val estimatedCacheBytes: Long
)
```

---

# 16. Cache Scanner Interface

```kotlin
interface CacheScanner {

    suspend fun scan(): ScanResult

    suspend fun scanPackage(
        packageName: String
    ): AppCacheInfo?
}
```

Implementation:

```text
AndroidCacheScanner
        |
        +-- PackageManager
        |
        +-- StorageStatsManager
```

---

# 17. StorageStats Implementation

Android's `StorageStatsManager` exposes package storage statistics, including cache usage. Querying statistics for other applications requires Usage Access, and package queries can be relatively expensive, so the operation should run away from the main thread.

Conceptual implementation:

```kotlin
class StorageStatsRepository(
    context: Context
) {

    private val manager =
        context.getSystemService(
            StorageStatsManager::class.java
        )

    fun query(
        packageName: String,
        storageUuid: UUID,
        userHandle: UserHandle
    ): StorageStats {

        return manager.queryStatsForPackage(
            storageUuid,
            packageName,
            userHandle
        )
    }
}
```

Usage:

```kotlin
val stats = repository.query(
    packageName = applicationInfo.packageName,
    storageUuid =
        applicationInfo.storageUuid
            ?: StorageManager.UUID_DEFAULT,
    userHandle = Process.myUserHandle()
)

val result = AppCacheInfo(
    packageName = applicationInfo.packageName,
    appName = label,
    cacheBytes = stats.cacheBytes,
    appBytes = stats.appBytes,
    dataBytes = stats.dataBytes,
    isSystemApp = isSystem,
    measurementAvailable = true
)
```

---

# 18. Scanner Concurrency

Do not launch hundreds of package queries simultaneously.

Recommended concurrency:

```text
4–8 package queries
```

at a time.

Example concept:

```kotlin
val semaphore = Semaphore(6)

packages.map { app ->
    async(Dispatchers.IO) {

        semaphore.withPermit {
            scanOne(app)
        }
    }
}.awaitAll()
```

Any individual exception should produce:

```kotlin
measurementAvailable = false
```

rather than terminate the scan.

---

# 19. Usage Access

Manifest declaration alone does not grant Usage Access.

The user must enable CacheSweep through Android Settings.

Provide:

```kotlin
interface UsageAccessManager {

    fun hasAccess(): Boolean

    fun createSettingsIntent(): Intent
}
```

Settings intent:

```kotlin
Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
```

The setup screen should explain:

```text
CacheSweep uses Usage Access only to obtain
storage statistics for installed applications.

This information remains on your device.
```

---

# 20. Device Storage Measurement

This is an important correction to the original PRD.

`StorageStatsManager.getFreeBytes()` includes not only unused blocks but cached storage Android considers reclaimable. Therefore it should **not** be treated as the physical before/after measurement for cleanup success.

For physical available-space snapshots, use:

```kotlin
StatFs(
    Environment.getDataDirectory().absolutePath
)
```

Example:

```kotlin
class DeviceStorageRepository {

    fun snapshot(): DeviceStorageInfo {

        val statFs = StatFs(
            Environment
                .getDataDirectory()
                .absolutePath
        )

        return DeviceStorageInfo(
            totalBytes = statFs.totalBytes,
            availableBytes = statFs.availableBytes
        )
    }
}
```

This produces the primary:

```text
Before available storage
After available storage
```

comparison.

---

# 21. Cleanup Measurement

Take two separate measurements.

## Physical storage delta

```kotlin
val physicalDelta =
    maxOf(
        0,
        after.availableBytes -
            before.availableBytes
    )
```

## Reported cache delta

```kotlin
val cacheDelta =
    maxOf(
        0,
        beforeReportedCache -
            afterReportedCache
    )
```

These values should not be expected to match exactly.

---

# 22. Cleanup Result

```kotlin
data class CleanupResult(
    val startedAtMillis: Long,

    val physicalFreeBefore: Long,
    val physicalFreeAfter: Long,

    val cacheBefore: Long,
    val cacheAfter: Long,

    val attemptedPackages: Int,
    val successfulPackages: Int,
    val failedPackages: List<String>
) {

    val measuredFreedBytes: Long
        get() = maxOf(
            0,
            physicalFreeAfter -
                physicalFreeBefore
        )

    val reportedCacheReduction: Long
        get() = maxOf(
            0,
            cacheBefore -
                cacheAfter
        )
}
```

---

# 23. Shizuku Lifecycle

CacheSweep must handle Shizuku as an external runtime capability.

States:

```kotlin
sealed interface ShizukuState {

    data object NotRunning :
        ShizukuState

    data object PermissionRequired :
        ShizukuState

    data object Connecting :
        ShizukuState

    data class Ready(
        val uid: Int
    ) : ShizukuState

    data class Error(
        val reason: String
    ) : ShizukuState
}
```

Shizuku provides Binder-received/dead listeners and explicit permission APIs; the app must only use Shizuku APIs once the Binder is available.

---

# 24. Shizuku Permission Flow

Conceptually:

```kotlin
when {
    Shizuku.isPreV11() -> {
        // compatibility handling if needed
    }

    Shizuku.checkSelfPermission() ==
        PackageManager.PERMISSION_GRANTED -> {
        connect()
    }

    else -> {
        Shizuku.requestPermission(
            SHIZUKU_PERMISSION_REQUEST
        )
    }
}
```

Listen for permission results using Shizuku's permission-result listener.

---

# 25. Why Use a Shizuku UserService

Shizuku can run application-defined code under shell/root identity using its `UserService` mechanism. Its older direct process API is deprecated, making a UserService the better architecture for the privileged backend.

Architecture:

```text
CacheSweep process
       |
       | Binder/AIDL
       v
CacheOpsUserService
       |
       | shell identity
       v
/system/bin/pm
```

Benefits:

* UI process stays unprivileged.
* Commands are isolated.
* Privileged interface can be tiny.
* No generic terminal is exposed.
* Backend can be replaced later.

---

# 26. Privileged AIDL Interface

The privileged contract should be deliberately small.

Example:

```aidl
package dev.cachesweep.app.shizuku;

interface ICacheOpsService {

    int getProtocolVersion();

    int getPrivilegedUid();

    boolean supportsSelectiveCacheClear();

    boolean supportsGlobalTrim();

    int clearPackageCache(
        String packageName,
        int userId
    );

    int trimCaches(
        long desiredFreeBytes
    );

    String getLastError();
}
```

Important:

There must not be:

```aidl
String execute(String command);
```

or:

```aidl
int shell(String args);
```

The application should never expose an arbitrary privileged shell.

---

# 27. Privileged UserService

Conceptual skeleton:

```kotlin
class CacheOpsUserService :
    ICacheOpsService.Stub() {

    override fun getProtocolVersion(): Int =
        1

    override fun getPrivilegedUid(): Int =
        Process.myUid()

    override fun supportsSelectiveCacheClear():
        Boolean {

        return CapabilityProbe
            .supportsCacheOnlyClear()
    }

    override fun supportsGlobalTrim():
        Boolean {

        return CapabilityProbe
            .supportsTrimCaches()
    }

    override fun clearPackageCache(
        packageName: String,
        userId: Int
    ): Int {

        require(
            PackageValidator.isValid(
                packageName
            )
        )

        return PackageCommands
            .clearCache(
                packageName,
                userId
            )
            .exitCode
    }

    override fun trimCaches(
        desiredFreeBytes: Long
    ): Int {

        require(desiredFreeBytes >= 0)

        return PackageCommands
            .trimCaches(
                desiredFreeBytes
            )
            .exitCode
    }
}
```

---

# 28. Never Use `sh -c`

Avoid:

```kotlin
ProcessBuilder(
    "sh",
    "-c",
    "pm clear --cache-only $packageName"
)
```

Instead use argument arrays:

```kotlin
ProcessBuilder(
    "/system/bin/pm",
    "clear",
    "--user",
    userId.toString(),
    "--cache-only",
    packageName
)
```

This prevents package names from being interpreted as shell syntax.

---

# 29. Package Validation

Only clean packages that:

1. Were returned by Android's PackageManager.
2. Exist in the latest scan.
3. Are not CacheSweep itself.
4. Have a valid package identifier.

Do not trust package names arriving from:

* Intents.
* Deep links.
* Clipboard.
* External IPC.
* User text input.

There is no reason for arbitrary package input in the MVP.

---

# 30. Most Important Safety Invariant

The application must **never issue**:

```text
pm clear PACKAGE
```

without:

```text
--cache-only
```

The normal `pm clear PACKAGE` path deletes application user data; the cache-only option selects Android's cache-file deletion path instead.

This must have both:

* Runtime guards.
* Automated tests.

Example:

```kotlin
check(
    args.contains("--cache-only")
) {
    "Refusing unsafe pm clear command"
}
```

---

# 31. Capability Probe

Run a capability probe once after connecting to the privileged backend.

Conceptual logic:

```text
pm help
   |
   +-- contains "clear" and "--cache-only"
   |         |
   |         v
   |      selective = true
   |
   +-- contains "trim-caches"
             |
             v
          global = true
```

Cache this result for the current Shizuku session.

Re-run it after:

* Device reboot.
* Shizuku restart.
* Android system update.

Do not hardcode:

```text
Android >= 16 means supported
```

because OEM package-manager behavior may differ.

---

# 32. Selective Cleaner

Interface:

```kotlin
interface CacheCleaner {

    suspend fun capabilities():
        CleanerCapabilities

    suspend fun clearPackages(
        packages: List<String>
    ): CleanupResult

    suspend fun trimGlobally(
        desiredFreeBytes: Long
    ): CleanupResult
}
```

Selective flow:

```text
selected packages
       |
       v
validate Shizuku
       |
       v
verify --cache-only capability
       |
       v
take before snapshot
       |
       v
clear app 1
       |
       v
clear app 2
       |
      ...
       |
       v
wait for storage stats to settle
       |
       v
rescan
       |
       v
result
```

---

# 33. Selective Cleaning Progress

Selective cleaning has genuine progress.

Example:

```text
Cleaning cache

12 of 37 apps

Chrome
```

State:

```kotlin
data class CleaningProgress(
    val current: Int,
    val total: Int,
    val currentAppName: String?
)
```

Do not estimate a percentage based on bytes.

Use package count.

---

# 34. Global Trim Fallback

If selective cache clearing is unavailable:

```text
Global cache trim
```

Use:

```text
pm trim-caches DESIRED_FREE_SPACE
```

The number is a desired free-storage target, not "number of bytes to delete."

Possible target calculation:

```kotlin
val desiredFree =
    minOf(
        device.totalBytes,
        device.availableBytes +
            estimatedCacheBytes
    )
```

This asks Android to attempt to increase free storage roughly by the reported cache amount.

Actual reclaimed storage may be smaller.

---

# 35. Do Not Silently Change Cleaning Mode

Suppose the user selected:

```text
Chrome
Instagram
YouTube
```

but selective cleaning becomes unavailable.

Do not silently switch to:

```text
clean every application's cache
```

Instead show:

```text
Selective cleaning isn't available
on this Android build.

CacheSweep can ask Android to trim
caches across the device instead.

[ Cancel ]

[ Clean Device Cache ]
```

This preserves user intent.

---

# 36. Post-Cleanup Settling

Storage statistics may not update instantly.

Recommended process:

```text
cleaning complete
      |
      v
snapshot physical storage
      |
      v
wait ~500 ms
      |
      v
query cache stats
      |
      v
if still changing:
wait ~1.5 sec
      |
      v
final query
```

Use a small bounded retry strategy.

Do not create fake progress during this period.

UI:

```text
Updating storage information…
```

---

# 37. Noise Threshold

Physical storage can change while other applications run.

Define a small threshold, for example:

```text
16 MB
```

If measured delta is below the threshold:

```text
Cleanup completed.

No significant change in available
storage could be measured.
```

Do not show:

```text
0.003 GB freed
```

---

# 38. Compose Navigation

Use a single Activity:

```text
MainActivity
    |
    v
NavHost
```

Routes:

```kotlin
sealed class Route(
    val value: String
) {

    data object Setup :
        Route("setup")

    data object Dashboard :
        Route("dashboard")

    data object Apps :
        Route("apps")

    data object AppDetail :
        Route("app/{packageName}")

    data object Cleaning :
        Route("cleaning")

    data object Result :
        Route("result")

    data object Settings :
        Route("settings")
}
```

---

# 39. ViewModels

Recommended:

```text
SetupViewModel

DashboardViewModel

AppsViewModel

CleanerViewModel

SettingsViewModel
```

Each exposes immutable state using:

```kotlin
StateFlow<UiState>
```

and accepts explicit events.

Example:

```kotlin
sealed interface AppsEvent {

    data class SearchChanged(
        val value: String
    ) : AppsEvent

    data class ToggleSelected(
        val packageName: String
    ) : AppsEvent

    data class SortChanged(
        val sort: AppSort
    ) : AppsEvent

    data object Refresh :
        AppsEvent
}
```

---

# 40. Dashboard State

```kotlin
data class DashboardUiState(
    val loading: Boolean = true,

    val storage:
        DeviceStorageInfo? = null,

    val totalCacheBytes:
        Long = 0,

    val scannedApps:
        Int = 0,

    val largestApps:
        List<AppCacheInfo> =
            emptyList(),

    val shizukuState:
        ShizukuState =
            ShizukuState.NotRunning,

    val cleanerCapabilities:
        CleanerCapabilities? = null
)
```

---

# 41. Apps Screen State

```kotlin
data class AppsUiState(
    val apps: List<AppCacheInfo> =
        emptyList(),

    val query: String = "",

    val sort: AppSort =
        AppSort.CACHE_DESC,

    val selected:
        Set<String> =
            emptySet(),

    val supportsSelectiveCleaning:
        Boolean = false
)
```

---

# 42. Sorting

```kotlin
enum class AppSort {
    CACHE_DESC,
    TOTAL_DESC,
    NAME_ASC
}
```

Default:

```text
CACHE_DESC
```

---

# 43. Selective Cleaning UX

If supported:

```text
App Cache

Select apps                         3 selected

[x] Instagram                     1.42 GB
[x] Chrome                        1.08 GB
[ ] Spotify                        812 MB
[x] YouTube                        711 MB
[ ] Maps                           384 MB

Selected cache

3.21 GB

[ CLEAR SELECTED CACHE ]
```

The button should say:

> Clear selected cache

not:

> Free 3.21 GB

because cache size does not equal guaranteed reclaimed space.

---

# 44. App Detail

```text
Instagram

Cache
1.42 GB

App
312 MB

Other data
2.12 GB

Total
3.86 GB

Cache contains temporary files that
the application can usually recreate.

[ Clear Cache ]

[ Open Android Storage Settings ]
```

Show **Clear Cache** only if:

```text
supportsSelectiveCacheClear == true
```

Otherwise:

```text
[ Open Android Storage Settings ]
```

only.

---

# 45. Cleaning State Machine

```kotlin
sealed interface CleaningState {

    data object Idle :
        CleaningState

    data object Validating :
        CleaningState

    data object SnapshotBefore :
        CleaningState

    data class Clearing(
        val current: Int,
        val total: Int,
        val currentPackage: String?
    ) : CleaningState

    data object WaitingForStats :
        CleaningState

    data object SnapshotAfter :
        CleaningState

    data class Completed(
        val result: CleanupResult
    ) : CleaningState

    data class Failed(
        val message: String
    ) : CleaningState
}
```

This state machine should be owned by `CleanerViewModel` or a `CleanupCoordinator`.

---

# 46. Cleanup Coordinator

```kotlin
class CleanupCoordinator(
    private val cleaner: CacheCleaner,
    private val scanner: CacheScanner,
    private val storage:
        DeviceStorageRepository
) {

    suspend fun clean(
        plan: CleanupPlan,
        onProgress:
            suspend (CleaningState) -> Unit
    ): CleanupResult {
        // implementation
    }
}
```

Responsibilities:

1. Validate privileges.
2. Validate capability.
3. Capture pre-clean snapshot.
4. Perform cleaning.
5. Handle partial failures.
6. Wait for stats.
7. Rescan.
8. Capture post-clean snapshot.
9. Produce result.

---

# 47. Partial Failures

Selective cleaning should continue if one application fails.

Example result:

```text
Cleanup complete

2.3 GB freed

34 apps cleaned
3 apps couldn't be cleaned

Failed:

Google Play services
System UI
Vendor Security Service

[ Done ]
```

The user should be able to inspect failed packages in an expandable section.

---

# 48. Settings Persistence

Use DataStore Preferences.

Settings:

```kotlin
data class UserSettings(
    val showSystemApps: Boolean,
    val showZeroCacheApps: Boolean,
    val sortMode: AppSort,
    val themeMode: ThemeMode
)
```

No database is required for the initial MVP unless cleanup history becomes important.

A short cleanup history can initially also be serialized into DataStore.

---

# 49. Cleanup History

Recommended record:

```kotlin
data class CleanupHistoryEntry(
    val timestampMillis: Long,
    val mode: CleanupMode,
    val packagesAttempted: Int,
    val packagesSucceeded: Int,
    val measuredFreedBytes: Long,
    val reportedCacheReductionBytes: Long
)
```

Maximum:

```text
25 records
```

Delete oldest records beyond the limit.

---

# 50. Shizuku Setup Screen

Possible state:

```text
Enable cache cleaning

CacheSweep uses Shizuku to perform Android
cache-management operations with ADB-level
permissions.

Your device does not need to be rooted.

1. Start Shizuku
2. Return to CacheSweep
3. Grant CacheSweep access

Shizuku status
Running

CacheSweep permission
Not granted

[ Grant Permission ]
```

On non-root devices Shizuku generally runs through ADB/shell privileges, and its documentation notes that the service may need to be started again after reboot.

---

# 51. Error Model

```kotlin
sealed interface CleanerError {

    data object ShizukuUnavailable :
        CleanerError

    data object PermissionDenied :
        CleanerError

    data object SelectiveUnsupported :
        CleanerError

    data object GlobalTrimUnsupported :
        CleanerError

    data class CommandFailed(
        val exitCode: Int,
        val message: String
    ) : CleanerError

    data class Unexpected(
        val cause: Throwable
    ) : CleanerError
}
```

Never show raw exception dumps in release UI.

---

# 52. Security Requirements

The implementation must enforce:

### S-01

No arbitrary shell command input.

### S-02

No `sh -c`.

### S-03

No exported privileged service accessible to arbitrary applications.

### S-04

Every package name must come from the locally discovered installed-package set.

### S-05

CacheSweep must exclude itself from cleaning while the operation is active.

### S-06

Never use `pm clear` without `--cache-only`.

### S-07

Never invoke app-data deletion APIs.

### S-08

Never expose the Shizuku service through deep links.

### S-09

No remote command capability.

### S-10

No internet permission in MVP.

---

# 53. Unit Tests — Mandatory

## Command safety test

```kotlin
@Test
fun clearCommand_alwaysContainsCacheOnly() {

    val args =
        PackageCommands
            .buildClearCacheArgs(
                packageName =
                    "com.example.test",
                userId = 0
            )

    assertTrue(
        args.contains("--cache-only")
    )
}
```

---

## Ensure full clear cannot be generated

```kotlin
@Test
fun commandBuilder_neverCreatesPlainPmClear() {

    val command =
        PackageCommands
            .buildClearCacheArgs(
                "com.example.test",
                0
            )
            .joinToString(" ")

    assertNotEquals(
        "pm clear com.example.test",
        command
    )
}
```

---

## Negative storage delta

```kotlin
@Test
fun negativeStorageDelta_reportsZero() {

    val result =
        calculateFreedBytes(
            before = 10_000,
            after = 9_000
        )

    assertEquals(
        0,
        result
    )
}
```

---

## Partial scanner failures

Test:

```text
300 installed apps

297 queries succeed

3 throw SecurityException
```

Expected:

```text
scan succeeds

successfulApps = 297

failed = 3
```

---

# 54. Capability Parser Tests

Input:

```text
pm clear [--user USER_ID] [--cache-only] PACKAGE
```

Expected:

```text
supportsSelectiveCacheClear = true
```

Input without `--cache-only`:

```text
pm clear [--user USER_ID] PACKAGE
```

Expected:

```text
supportsSelectiveCacheClear = false
```

---

# 55. Device Feasibility Spike

Before building all UI polish, perform the following tests on a physical phone.

## Step 1 — Verify Shizuku

Confirm:

```text
Shizuku Binder available

Permission granted

UID = 2000
```

for the expected non-root ADB mode.

---

## Step 2 — Inspect package manager

Through the privileged UserService, inspect:

```text
pm help
```

Record:

```text
--cache-only present?
trim-caches present?
```

---

## Step 3 — Build a Disposable Test App

Create a tiny test application containing:

```text
SharedPreferences:
logged_in = true

Database:
test record

Cache:
500 MB generated test cache
```

This avoids experimenting on important personal apps.

---

## Step 4 — Run Selective Cache Clear

Execute:

```text
pm clear --cache-only <test-package>
```

Expected:

```text
Cache decreases substantially

SharedPreferences remain

Database remains

Application does not behave like
a fresh installation
```

This validates the critical safety property.

---

## Step 5 — Repeat

Run the selective operation approximately:

```text
10 times
```

with regenerated cache.

Check:

* No app-data loss.
* No crashes.
* Consistent return status.
* Storage statistics update.

---

# 56. Global Trim Spike

Generate cache in several disposable apps.

Measure:

```text
physical free storage

aggregate reported cache
```

Run:

```text
pm trim-caches <target>
```

Then measure again.

Record:

```text
Requested target
Reported cache before
Reported cache after
Physical free before
Physical free after
Command exit code
```

---

# 57. Shizuku Failure Tests

Test all of:

```text
Shizuku stopped before cleaning

Shizuku stopped during app scan

Shizuku process restarted

Permission denied

Permission revoked

Device rebooted
```

Expected:

* Cache scanning should continue when possible.
* Privileged cleaning is disabled.
* App does not crash.
* UI explains how to reconnect.

---

# 58. Usage Access Failure Tests

Test:

```text
Usage Access granted
Usage Access revoked
Usage Access granted again
```

Expected:

```text
device free storage remains visible

app cache list becomes unavailable when revoked

cleaning can still be capability-checked independently

UI prompts user to restore Usage Access
```

---

# 59. OEM Compatibility Test Matrix

For every available device, record:

```text
Manufacturer
Model
Android version
Build number

StorageStats works?
Usage Access works?
Shizuku works?
--cache-only works?
trim-caches works?
Selective clear preserves data?
Stats refresh latency?
```

Do not infer that Samsung/Xiaomi/Pixel behavior is identical.

---

# 60. MVP Feature Matrix

```text
Feature                      Required

Device storage display          YES

Installed app scanner           YES

Per-app cache reporting         YES

Sort by cache                   YES

Search apps                     YES

Usage Access setup              YES

Shizuku setup                   YES

Runtime capability probe        YES

Per-app cache clear          IF SUPPORTED

Multi-select cache clear     IF SUPPORTED

Global cache trim               YES

Before/after measurement        YES

Open native app settings        YES

Dark mode                       YES

Local settings                  YES

No internet                     YES

No analytics                    YES

Root support                     NO

Automatic scheduled clean       NO
```

---

# 61. Implementation Milestones

## Milestone 0 — Privilege Feasibility

Build only:

```text
Shizuku connection
Capability probe
Test package cache generation
Selective clear
Global trim
```

Do not spend significant time polishing UI until this works on the target phone.

---

## Milestone 1 — Scanner

Implement:

```text
Usage Access

Package enumeration

StorageStatsManager

Per-app cache statistics

Aggregate cache

Sorting

Search
```

---

## Milestone 2 — Cleaner Backend

Implement:

```text
Shizuku UserService

AIDL

Capability detection

Selective clear

Global trim

Error handling
```

---

## Milestone 3 — Cleanup Coordinator

Implement:

```text
Before snapshot

Cleaning state machine

Post-clean settling

Rescan

Results

Partial failures
```

---

## Milestone 4 — Compose UX

Implement:

```text
Setup

Dashboard

App list

App detail

Confirmation

Cleaning screen

Result

Settings
```

---

## Milestone 5 — Hardening

Implement:

```text
Security tests

Process recreation

Shizuku death handling

Permission revocation

Large app-count testing

OEM/device testing

Release build
```

---

# 62. Recommended First Prototype Screen

Do not initially build the full product.

Create a developer diagnostic screen:

```text
CacheSweep Technical Test

Android
16

Shizuku
Connected

Privileged UID
2000

Selective cache clear
SUPPORTED

Global trim
SUPPORTED

Usage Access
GRANTED

Installed apps
314

Reported cache
7.24 GB


Test application

Cache before
512 MB

[ CLEAR TEST CACHE ]

Cache after
2.4 MB

Preferences intact
YES

Database intact
YES
```

This screen validates almost every risky assumption before UI development.

---

# 63. Go / No-Go Criteria

Proceed to full product implementation only if:

```text
[PASS] StorageStats reports useful cache values

[PASS] Shizuku UserService runs reliably

[PASS] Capability detection works

[PASS] Selective clearing preserves app data
       OR selective mode is safely disabled

[PASS] Global trim works as fallback

[PASS] No root is required

[PASS] Shizuku reconnect behavior is manageable
```

If `--cache-only` fails on the target phone:

```text
Selective cleaning = disabled
```

The app remains viable with:

```text
cache scanner
+
global trim
+
native app settings shortcuts
```

---

# 64. Definition of Done for v0.1

CacheSweep v0.1 is complete when:

* APK builds successfully.
* APK installs through sideloading.
* Usage Access setup works.
* Installed apps can be scanned.
* Cache sizes display correctly enough for prioritization.
* Shizuku state is detected.
* Shizuku permission can be requested.
* Capability probe reports the device's actual support.
* CacheSweep never issues an unqualified `pm clear`.
* Selective cache cleaning works when supported.
* Global trimming works as fallback.
* App data remains intact after selective cleaning.
* Before/after storage is measured.
* Failed package operations do not abort the entire clean.
* No root is required.
* No network permission exists.
* No analytics exist.
* Release APK can be generated.

---

# 65. Final Architecture Decision

The recommended CacheSweep v1 architecture is:

```text
                 ┌─────────────────────┐
                 │    Jetpack Compose  │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │     ViewModels      │
                 └──────────┬──────────┘
                            │
              ┌─────────────┴─────────────┐
              │                           │
              ▼                           ▼
    ┌─────────────────┐         ┌─────────────────┐
    │  Cache Scanner  │         │ Cleanup Coord.  │
    └────────┬────────┘         └────────┬────────┘
             │                           │
             ▼                           ▼
    ┌─────────────────┐         ┌─────────────────┐
    │ StorageStatsMgr │         │   CacheCleaner  │
    └─────────────────┘         └────────┬────────┘
                                        │
                                        ▼
                               ┌─────────────────┐
                               │ Shizuku Manager │
                               └────────┬────────┘
                                        │
                                        ▼
                               ┌─────────────────┐
                               │   UserService   │
                               └────────┬────────┘
                                        │
                            ┌───────────┴───────────┐
                            │                       │
                            ▼                       ▼
                  pm clear --cache-only     pm trim-caches
```

The most important design principle is:

> **CacheSweep does not gain unrestricted access to other applications. It asks Android's own package-management system to perform cache-specific operations using Shizuku's ADB/shell identity.**

That keeps the cleaner narrow, auditable, and substantially safer than directly deleting files inside application data directories.

---

# 66. Implementation Target

The first code milestone should not be the polished dashboard.

It should be a **technical spike APK** proving these four things on the actual phone:

```text
1. StorageStats can measure app caches.

2. Shizuku connects successfully.

3. The device exposes a safe cache-cleaning
   operation.

4. Running that operation removes cache
   without removing application data.
```

Once those four checks pass, the remaining work is primarily conventional Android application engineering.
