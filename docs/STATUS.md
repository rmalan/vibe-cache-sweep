# CacheSweep Development Status

**Last updated:** 2026-08-16
**Overall status:** In progress
**Current phase:** Phase 0 — Technical Feasibility
**Current task:** P0-17 through P0-24 — Shizuku Connection and Privileged UID Validation

---

# At a Glance

* [ ] Phase 0 — Technical Feasibility
* [ ] Phase 1 — Production Cache Scanner
* [ ] Phase 2 — Production Cleaner
* [ ] Phase 3 — Cleanup Coordinator & Results
* [ ] Phase 4 — Product UI & Persistence
* [ ] Phase 5 — Hardening & Release

---

# Current Task

## P0-17 through P0-24 — Shizuku Connection and Privileged UID Validation

**Status:** Ready to start

### Objective

Verify Shizuku connection state, permission workflow, Binder death handling, and shell UID retrieval on device.

### Expected outcome

Shizuku connection status and privileged backend UID (expected 2000 for ADB shell) are displayed and verified on the diagnostic screen.

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

---

# Phase 0 Progress

## Foundation

* [x] P0-01 Create Android Studio project
* [x] P0-02 Configure Kotlin + Jetpack Compose
* [x] P0-03 Configure SDK/Gradle
* [x] P0-04 Add dependencies
* [x] P0-05 Create project structure
* [x] P0-06 Diagnostic Compose screen
* [x] P0-07 Verify debug build

## Usage Access

* [x] P0-08 through P0-11

## Storage statistics

* [x] P0-12 through P0-16

## Shizuku

* [ ] P0-17 through P0-24

## Privileged backend

* [ ] P0-25 through P0-29

## Capability detection

* [ ] P0-30 through P0-34

## Cache safety test

* [ ] P0-35 through P0-44

## Global trimming

* [ ] P0-45 through P0-49

See `ROADMAP.md` for complete task descriptions.

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
    +---- StorageStats scanner
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

These capabilities still require validation on the target physical device.

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
SUCCESS: ./gradlew testDebugUnitTest completed successfully (23/23 unit tests passed across 6 test suites).
```

---

# Physical Device Validation

**Status:** In progress (Validated on Samsung Galaxy A34 5G, SM-A346E, Android 16 / SDK 36)

Validation items:

* [x] Android version/build recorded (Samsung SM-A346E, Android 16, API 36)
* [x] Usage Access permission verified (AppOps GET_USAGE_STATS: allow)
* [x] StatFs storage snapshot verified (Total, Used, Free accurately reported)
* [x] StorageStats returns useful package cache information (542 apps scanned in 840ms, 15.99 GB total reported cache)
* [x] Single package StorageStats inspector verified with app/cache/data/total size breakdown
* [ ] Shizuku starts successfully
* [ ] CacheSweep Shizuku permission works
* [ ] Shizuku privileged UID identified
* [ ] `clear --cache-only` capability detected
* [ ] Selective cache clearing tested safely
* [ ] App data verified intact
* [ ] `trim-caches` capability detected
* [ ] Global trimming tested

---

# Known Issues

None.

---

# Current Blockers

None.

---

# Architecture Deviations

None.

Current implementation follows `TECH_SPEC.md`.

---

# Most Recent Completed Task

**P0-12 through P0-16 — Storage Statistics Validation**

---

# Exact Next Action

Implement / Verify:

**P0-17 through P0-24 — Shizuku connection and privileged UID validation**

Then continue with:

* P0-25 through P0-29: Privileged backend verification
* P0-30 through P0-34: Capability detection
