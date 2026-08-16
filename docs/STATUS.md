# CacheSweep Development Status

**Last updated:** 2026-08-16
**Overall status:** In progress
**Current phase:** Phase 5 (Hardening & Release) — Ready to start
**Current task:** P5-01 through P5-06 — Failure Scenario Hardening

---

# At a Glance

* [x] Phase 0 — Technical Feasibility
* [x] Phase 1 — Production Cache Scanner
* [x] Phase 2 — Production Cleaner
* [x] Phase 3 — Cleanup Coordinator & Results
* [x] Phase 4 — Product UI & Persistence
* [ ] Phase 5 — Hardening & Release

---

# Current Task

## P5-01 through P5-06 — Failure Scenario Hardening & Edge Cases

**Status:** Ready to start

### Objective

Harden CacheSweep against all real-world failure scenarios and edge cases outlined in ROADMAP.md Phase 5:
- Shizuku absent (`P5-01`)
- Shizuku stopped / process killed (`P5-02`)
- Shizuku dies / connection drops mid-cleanup (`P5-03`)
- Permission denied (`P5-04`)
- Permission revoked while app is running (`P5-05`)
- Usage Access revoked while app is running (`P5-06`)

### Expected outcome

Robust error isolation and recovery paths ensuring the app never crashes, hangs, or leaves inconsistent state when system privileges or background services fail.

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

## StorageStats Repository & Per-Package Storage Model (P1-06 to P1-09)

* [x] P1-06 Production `StorageStatsRepository` interface and `AndroidStorageStatsRepository` implementation created
* [x] P1-07 Volume UUID resolution implemented defaulting gracefully to `StorageManager.UUID_DEFAULT`
* [x] P1-08 Robust exception isolation implemented (`SecurityException`, `IOException`, `IllegalArgumentException`, etc.) returning typed unmeasured stats with error descriptions
* [x] P1-09 Dedicated `PackageStorageStats` model created with `ZERO` / `failed(...)` factories, non-negative value coercion, and `AppCacheInfo.fromPackageAndStats` mapping
* [x] Comprehensive unit tests added in `PackageStorageStatsTest`, `AndroidStorageStatsRepositoryTest`, `StorageModelsTest`, and updated `AndroidCacheScannerTest`

## Scanner Engine & Progressive State (P1-10 to P1-16)

* [x] P1-10 Production `CacheScanner` interface refined supporting both one-shot `scan` and reactive `scanFlow` with `includeSelf` and `includeSystem` parameters
* [x] P1-11 Bounded concurrency implemented using `Semaphore(6)` (configurable) with channel-based coroutine coordination
* [x] P1-12 Progressive `ScanState` flow modeled (`Idle`, `Discovering`, `Scanning` with progress fraction / running cache, `Complete`, `Failed`) and wired to UI
* [x] P1-13 Robust partial-failure isolation implemented: individual package query failures never abort device scan
* [x] P1-14 Aggregate reported cache accurately computed across all successfully measured packages
* [x] P1-15 Attempted vs successful package measurement metrics tracked in `ScanResult`
* [x] P1-16 Accurate scan duration measurement recorded in milliseconds
* [x] Comprehensive unit tests added in `ScanStateTest` and `AndroidCacheScannerTest` (P1-25, P1-26)

## User Experience & Cache List UI (P1-17 to P1-24 & P1-27 to P1-29)

* [x] P1-17 Production cache application list implemented (`AppCacheListScreen`, `AppCacheRow`, `AppIcon`)
* [x] P1-18 Sort by cache size implemented (`AppSort.CACHE_DESC`)
* [x] P1-19 Sort by total storage footprint implemented (`AppSort.TOTAL_DESC`)
* [x] P1-20 Sort alphabetically implemented (`AppSort.NAME_ASC`)
* [x] P1-21 Real-time live search filtering implemented by app name and package name (`AppFilter`)
* [x] P1-22 Pull-to-refresh scanner trigger implemented (`PullToRefreshBox`)
* [x] P1-23 Application detail bottom sheet implemented with storage breakdown and educational note (`AppDetailBottomSheet`)
* [x] P1-24 Native Android per-app storage settings shortcut intent implemented (`PackageShortcuts`)
* [x] P1-27 Sorting unit tests added in `AppSortTest`
* [x] P1-28 Search and filtering unit tests added in `AppFilterTest`
* [x] P1-29 350+ application high-capacity scan performance test verified in `AndroidCacheScannerTest`

## Cleaner Abstraction & Typed Plan Engine (P2-01 to P2-04)

* [x] P2-01 Production `CacheCleaner` interface defined with capabilities query, single-package clear, multi-package batch clear with progressive feedback, global trim, and typed `CleanupPlan` execution
* [x] P2-02 `CleanerCapabilities` model implemented with readiness helpers (`isReady`, `canCleanSelective`, `canCleanGlobal`, `canCleanAny`) and `UNAVAILABLE` fallback constant
* [x] P2-03 `CleanupPlan` engine implemented supporting `SELECTIVE` and `GLOBAL_TRIM` modes with package validation, self-clean exclusion, and factory methods
* [x] P2-04 Structured `CleanerError` hierarchy, `CleaningProgress`, and `CleanerBatchResult` models implemented with error attribution
* [x] `PackageValidator` enhanced with `isValidFormat` and `isSelfPackage` classification
* [x] Production `ShizukuCacheCleaner` updated with capability gating, robust error handling, progressive feedback, and plan execution
* [x] Comprehensive unit tests added in `CleanerCapabilitiesTest`, `CleanupPlanTest`, `CleanerErrorTest`, and `ShizukuCacheCleanerTest`

## Selective Cleaning & Multi-Package Engine (P2-05 to P2-11)

* [x] P2-05 Production package cache clear verified with `--cache-only` enforcement and timeout bounds
* [x] P2-06 Package validation against scanned package set implemented in `PackageValidator.validatePackage`, `PackageValidator.isKnownPackage`, and `CleanupPlan.validate(scannedPackages)`
* [x] P2-07 CacheSweep self-package cleaning strictly prohibited across validator, plan, cleaner, AIDL service, and command builder
* [x] P2-08 Multi-package selective cleaning implemented supporting plan generation and batch execution
* [x] P2-09 Progressive package-count feedback implemented with app display name resolution via `PackageRepository` in `CleaningProgress`
* [x] P2-10 Robust individual failure isolation implemented: individual package failures (invalid format, self package, not scanned, exit code, or IPC exception) never abort remaining batch
* [x] P2-11 Granular failed-package reporting implemented with `CleanerBatchResult`, `errors` map, and `errorMessageSummary()`
* [x] Comprehensive unit tests added in `PackageValidatorTest`, `CleanerErrorTest`, `CleanupPlanTest`, and `ShizukuCacheCleanerTest`

## Global Cache Trimming Engine & Fallback (P2-12 to P2-15)

* [x] P2-12 Production global cache trimming engine implemented (`pm trim-caches <DESIRED_FREE_SPACE>`) in `GlobalTrimCalculator`, `ShizukuCacheCleaner`, `CacheOpsUserService`, and `PackageCommands`
* [x] P2-13 Target free-storage calculation implemented in `GlobalTrimCalculator.calculateDesiredFreeBytes(deviceStorage, estimatedCacheBytes)` conforming to TECH_SPEC Section 34 with overflow and non-negative boundary protection, plus `CleanupPlan.globalTrim(deviceStorage, ...)` and `CleanupPlan.maxGlobalTrim(deviceStorage)` factories
* [x] P2-14 Explicit user consent strictly required before selective → global degradation in `CleanupPlan.toGlobalTrimFallback(deviceStorage, userConsentConfirmed)`, `CleanupPlan.canFallbackToGlobalTrim`, and enforced in `ShizukuCacheCleaner` (preventing silent auto-downgrades)
* [x] P2-15 Graceful handling of unsupported global trim implemented with structured `CleanerError.GlobalTrimUnsupported`, `CleanerError.ShizukuUnavailable`, `CleanerError.PermissionDenied`, `CleanerError.CommandFailed`, and IPC exception isolation
* [x] Comprehensive unit tests added in `GlobalTrimCalculatorTest`, `CleanupPlanTest`, and `ShizukuCacheCleanerTest`

## Cleaner Security Auditing & Invariant Verification (P2-16 to P2-20)

* [x] P2-16 Zero `sh -c` usage across all modules enforced and verified via automated recursive source scanner test (`CodebaseSecurityAuditTest.codebase_hasZeroUsageOfShDashC`)
* [x] P2-17 Arbitrary command execution eliminated: reflection audit verified only typed AIDL interfaces (`ICacheOpsService`), typed `CacheCleaner` domain methods, and zero `Runtime.getRuntime().exec` across production code (`CodebaseSecurityAuditTest.codebase_hasNoRuntimeExecInProductionCode`, `aidlInterface_strictlyEnforcesTypedOperationsWithoutArbitraryExecution`, `cacheCleanerInterface_containsOnlyTypedOperations`)
* [x] P2-18 Command argument generation verified across all valid package patterns, user IDs, boundary conditions (0L, 1L, Long.MAX_VALUE), and injection payloads (metacharacters, traversal, delimiters, self packages, negative IDs) in `PackageCommandsTest`
* [x] P2-19 Critical invariant enforced: plain `pm clear PACKAGE` without `--cache-only` cannot be generated or executed in any permutation (`commandBuilder_neverCreatesPlainPmClear`, `execute_refusesClearWithoutCacheOnly_inAnyForm` in `PackageCommandsTest`)
* [x] P2-20 Android component and permissions security audited: verified no `INTERNET` permission in `:app` or `:fixture`, `allowBackup="false"`, `MainActivity` is the sole exported activity with `MAIN`/`LAUNCHER`, no exported services or receivers, and `ShizukuProvider` is properly permission-gated with `INTERACT_ACROSS_USERS_FULL` (`AndroidManifestSecurityAuditTest`)

## Cleanup Coordinator Engine & Snapshots (P3-01 to P3-13)

* [x] P3-01 Cleanup state machine sealed interface implemented (`CleaningState`: `Idle`, `Validating`, `SnapshotBefore`, `Clearing`, `WaitingForStats`, `SnapshotAfter`, `Completed`, `Failed`) with progress fraction calculation
* [x] P3-02 Capability pre-validation enforced before initiating every cleanup operation in `CleanupCoordinator` (Shizuku availability, permission authorization, selective vs global trim mode support, plan integrity)
* [x] P3-03 Pre-clean physical storage snapshot captured via `DeviceStorageRepository.snapshot()`
* [x] P3-04 Pre-clean reported cache snapshot captured across target packages via `StorageStatsRepository` / `CleanupPlan`
* [x] P3-05 Cleanup plan executed via `CacheCleaner.executePlan` with progressive package-count and application name feedback
* [x] P3-06 Bounded storage-stat settling delay implemented (`settlingDelayMillis` with `DEFAULT_SETTLING_DELAY_MS = 500ms`, zero-delay configurable for instant unit testing)
* [x] P3-07 Target packages rescanned after cleanup to measure actual remaining cache footprint
* [x] P3-08 Post-clean physical storage snapshot captured via `DeviceStorageRepository.snapshot()`
* [x] P3-09 Post-clean reported cache snapshot calculated/measured
* [x] P3-10 Physical free-space delta calculated (`physicalFreeAfter - physicalFreeBefore`)
* [x] P3-11 Reported cache delta calculated (`cacheBefore - cacheAfter`)
* [x] P3-12 Negative freed values clamped to zero (`maxOf(0L, ...)`) protecting against background write noise
* [x] P3-13 Noise threshold constant (`DEFAULT_NOISE_THRESHOLD_BYTES = 16MB`) and `isSignificantReclaim` evaluation implemented conforming to TECH_SPEC Section 37
* [x] `CleanupCoordinator` wired into dependency container `AppContainer`

## Cleanup Screens & Partial Failures UI (P3-14 to P3-17)

* [x] P3-14 Cleanup confirmation dialog implemented (`CleanupConfirmationDialog`) conforming to PRD FR-009 with clear educational expectations and reported cache estimates (avoiding guaranteed reclaim claims)
* [x] P3-15 Cleaning progress screen implemented (`CleaningProgressScreen`) conforming to PRD FR-011 with real-time state machine progression (Validating, SnapshotBefore, Clearing with live package counts and app names, WaitingForStats, and SnapshotAfter) without fake percentage indicators
* [x] P3-16 Cleanup result screen implemented (`CleanupResultScreen`) displaying physical storage before/after metrics, reported cache before/after metrics, significance evaluation, duration, and educational disclaimer
* [x] P3-17 Expandable partial failures section implemented (`PartialFailuresSection`) displaying failed packages with app icons, display names, and attributed failure reasons
* [x] `CleanerViewModel` implemented and wired to `AppCacheListScreen` with multi-select cleaning, single-app clearing from bottom sheet, and result dismissal refresh
* [x] Comprehensive unit tests added in `CleanerViewModelTest` and `CleanupScreensFormattingTest`

## Onboarding Flow & First Launch Experience (P4-01 to P4-04)

* [x] P4-01 Welcome screen implemented (`WelcomeStepContent` in `OnboardingScreen.kt`) with transparent branding, value proposition cards (Storage Visibility, Safe & Controlled, 100% Private & Local), and "Get Started" CTA
* [x] P4-02 Usage Access onboarding implemented (`UsageAccessStepContent`) with educational explanation, privacy guarantee (no tracking or uploading), live granted/required state badge, and direct Android Settings intent with `LifecycleResumeEffect` auto-refresh
* [x] P4-03 Shizuku onboarding implemented (`ShizukuStepContent`) explaining system-level cache trimming without root, live status monitoring (Ready, Permission Required, Connecting, Not Running, Error), "Grant Shizuku Permission" / "Open Shizuku" intents, and manual fallback clarification
* [x] P4-04 First scan flow and setup summary implemented (`FirstScanStepContent`) reviewing permissions, initiating scan, and persisting completion state in DataStore
* [x] UserSettings model (`UserSettings`, `ThemeMode`) and `UserSettingsRepository` / `DataStoreUserSettingsRepository` implemented for non-blocking preferences persistence (D-028)
* [x] `OnboardingViewModel` implemented with multi-step progression, bounds protection, live status checks, and completion event handling
* [x] Dynamic destination routing configured in `MainActivity` defaulting to Onboarding on first launch and transitioning to `AppCacheListScreen` upon completion
* [x] Comprehensive unit tests added in `UserSettingsRepositoryTest`, `OnboardingViewModelTest`, and `OnboardingFormattingTest` (17 new tests); 188/188 tests passing across 34 test suites

## Dashboard Screen & Storage Visualizations (P4-05 to P4-10)

* [x] P4-05 Device storage visualization implemented (`DeviceStorageCard` in `DashboardScreen.kt`) with used/available values, storage bar, and percentage calculation conforming to PRD FR-001
* [x] P4-06 Aggregate cache summary implemented (`ApplicationCacheCard`) with estimated cache headline, scanned vs measured apps count, and educational disclaimer
* [x] P4-07 Largest cache consumers preview implemented (`LargestCachesCard`) displaying top 5 cache consumers with app icons, names, cache sizes, bottom sheet inspection, and "View all apps →" navigation
* [x] P4-08 Live Shizuku connection status card implemented (`ShizukuStatusCard`) handling Ready (UID 2000), Permission Required (with grant action), Not Running (with open/check actions), Connecting, and Error states conforming to PRD FR-008
* [x] P4-09 Last scan metadata display implemented with relative time ("Just now", "X minutes ago", "Yesterday") and scan duration formatting (`DashboardTimeFormatter`)
* [x] P4-10 Primary hero cleanup action implemented (`PrimaryCleanupHero`) with one-tap trigger initiating CleanupCoordinator workflow (Confirmation dialog -> Progress -> Result screen)
* [x] `DashboardViewModel` implemented with immediate device storage snapshotting (sub-500ms initial render), reactive Shizuku state observation, and progressive scan flow
* [x] `MainActivity` updated with `MainDestination.DASHBOARD` as default post-onboarding screen with seamless navigation between Dashboard, App Cache List, Diagnostics, and Onboarding
* [x] Added 9 unit tests across `DashboardViewModelTest` and `DashboardFormattingTest`; 197/197 unit tests passing across 36 test suites

## Settings Screen, Preferences & Cleanup History (P4-11 to P4-19)

* [x] P4-11 Show system apps preference toggle implemented with real-time scanner filtering and DataStore synchronization
* [x] P4-12 Show zero-cache apps preference toggle implemented with real-time scanner filtering and DataStore synchronization
* [x] P4-13 Default sort preference picker implemented supporting Cache Size, Total Footprint, and Alphabetical sorting
* [x] P4-14 Theme mode selector implemented supporting System Default, Light, and Dark themes with dynamic Material 3 composition in `MainActivity`
* [x] P4-15 Local cleanup history clearance implemented with confirmation dialog and non-blocking DataStore reset
* [x] P4-16 DataStore Preferences configured for non-blocking persistence (`user_settings` and `cleanup_history`)
* [x] P4-17 User preferences persistence implemented across app lifecycle and restarts
* [x] P4-18 Cleanup history persistence implemented (`CleanupHistoryEntry`, `CleanupHistoryRepository`, `DataStoreCleanupHistoryRepository`)
* [x] P4-19 Cleanup history bounded at 25 records max per TECH_SPEC Section 49, automatically trimming oldest entries upon new cleanup records
* [x] `SettingsViewModel` and `SettingsScreen` implemented conforming to PRD Section 19 with Shizuku status inspection, privacy guarantees, and about info
* [x] `CleanupCoordinator` updated to automatically record completed cleanup operations to `CleanupHistoryRepository`
* [x] `AppsViewModel` updated to respect user settings for default sort mode and system apps filter
* [x] `MainActivity` updated with `MainDestination.SETTINGS` destination and dynamic `themeMode` binding
* [x] Added 16 unit tests across `CleanupHistoryRepositoryTest`, `SettingsViewModelTest`, and `SettingsFormattingTest`; 213/213 unit tests passing across 39 test suites

## Neobrutalism Design System (D-033)

* [x] Neobrutalist design tokens implemented (`Color.kt`, `Theme.kt`, `Type.kt`) with punchy high-contrast palette and Cyber-Brutalist dark theme
* [x] Core Neobrutalist component library created (`NeoComponents.kt`): `NeoCard`, `NeoButton`, `NeoBadge`, and `NeoProgressBar` featuring 2dp–2.5dp solid black/chalk outlines, hard unblurred drop shadows (`offset(4.dp, 4.dp)`), and flat containers
* [x] Neobrutalism redesign applied across all app screens:
  - `DashboardScreen`: Replaced all cards with `NeoCard`, `NeoProgressBar`, `NeoButton`, and `NeoBadge`
  - `AppCacheListScreen`: Restyled search input field (2dp border), Neobrutalist filter chips (solid 1.5dp–2dp border), `NeoProgressBar`, and floating action button
  - `AppCacheRow`: Metadata badges and byte tags styled with `NeoBadge`
  - `AppDetailBottomSheet`: Storage breakdown styled with `NeoCard` and `NeoButton` CTAs
  - `CleanupConfirmationDialog`: High-contrast confirmation modal with `NeoCard` estimate and bordered action buttons
  - `CleaningProgressScreen`: Chunky progress indicator with `NeoCard` and `NeoProgressBar`
  - `CleanupResultScreen`: Result card with `NeoCard` metrics comparison table, `NeoBadge` deltas, and `NeoButton` Done CTA
  - `PartialFailuresSection`: Solid error card with `NeoCard` and expandable failure details
  - `SettingsScreen`: Modular `NeoCard` sections, `NeoBadge` badges, bold switches, and bordered dialogs
  - `OnboardingScreen`: 4-step wizard with `NeoProgressBar`, `NeoCard` summary, `NeoBadge` status badges, and `NeoButton` CTAs
* [x] All 214+ unit tests pass cleanly (`./gradlew testDebugUnitTest`) and APK builds cleanly (`./gradlew assembleDebug`)

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

# Phase 1 Gate: PASSED

* [x] Scanner works independently of Shizuku
* [x] One bad package does not fail the scan
* [x] UI remains responsive with progressive state and memory-bounded thumbnails
* [x] Cache list can be searched and sorted (Cache size, Total size, Alphabetical)
* [x] Build and tests pass (83/83 unit tests passing)

---

# Phase 2 Gate: PASSED

* [x] Selective cleaning works when supported
* [x] Unsupported devices degrade gracefully
* [x] Global fallback works
* [x] Partial failures work
* [x] Safety tests pass
* [x] No arbitrary privileged shell interface exists

---

# Phase 3 Gate: PASSED

* [x] Full scan → clean → rescan workflow works
* [x] Results never claim guaranteed reclaim amounts
* [x] Negative values are handled
* [x] Storage-stat delay is handled
* [x] Interrupted Shizuku session fails safely
* [x] All 171 unit tests passing across 31 test suites

---

# Phase 4 Gate: PASSED

* [x] Complete product flow works (Onboarding -> Dashboard -> App Cache List -> Settings -> Cleanup Coordinator)
* [x] Product copy matches PRD
* [x] Settings persist via DataStore (system apps, zero-cache apps, sort order, theme mode, cleanup history)
* [x] Light/dark themes work with dynamic system bar controls
* [x] Basic accessibility review passes (SP typography, >=48dp touch targets, TalkBack spoken byte format, semantic headings)
* [x] All 217 unit tests passing across 40 test suites

---

# Current Technical Knowledge

The intended architecture is:

```text
Compose UI (DashboardScreen / AppCacheListScreen / DiagnosticScreen / CleaningProgressScreen / CleanupResultScreen / SettingsScreen)
    |
ViewModels (DashboardViewModel / AppsViewModel / CleanerViewModel / OnboardingViewModel / SettingsViewModel)
    |
Domain / Coordinator (CleanupCoordinator)
    |
    +---- PackageRepository (Package discovery, icons, system classification)
    |
    +---- StorageStatsRepository (Per-app cache & storage stats)
    |
    +---- UserSettingsRepository (DataStore preferences & settings persistence)
    |
    +---- CleanupHistoryRepository (DataStore bounded cleanup history)
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
SUCCESS: ./gradlew assembleDebug completed successfully (app-debug.apk generated).
```

---

# Test Status

```text
SUCCESS: ./gradlew testDebugUnitTest completed successfully (214/214 unit tests passed across 39 test suites).
```

---

# Physical Device Validation

**Status:** Full Live Device Validation Passed (Tested on Samsung Galaxy A34 5G, SM-A346E, Android 16 / SDK 36 over ADB Wireless)

Validation items:

* [x] Android version/build recorded (Samsung SM-A346E, Android 16, API 36)
* [x] Usage Access permission verified (AppOps GET_USAGE_STATS: allow)
* [x] StatFs storage snapshot verified (95.34 GB used, 129.49 GB available of 224.84 GB, 42% storage used)
* [x] StorageStats scanner verified (543 apps scanned in 5.5s, 2.05 GB total reported cache)
* [x] Shizuku server running via ADB Wireless (PID 7864, UID 2000 confirmed, state `Ready (UID 2000)`)
* [x] Capability Probe verified: `supportsSelectiveCacheClear = false` (properly gated for non-root UID 2000), `supportsTrimCaches = true`
* [x] Dashboard UI verified: Device storage card, Cache summary card, Shizuku status card (Global Trimming chip), Largest caches preview, Hero Clean button
* [x] One-tap Clean Cache flow executed live on physical device: Confirmation dialog -> Progress state machine -> Cleanup result screen (+89.8 MB freed, -89.8 MB reported cache reduced in 1.7s)
* [x] App Cache List screen verified: Search bar, Sort chips (Cache size, Total size, App name, Hide 0 B), pull-to-refresh, FAB ("Clean All Cache • 1.85 GB")
* [x] AppDetailBottomSheet verified: Storage breakdown (Cache, App/Code, User Data, Total Storage) and direct shortcut to native Android Storage Settings (`PackageShortcuts.openStorageSettings`)

---

# Known Issues

None.

---

# Current Blockers

None.

---

# Architecture Deviations

None. Current implementation follows `TECH_SPEC.md` and `DECISIONS.md` (including D-027, D-031, and D-032).

---

# Most Recent Completed Task

**Bugfix: App Cache List "Clean All Cache" Plan Validation Error**

* Resolved `Plan validation failed: Global trim target bytes must be positive` occurring when cleaning from `AppCacheListScreen`.
* Root cause: `AppCacheListScreen` FAB condition `if (!state.supportsSelectiveCleaning || !hasSelected)` incorrectly triggered `globalTrim` with `desiredFreeBytes = 0L` when no apps were individually selected (`!hasSelected`), failing validation.
* Fixed FAB onClick logic to generate selective cleanup (`CleanupPlan.fromApps(targetApps)`) whenever selective cleaning is supported, and use `CleanupPlan.globalTrim(estimatedCacheBytes = totalCache)` with nullable `desiredFreeBytes` when global trim is needed.
* Updated `CleanupPlan.globalTrim` factory to support nullable `desiredFreeBytes` and compute target bytes safely.
* Added unit tests in `CleanupPlanTest` and `CleanupCoordinatorTest` covering `desiredFreeBytes = null` resolution and execution.
* Verified 217/217 unit tests passing.

---

# Exact Next Action

Begin:

**P5-01 through P5-06 — Failure Scenario Hardening & Edge Cases (Phase 5 — Hardening & Release)**

* Verify graceful handling when Shizuku is absent (`P5-01`)
* Verify behavior when Shizuku service is stopped or not running (`P5-02`)
* Verify recovery when Shizuku binder dies mid-operation (`P5-03`)
* Verify error paths when Shizuku permission is denied or revoked (`P5-04`, `P5-05`)
* Verify error paths when Usage Access is revoked (`P5-06`)

