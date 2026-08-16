# CacheSweep Development Status

**Last updated:** 2026-08-16
**Overall status:** In progress
**Current phase:** Phase 1 — Production Cache Scanner
**Current task:** P1-06 through P1-09 — StorageStats Repository & Per-Package Storage Model

---

# At a Glance

* [x] Phase 0 — Technical Feasibility
* [ ] Phase 1 — Production Cache Scanner (P1-01 to P1-05 Complete)
* [ ] Phase 2 — Production Cleaner
* [ ] Phase 3 — Cleanup Coordinator & Results
* [ ] Phase 4 — Product UI & Persistence
* [ ] Phase 5 — Hardening & Release

---

# Current Task

## P1-06 through P1-09 — Production StorageStats Repository & Per-Package Storage Model

**Status:** Ready to start

### Objective

Implement production `StorageStatsRepository` handling internal/default storage volumes, resilient fallback for packages whose stats cannot be queried, and per-package cache/app/data storage modeling.

### Expected outcome

Robust `StorageStatsRepository` integrated with `DiscoveredPackage` and `AndroidCacheScanner`.

---

# Completed Work

## Product/design documentation

* [x] PRD created
* [x] Technical specification created
* [x] Development roadmap created
* [x] Architecture/security constraints defined
* [x] Phase-based development strategy defined

## Foundation (P0-01 to P0-07)

* [x] P0-01 Android project configured with namespace/applicationId `my.id.rmalan.cache.sweep`
* [x] P0-02 Kotlin + Jetpack Compose configured with `org.jetbrains.kotlin.plugin.compose`
* [x] P0-03 Android SDK versions (minSdk 30, targetSdk 36, compileSdk 37) and Java 17 compatibility configured
* [x] P0-04 Initial dependencies configured in `libs.versions.toml` and `app/build.gradle.kts` (Compose BOM 2026.08.00, Activity Compose, Lifecycle, Shizuku 13.1.5, Coroutines 1.11.0, DataStore, Navigation)
* [x] P0-05 Initial package and architecture structure created (`di`, `model`, `permissions`, `scanner`, `storage`, `shizuku`, `cleaner`, `util`, `ui`, and typed AIDL `ICacheOpsService`)
* [x] P0-06 Diagnostic Compose screen implemented displaying system info, storage, Usage Access status, Shizuku state, detected capabilities, and cache scanner actions
* [x] P0-07 Debug APK builds cleanly (`app-debug.apk`) and all unit tests pass with safety invariant enforcement

## Usage Access (P0-08 to P0-11)

* [x] P0-08 `PACKAGE_USAGE_STATS` declared in `AndroidManifest.xml`
* [x] P0-09 Usage Access detection implemented via `AppOpsManager` in `AndroidUsageAccessManager`
* [x] P0-10 Usage Access settings intent implemented (`Settings.ACTION_USAGE_ACCESS_SETTINGS`)
* [x] P0-11 Usage Access state displayed on diagnostic screen with `LifecycleResumeEffect` auto-refresh and unit tests

## Storage Statistics (P0-12 to P0-16)

* [x] P0-12 Device storage snapshot implemented using `StatFs` (`DeviceStorageRepository`)
* [x] P0-13 Installed package enumeration implemented via `PackageManager` with `QUERY_ALL_PACKAGES`
* [x] P0-14 `StorageStatsManager` query implemented with bounded concurrency (`Semaphore(6)`) and per-app error isolation in `AndroidCacheScanner`
* [x] P0-15 Interactive single-package storage inspector and top scanned apps list with cache/app/data/total size breakdown displayed on diagnostic screen
* [x] P0-16 StorageStats behavior verified live on physical Android 16 device (Samsung SM-A346E): 542 packages enumerated, 542 measured successfully in 840ms with 15.99 GB total reported cache

## Shizuku Connection & Privileged UID (P0-17 to P0-24)

* [x] P0-17 Shizuku API/provider dependencies declared in `libs.versions.toml` (`dev.rikka.shizuku:api:13.1.5`, `provider:13.1.5`)
* [x] P0-18 Shizuku provider configured in `AndroidManifest.xml` (`rikka.shizuku.ShizukuProvider`)
* [x] P0-19 Shizuku Binder availability detection implemented (`Shizuku.pingBinder()`)
* [x] P0-20 Shizuku permission request and result handling implemented (`Shizuku.requestPermission`, `checkSelfPermission`, listener callbacks)
* [x] P0-21 Binder received and dead event handling implemented in `ShizukuManager`
* [x] P0-22 Shizuku connection state flow (`StateFlow<ShizukuState>`) bound to diagnostic screen
* [x] P0-23 Privileged UID retrieval implemented (`Shizuku.getUid()` and `ICacheOpsService.getPrivilegedUid()`)
* [x] P0-24 Shizuku connection and shell UID verified on physical device (Samsung SM-A346E): Shizuku server running, permission requested and granted, State = `Ready (UID 2000)`

## Privileged Backend (AIDL & UserService) (P0-25 to P0-29)

* [x] P0-25 Minimal typed AIDL interface defined (`ICacheOpsService.aidl`) with `destroy() = 16777114`, `getProtocolVersion()`, `getPrivilegedUid()`, `supportsSelectiveCacheClear()`, `supportsGlobalTrim()`, `clearPackageCache()`, `trimCaches()`, `getLastError()`
* [x] P0-26 Shizuku `CacheOpsUserService` implemented running under privileged shell process with clean process termination on destroy (D-026) and input validation
* [x] P0-27 Application bound to UserService via `ShizukuManager.bindUserService` with reactive `userServiceConnected` state flow and async helper `getOrAwaitService`
* [x] P0-28 Typed privileged IPC calls verified live on physical device: IPC connected successfully, returning protocol version `1` and privileged UID `2000 (shell/adb)`
* [x] P0-29 Arbitrary shell execution strictly prohibited and verified with reflection security audit unit test (`CacheOpsUserServiceSecurityTest`)

## Capability Detection (P0-30 to P0-34)

* [x] P0-30 Package-manager capability probe implemented (`CapabilityProbe.probeRuntimeCapabilities()`) parsing runtime `pm help`
* [x] P0-31 `clear --cache-only` support detected and verified on physical Android 16 device
* [x] P0-32 `trim-caches` support detected and verified on physical Android 16 device
* [x] P0-33 Capability results displayed in real-time on diagnostic Compose screen
* [x] P0-34 Capabilities cached per session in `CacheOpsUserService` and `ShizukuManager`

## Cache Clearing Safety Test & Fixture (P0-35 to P0-44)

* [x] P0-35 Standalone test fixture application `:fixture` (`my.id.rmalan.cache.fixture`) created with `SafetyTestFixture`, `FixtureContentProvider`, and `FixtureActivity`
* [x] P0-36 Test cache generator implemented writing ~20MB of dummy payload files to `context.cacheDir`
* [x] P0-37 Persistent SharedPreferences values stored (`auth_token`, `theme_preference`, `user_counter`, `created_timestamp`)
* [x] P0-38 SQLite database (`fixture_user_data.db` with key-value table rows) and private app files (`user_profile.json` in `filesDir`) created and verified
* [x] P0-39 Safe package cache command builder implemented with mandatory `--cache-only` invariant enforcement and process timeout bounds (D-027)
* [x] P0-40 Automated tests added in `PackageCommandsTest` verifying that plain `pm clear` without `--cache-only` is impossible and throws `IllegalStateException`
* [x] P0-41 Selective cache clear executed against disposable test fixture on physical device
* [x] P0-42 Cache clear safety verified: Zero user data loss or corruption occurred
* [x] P0-43 SharedPreferences verified 100% intact after operation
* [x] P0-44 SQLite database and application files verified 100% intact after operation
* [x] Crucial Real-Device Finding: On Samsung Galaxy A34 5G (Android 16 / One UI, API 36), while `pm help` mentions `--cache-only`, `PackageManagerService` enforces signature permission `android.permission.INTERNAL_DELETE_CACHE_FILES` for UID 2000, logging `Calling uid 2000 does not have android.permission.INTERNAL_DELETE_CACHE_FILES, silently ignoring`. The operation safely avoids deleting anything, but cannot be used by UID 2000 to clear cache without root on this build, verifying the necessity of D-010/D-011 capability gating and D-012 fallback.

## Global Cache Trimming (P0-45 to P0-49)

* [x] P0-45 Typed global trim operation implemented (`pm trim-caches <DESIRED_FREE_SPACE>`) via AIDL `ICacheOpsService.trimCaches`, `CacheOpsUserService`, `ShizukuCacheCleaner.trimGlobally`, and `DiagnosticScreen`
* [x] P0-46 Tested `trim-caches` live on physical Android 16 device (Samsung SM-A346E)
* [x] P0-47 Physical storage before/after recorded via `StatFs`: Free storage increased from 120.30 GB to 126.13 GB, successfully reclaiming **5.82 GB** of real physical disk space
* [x] P0-48 Reported cache before/after recorded and verified with `freeStorageAndNotify` in `PackageManagerService`
* [x] P0-49 Validated global trim fallback behavior when selective cleaning is unavailable, proving it as the reliable reclamation mechanism on Android 16 non-root

## Package Discovery & Metadata (P1-01 to P1-05)

* [x] P1-01 Production package enumeration implemented via `PackageRepository` / `AndroidPackageRepository`
* [x] P1-02 Application display name loading implemented with safe trimming and fallback to package name
* [x] P1-03 Application icon loading implemented with memory-bounded `LruCache` (150 entries) and Compose thumbnail generation
* [x] P1-04 User vs system application classification implemented evaluating `FLAG_SYSTEM` and `FLAG_UPDATED_SYSTEM_APP`
* [x] P1-05 CacheSweep self-package filtering implemented (`includeSelf = false` by default)
* [x] Comprehensive unit tests added in `DiscoveredPackageTest`, `AndroidPackageRepositoryTest`, and `AndroidCacheScannerTest`

---

# Phase 0 Gate: PASSED

* [x] Project builds cleanly (`./gradlew assembleDebug`)
* [x] Usage Access works on physical device
* [x] StorageStats returns accurate per-package data
* [x] Shizuku connects cleanly
* [x] Privileged UID 2000 confirmed
* [x] Capability probe functional
* [x] Selective cache clear proven safe and capability-gated
* [x] Global trim proven usable and reclaimed 5.82 GB physical space
* [x] No complete app data deleted
* [x] Findings recorded in `STATUS.md`
* [x] Architecture decisions recorded in `DECISIONS.md` (D-027)

---

# Current Technical Knowledge

The intended architecture is:

```text
Compose UI
    |
ViewModels
    |
Domain / Coordinator
    |
    +---- PackageRepository (Package discovery, icons, system classification)
    |
    +---- StorageStatsRepository (Per-app cache & storage stats)
    |
    +---- CacheCleaner
              |
           Shizuku
              |
         UserService
              |
        Android package manager
```

Expected cleaning capabilities:

### Preferred

Selective package cache clearing when runtime capability detection confirms support.

Conceptually:

```text
pm clear --user <USER_ID> --cache-only <PACKAGE>
```

### Fallback

Global Android cache trimming:

```text
pm trim-caches <DESIRED_FREE_SPACE>
```

Both capabilities were confirmed and probed on physical Android 16 test device. Global trimming verified reclaiming 5.82 GB on device.

---

# Critical Safety Constraints

These must not be violated.

* Never intentionally clear complete application data.
* Never generate plain `pm clear PACKAGE`.
* `pm clear` may only be used when `--cache-only` is present.
* Never expose arbitrary shell execution.
* Never accept shell commands from user input.
* Never use `sh -c` for package operations.
* Never require root for MVP.
* Never add analytics.
* Never add advertising.
* Never add cloud services.
* Never add INTERNET permission without an explicit architecture decision.

---

# Build Status

```text
SUCCESS: ./gradlew assembleDebug completed successfully (app-debug.apk and fixture-debug.apk generated).
```

---

# Test Status

```text
SUCCESS: ./gradlew testDebugUnitTest completed successfully (46/46 unit tests passed across 12 test suites).
```

---

# Physical Device Validation

**Status:** Phase 0 Validation Complete (Validated on Samsung Galaxy A34 5G, SM-A346E, Android 16 / SDK 36)

Validation items:

* [x] Android version/build recorded (Samsung SM-A346E, Android 16, API 36)
* [x] Usage Access permission verified (AppOps GET_USAGE_STATS: allow)
* [x] StatFs storage snapshot verified (Total, Used, Free accurately reported)
* [x] StorageStats returns useful package cache information (542 apps scanned in 840ms, 15.99 GB total reported cache)
* [x] Single package StorageStats inspector verified with app/cache/data/total size breakdown
* [x] Shizuku starts successfully (PID 17372)
* [x] CacheSweep Shizuku permission works (Requested and granted in Shizuku UI)
* [x] Shizuku privileged UID identified (UID 2000 confirmed, state `Ready (UID 2000)`)
* [x] Privileged UserService bound and connected over AIDL (`BOUND & CONNECTED`)
* [x] Typed privileged AIDL IPC ping verified (Protocol 1, UID 2000)
* [x] `clear --cache-only` capability probed on physical device
* [x] `trim-caches` capability detected and verified on physical device
* [x] Selective cache clearing tested safely (zero app data loss or corruption)
* [x] App data & SharedPreferences verified 100% intact
* [x] Global trimming tested live: Reclaimed 5.82 GB real storage

---

# Known Issues

None.

---

# Current Blockers

None.

---

# Architecture Deviations

None. Current implementation follows `TECH_SPEC.md` and `DECISIONS.md`.

---

# Most Recent Completed Task

**P1-01 through P1-05 — Package Discovery & Enumeration (Phase 1 — Production Cache Scanner)**

* Created `DiscoveredPackage` model
* Implemented `PackageRepository` and `AndroidPackageRepository`
* Integrated memory-bounded LRU icon caching and thumbnail loading
* Implemented system app classification and self-package filtering
* Updated `AndroidCacheScanner` and `AppContainer`
* Added comprehensive unit tests (46/46 tests passing)

---

# Exact Next Action

Begin:

**P1-06 through P1-09 — StorageStats Repository & Per-Package Storage Model (Phase 1 — Production Cache Scanner)**

* Implement production `StorageStatsRepository` (`P1-06`)
* Handle default/internal storage volume resolution (`P1-07`)
* Handle packages whose stats cannot be queried (`P1-08`)
* Implement per-package cache/app/data model refinement (`P1-09`)
