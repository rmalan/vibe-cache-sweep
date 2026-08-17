# CacheSweep Architecture & Product Decisions

This file records important decisions that should survive individual coding-agent sessions.

Use this document when a decision:

* affects architecture,
* affects security,
* affects privacy,
* changes product behavior,
* changes platform compatibility,
* contradicts or refines the original specification,
* would be expensive to repeatedly reconsider.

Do not record minor implementation details here.

---

# Decision Status

Use:

* **Accepted** — current project direction.
* **Proposed** — under consideration.
* **Superseded** — replaced by a later decision.
* **Rejected** — considered and intentionally not used.

---

# D-001 — Build the application incrementally

**Status:** Accepted

## Decision

CacheSweep will be implemented in explicit phases rather than asking a coding agent to build the entire product in one session.

Development order:

1. Technical feasibility.
2. Production scanner.
3. Cleaner.
4. Cleanup coordinator.
5. Product UI.
6. Hardening/release.

## Reason

Android storage privileges, OEM behavior, Shizuku behavior, and package-manager cache operations need real-device validation.

Building the complete application before validating these assumptions creates unnecessary implementation risk.

---

# D-002 — Repository is the source of truth

**Status:** Accepted

## Decision

Coding-agent conversation history must not be required to continue development.

Persistent context lives in:

* `AGENTS.md`
* `docs/PRD.md`
* `docs/TECH_SPEC.md`
* `docs/ROADMAP.md`
* `docs/STATUS.md`
* `docs/DECISIONS.md`
* Git history
* current source code

## Reason

Agent sessions may hit context/token limits or be replaced by new sessions.

A new agent must be able to continue development by reading the repository.

---

# D-003 — Kotlin + Jetpack Compose

**Status:** Accepted

## Decision

Primary application implementation will use:

* Kotlin
* Jetpack Compose
* Material 3

## Reason

This is a modern native Android application and Compose provides an appropriate UI model for the project.

---

# D-004 — Android 11+ initial support

**Status:** Accepted

## Decision

Set `minSdk = 30` (Android 11).

## Reason

* Shizuku requires modern Android permissions.
* StorageStatsManager requires modern storage isolation.
* Android 11+ covers the vast majority of target users without legacy storage baggage.

---

# D-005 — UserService over IPC over AIDL for privileged operations

**Status:** Accepted

## Decision

Privileged operations run in a dedicated `Shizuku UserService` interacting via strongly-typed AIDL interface (`ICacheSweepPrivilegedService`).

## Reason

* Provides clear process isolation.
* Prevents arbitrary shell execution vulnerabilities.
* Keeps privileged command execution strictly inside service boundaries.

---

# D-006 — Command safety enforcement

**Status:** Accepted

## Decision

* Never execute `pm clear PACKAGE` without `--cache-only`.
* Unit test all command builders.
* Expose no generic shell API.

## Reason

Safety of user data is the primary constraint of CacheSweep.

---

# D-007 — Pure Kotlin Test Fixture in place of mockito/mockk in unit tests

**Status:** Accepted

## Decision

Use pure Kotlin fake implementations (e.g. `FakeShizukuPrivilegedService`) for fast, hermetic, zero-reflection unit tests instead of heavy mocking frameworks that suffer from ByteBuddy/JDK incompatibility.

---

# D-008 — Progressive StorageStats Scanning with Concurrency Limiting

**Status:** Accepted

## Decision

* Query `StorageStatsManager.queryStatsForPackage` asynchronously across installed packages.
* Cap concurrency via Kotlin Coroutines `Semaphore(4)`.
* Expose progressive scan state via Kotlin `Flow<ScanState>`.
* Catch individual package lookup failures and record them without aborting the entire scan.

## Reason

Ensures UI responsiveness, prevents memory spikes on devices with hundreds of applications, and protects against total scan failure when system packages or uninstalled UIDs throw exceptions.

---

# D-009 — Capability-Gated Cache Clearing Backend

**Status:** Accepted

## Decision

1. Probe device capability dynamically for `pm clear --cache-only <PKG>` and `pm trim-caches <BYTES>`.
2. Gate selective cache cleaning behind capability check.
3. Fall back to global trimming when selective cache clearing is unsupported, requiring user confirmation.

## Reason

Prevents fatal crashes or command failures on OEM ROMs where `--cache-only` might be restricted or unsupported.

---

# D-010 — Real-Time Shizuku IPC UserService Discovery & Capability Auto-Detection

**Status:** Accepted

## Decision

* Manage Shizuku connection state via `ShizukuManager` and broadcast reactive state via `StateFlow<ShizukuState>`.
* Automatically bind UserService upon receiving Shizuku binder and permission.
* Probe and cache cleaner capabilities on service connect.

---

# D-011 — Atomic Multi-Stage Cleanup State Machine

**Status:** Accepted

## Decision

Coordinate cleanup workflow through `CleanupCoordinator` state machine:
`Validating` -> `SnapshotBefore` -> `Clearing` -> `WaitingForStats` -> `SnapshotAfter` -> `Completed` (or `Failed`).

---

# D-012 — Bounded Storage-Stat Settling Delay

**Status:** Accepted

## Decision

Apply a 750ms settling delay after cache clearing operations before querying `StorageStatsManager` and `StatFs` for post-clean metrics.

## Reason

Android OS asynchronously recalculates quota and disk usage stats; immediate post-clean queries often report stale cache sizes.

---

# D-013 — Non-Misleading Metrics and Clamped Physical Deltas

**Status:** Accepted

## Decision

* Clamp negative physical freed storage deltas to 0 B.
* Require at least 1 MB change to report significant reclaim.
* Display both physical storage delta and reported cache reduction side-by-side with clear explanatory labels.

## Reason

Satisfies PRD FR-012/FR-013 requirements for transparency and prevents user confusion from background OS disk activity.

---

# D-028 — DataStore Preferences for Onboarding and Settings Persistence

**Status:** Accepted

## Context

CacheSweep needs to persist user preferences (such as showing system apps, zero-cache filtering, sort order, and theme) and track whether the initial onboarding wizard has been completed.

## Decision

1. Use AndroidX DataStore Preferences (`user_settings.preferences_pb`) via a dedicated `UserSettingsRepository` interface and `DataStoreUserSettingsRepository` implementation.
2. Model user preferences in a typed `UserSettings` data class with safe defaults (`onboardingCompleted = false`, `showSystemApps = false`, `showZeroCacheApps = true`, `sortMode = CACHE_DESC`, `themeMode = SYSTEM`).
3. Handle corrupted/missing preference files gracefully with `IOException` catching emitting default preferences.
4. Drive top-level navigation destination in `MainActivity` reactively from `UserSettings.onboardingCompleted`.

## Reason

Provides type-safe, non-blocking asynchronous persistence with reactive `Flow` emissions, eliminating main-thread disk I/O while maintaining testability without requiring a full SQLite/Room database for basic settings.

## Consequences

UI state and settings are synchronized reactively across all screens.

---

# D-029 — Bounded Local Cleanup History Persistence in DataStore

**Status:** Accepted

## Context

CacheSweep needs to preserve a short local history of past cleanup operations (timestamps, cleanup mode, package counts, measured storage freed, and reported cache reduced) without introducing heavy SQLite/Room dependencies, cloud databases, or network synchronization.

## Decision

1. Model cleanup records in `CleanupHistoryEntry` conforming to TECH_SPEC Section 49.
2. Implement `CleanupHistoryRepository` and `DataStoreCleanupHistoryRepository` using AndroidX DataStore Preferences (`cleanup_history.preferences_pb`).
3. Store records using pure Kotlin delimiter serialization and enforce a strict capacity limit of 25 records (`MAX_HISTORY_ENTRIES = 25`), automatically dropping the oldest records when new entries are recorded.
4. Provide immediate, non-blocking history clearance via `clearHistory()`.
5. Record history asynchronously inside `CleanupCoordinator` upon completion of valid cleanups.

## Reason

Keeps the architecture minimal, private, and 100% offline while satisfying product requirements for auditability and history inspection.

## Consequences

Cleanup history is preserved across app restarts, bounded in size, and completely isolated to local storage without any external dependencies.

---

# D-030 — Material 3 Accessible Design, High-Contrast Color Palette & Screen Reader Support

**Status:** Accepted

## Context

CacheSweep requires an accessible visual presentation across light and dark system themes, dynamic font scaling (1.0x - 2.0x), and screen reader (TalkBack) assistance without decorative fluff or inaccessible text abbreviations.

## Decision

1. Implement Material 3 color system with WCAG AAA compliant contrast tokens for Light (`#0F6687` Primary / `#F6FAFD` Background) and Dark (`#72D2FF` Primary / `#0F1417` Background) schemes.
2. Use pure SP units across all typography tokens (`displayLarge` through `labelSmall`) with proportional line heights to guarantee robust dynamic font scaling.
3. Configure `WindowCompat` status and navigation bar appearance so system icons remain sharp across light and dark modes.
4. Implement `ByteFormatter.formatAccessible` to expand byte figures into spoken TalkBack words (e.g. `1.42 gigabytes of cache`) and merge semantic row descriptions for smooth navigation.
5. Apply semantic `heading()` attributes to screen and section headers and enforce minimum 48x48 dp touch target dimensions across all interactive buttons, chips, and list rows.

## Reason

Ensures CacheSweep is usable for all users, respects system accessibility settings, and delivers a professional personal Android utility UX.

## Consequences

Full accessibility and Material 3 compliance across all screens, passing Phase 4 Product UI & Persistence gates.

---

# D-031 — Selective Cache Clear Root-Privilege Gating and Global Cache Trim Default

**Status:** Accepted

## Context

On modern Android (11+) non-root configurations with Shizuku (UID 2000), `pm help` advertises the `--cache-only` flag. However, `PackageManagerService` enforces signature-level permission `android.permission.INTERNAL_DELETE_CACHE_FILES` when `--cache-only` is passed, silently dropping the operation without invoking the observer callback. This causes `pm clear --cache-only` to hang and time out for UID 2000. Conversely, `pm trim-caches <DESIRED_FREE_SPACE>` is fully authorized for UID 2000, executing in milliseconds and reclaiming real physical cache storage.

## Decision

1. In `CapabilityProbe`, gate `supportsSelectiveCacheClear` strictly behind root privilege (`Process.myUid() == 0`). Non-root Shizuku sessions (UID 2000) evaluate `supportsSelectiveCacheClear = false` and `supportsTrimCaches = true`.
2. On non-root devices, configure Dashboard and App Cache List action buttons to default to Global Cache Trimming (`pm trim-caches`), executing immediate and safe system-wide cache reclamation.
3. Keep per-app storage inspection fully available via `StorageStatsManager` and `AppDetailBottomSheet`, providing direct one-tap shortcuts to native Android Application Storage Settings (`PackageShortcuts.openStorageSettings`) for manual per-app clearance.

## Reason

Prevents command timeouts and failed cleanup attempts on Android non-root devices, ensuring the one-tap cache cleaning operation works reliably via `pm trim-caches` while maintaining transparent per-app storage visibility.

## Consequences

CacheSweep operates reliably on non-root Shizuku configurations with instantaneous global cache trimming, while selective single/multi-app cleaning is safely capability-gated.

---

# D-032 — Retirement of Phase 0 Fixture Subproject & Diagnostic Spike Screen

**Status:** Accepted

## Context

During Phase 0 (Technical Feasibility), a secondary Gradle subproject `:fixture` (`my.id.rmalan.cache.fixture`) and a developer `DiagnosticScreen` workbench were implemented to validate Shizuku Binder communication, measure storage with `StorageStatsManager`, probe `pm` capabilities, and execute disposable safety tests against dummy test cache without endangering real app data.

Now that Phase 0 through Phase 4 are completed and the full production UI (Onboarding, Dashboard, App Cache List, Cleaning Progress, Cleanup Result, Settings) is in place and verified, the test fixture subproject and diagnostic screen are no longer needed.

## Decision

1. Remove `:fixture` subproject from `settings.gradle.kts` and delete the `fixture/` directory.
2. Remove `DiagnosticScreen.kt`, `SafetyTestManager.kt`, and `SafetyTestManagerTest.kt`.
3. Simplify `MainActivity` destinations and remove diagnostic debug action triggers from `DashboardScreen` and `OnboardingScreen`.
4. Update codebase security audits and command tests to audit the single production `:app` module.

## Reason

Adheres to simplicity guidelines ("stupid simple code"), eliminates dead spike code and developer bypass buttons from production UI, reduces APK bundle size, and streamlines the Gradle project into a single `:app` module.

## Consequences

Single-module project structure with faster build times, cleaner UI navigation, and zero dead code while retaining all strict safety and security invariants.

---

# D-033 — Neobrutalism Design System Implementation Across Whole Application

**Status:** Accepted

## Context

The previous UI utilized default Material 3 styling with soft elevations, subtle tonal containers, and pastel dynamic colors that reduced visual punch and contrast. To establish a distinctive, tactile, utilitarian aesthetic suited for an offline systems-level Android utility, the user requested transitioning the entire application to Neobrutalism design.

## Decision

1. **Design Tokens & Palette**:
   - Light Theme: `#F7F5F0` Warm Canvas, `#FFFFFF` Surface, `#121417` Solid Jet Black Outlines (2dp–2.5dp width), `#FFD028` Electric Yellow Primary, `#00C9A7` Mint Secondary, `#8B5CF6` Purple Tertiary, `#FF5252` Coral Error.
   - Dark Theme (Cyber-Brutalist): `#121417` Charcoal Canvas, `#1E2228` Slate Surface, `#E4E4E7` Chalk Outlines, `#D4FF00` Neon Volt Primary, `#00F0FF` Cyber Cyan Secondary.
   - Disabled `dynamicColor` by default so device-level pastel palettes do not wash out the high-contrast aesthetic.
2. **Core Component Library (`NeoComponents.kt`)**:
   - `NeoCard`: Solid unblurred drop shadows (`offset(4.dp, 4.dp)`), 2dp solid outlines, flat container surfaces.
   - `NeoButton`: Chunky tactile button with 2dp border, solid shadow underlay, bold typography.
   - `NeoBadge`: Solid pill badges with solid borders for metadata, system tags, and byte values.
   - `NeoProgressBar`: Rectangular bordered gauge with flat solid progress fill.
3. **Application-Wide Adoption**:
   - Updated all screens: `DashboardScreen`, `AppCacheListScreen`, `AppDetailBottomSheet`, `CleanupConfirmationDialog`, `CleaningProgressScreen`, `CleanupResultScreen`, `PartialFailuresSection`, `SettingsScreen`, `OnboardingScreen`, and `AppCacheRow`.
4. **Safety & Accessibility Invariants**:
   - Maintained all strict SP typography units for system font scaling.
   - Preserved all TalkBack accessibility labels and screen reader semantic descriptors.
   - Maintained strict zero-command security guarantees (zero `pm clear` without `--cache-only`, zero `sh -c`).

## Reason

Delivers a high-contrast, tactile, modern aesthetic across all application screens with zero unnecessary complexity or bloat ("stupid simple code"), ensuring clear readability of disk statistics and satisfying user requirements.

## Consequences

CacheSweep features a consistent, bold Neobrutalist design system in both Light and Cyber-Brutalist Dark themes with 100% unit test coverage passing.

---

# D-034 — Privilege and Permission Failure Hardening Architecture

**Status:** Accepted

## Context

In real-world Android usage, external dependencies and system permissions may fail at any time:
- Shizuku may be uninstalled or killed by OEM background task killers.
- The privileged Shizuku binder may die mid-operation while processing multi-package cache clearing or global trimming.
- Shizuku permission or Usage Access permission may be denied or revoked while the app is in the background.
- External package manager operations may throw `SecurityException` or `DeadObjectException`.

## Decision

1. **Defensive API Wrapping & IPC Isolation**:
   - Wrap all `Shizuku.*` and `AppOpsManager` invocations in safe try-catch blocks with typed fallback states (`ShizukuState.NotRunning`, `ShizukuState.Error`, `hasUsageAccess = false`).
   - In `ShizukuCacheCleaner`, catch `DeadObjectException` during batch processing and fail remaining packages immediately with `CleanerError.ShizukuUnavailable` to avoid hanging or repeated failed IPC attempts on a dead binder.
2. **Reactive Observation**:
   - Have `DashboardViewModel`, `AppsViewModel`, and `CleanerViewModel` observe `ShizukuManager.state` reactively, updating capabilities and action states dynamically when Shizuku starts, stops, or changes permissions.
3. **Usage Access Revocation Handling**:
   - Check `UsageAccessManager.hasAccess()` in `DashboardViewModel` and `AppsViewModel` during initialization, refresh, and resume.
   - Display prominent Neobrutalist warning banners on `DashboardScreen` and `AppCacheListScreen` when Usage Access is missing, providing direct one-tap shortcuts to Android Usage Access Settings.
4. **Resilient Cleanup Coordination**:
   - Ensure `CleanupCoordinator` safely captures before/after snapshots and deltas, handles partial failure attributions, and transitions cleanly to `Completed` or `Failed` without leaving the user stuck on `Clearing` or `WaitingForStats`.
5. **Safe Intent Launches**:
   - Protect all external Activity launches (`ACTION_USAGE_ACCESS_SETTINGS`, Shizuku launch intent) with try-catch and friendly Toast feedback when apps are missing.

## Reason

Ensures CacheSweep adheres to zero-crash, non-hanging reliability across all Android devices and edge cases while keeping the codebase simple, modular, and testable ("stupid simple code").

## Consequences

CacheSweep operates robustly against Shizuku process crashes, permission revocation, and background OS changes with 225/225 unit tests passing cleanly.


