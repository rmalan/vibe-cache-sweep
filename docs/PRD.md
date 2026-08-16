# Product Requirements Document — CacheSweep

**Working title:** CacheSweep
**Product type:** Personal Android utility
**Platform:** Android
**Distribution:** Sideloaded APK for personal use
**Primary privilege mechanism:** Shizuku / ADB shell privileges
**Document version:** 1.0
**Status:** Draft for implementation
**Target MVP:** Android 11+
**Primary language:** Kotlin
**UI framework:** Jetpack Compose

---

## 1. Executive Summary

CacheSweep is a lightweight Android utility designed for a user's own device to inspect application cache usage and reclaim storage occupied by disposable application cache files.

Modern Android intentionally prevents ordinary third-party applications from directly deleting the private cache directories of other apps. CacheSweep therefore uses **Shizuku** to perform permitted system-level storage maintenance operations with ADB/shell privileges while avoiding the need to root the device.

The application will provide:

* Total device storage usage.
* Total estimated application cache usage.
* Per-app cache estimates.
* Sorting by largest cache consumers.
* One-tap system cache cleanup.
* Before-and-after storage measurements.
* Manual per-app storage-management shortcuts.
* Clear explanations of what can and cannot be cleaned.
* No advertising.
* No analytics.
* No account.
* No cloud dependency.
* No internet requirement for normal operation.

The product is intended to be transparent rather than presenting itself as a generic "phone booster."

---

# 2. Problem Statement

Android applications can accumulate significant amounts of disposable cached data over time.

Examples include:

* Image caches.
* Video thumbnails.
* Temporary downloads.
* Browser resources.
* API response caches.
* Map tiles.
* Temporary media.
* WebView resources.
* Other regenerable application files.

Android can reclaim some of this storage automatically, but users may want to reclaim it sooner when:

* Device storage becomes low.
* A specific application accumulates unusually large cache data.
* The device has not automatically reclaimed cache.
* The user wants storage available before downloading or recording large files.
* The user simply wants greater visibility into storage usage.

The standard Android UI makes this process cumbersome because cache information is distributed across individual application settings screens.

CacheSweep provides a single-purpose interface for understanding and reclaiming cache storage.

---

# 3. Product Vision

> Give the user a fast, transparent, privacy-preserving way to understand application cache usage and ask Android to reclaim disposable cache storage.

CacheSweep should feel more like a storage diagnostic tool than a traditional "cleaner" application.

The app must never falsely imply that:

* Cleaning cache will make the phone dramatically faster.
* RAM needs to be "cleaned."
* Cache files are dangerous.
* Every reported cache byte can be reclaimed.
* Cache cleanup will permanently reduce application storage.
* Cache cleanup is necessary for routine device health.

---

# 4. Product Principles

## 4.1 Transparent

The user should understand:

* What is being measured.
* What is being deleted.
* Why permissions are required.
* What Shizuku does.
* Why actual reclaimed storage can differ from estimates.

## 4.2 Safe

CacheSweep should primarily target **regenerable cache data**.

It must not intentionally delete:

* User documents.
* Photos.
* Downloads.
* Messages.
* Offline files identified as user data.
* Application accounts.
* Application databases.
* Application preferences.
* Complete application data.

## 4.3 Minimal privilege

Use only the privileges needed to implement the product.

No root requirement for the primary product.

## 4.4 Private

No analytics SDK.

No advertising SDK.

No user account.

No cloud processing.

Normal cleaning functionality should work without internet access.

## 4.5 Predictable

Every destructive action must clearly identify its scope.

"Clear cache" must never secretly mean "clear app data."

---

# 5. Goals

## G1 — Storage visibility

Allow the user to quickly understand how much space application caches consume.

## G2 — Cache cleanup

Allow the user to initiate Android's cache-trimming mechanisms using Shizuku privileges.

## G3 — Measurable results

Show storage/cache measurements before and after cleaning.

## G4 — Simple setup

Guide the user through:

1. Installing/running Shizuku.
2. Granting CacheSweep Shizuku access.
3. Granting Usage Access when required.
4. Running the first scan.

## G5 — Privacy

Operate completely locally.

## G6 — Maintainability

Isolate Android-version-specific and Shizuku-specific functionality behind service abstractions so alternative strategies can be added later.

---

# 6. Non-Goals

The MVP will **not**:

* Clear complete application data.
* Log users out of applications.
* Act as a RAM booster.
* Kill background applications for performance.
* Promise battery improvements.
* Remove photos or videos.
* Delete downloads.
* Delete duplicate files.
* Delete APK files.
* Remove application databases.
* Perform antivirus scanning.
* Optimize CPU performance.
* Modify Android system files.
* Require root.
* Include an arbitrary shell terminal.
* Execute shell commands supplied by the user.
* Upload storage information.
* Automatically uninstall applications.
* Claim a precise amount will be reclaimed before cleanup.

---

# 7. Intended User

## Primary persona

**Device owner / advanced Android user**

Characteristics:

* Owns and controls the device.
* Is comfortable sideloading an APK.
* Is willing to configure Shizuku.
* Wants control over device storage.
* Understands that cache can return as applications are used again.

This is not initially intended as a mass-market Play Store application.

---

# 8. Primary User Stories

### US-01 — See storage status

As a user, I want to see my device's total, used, and available storage so I know whether cleanup is useful.

### US-02 — See cache size

As a user, I want to see approximately how much application cache exists on my phone.

### US-03 — Find large caches

As a user, I want applications sorted by cache size so I can understand which apps are responsible for most cache usage.

### US-04 — Clean cache

As a user, I want to press one button to request that Android reclaim application caches.

### US-05 — See results

As a user, I want to know approximately how much storage was reclaimed.

### US-06 — Manage a specific app

As a user, I want to open an application's Android storage settings directly if I want to manage that application manually.

### US-07 — Understand permissions

As a user, I want a clear explanation before granting Shizuku or Usage Access.

### US-08 — Recover from Shizuku being unavailable

As a user, I want the app to explain what to do when Shizuku is not running.

---

# 9. MVP Scope

The MVP consists of five primary capabilities:

1. **Onboarding**
2. **Storage dashboard**
3. **Application cache scanner**
4. **System cache cleaner**
5. **Cleanup results**

---

# 10. Information Architecture

Primary navigation:

```text
Dashboard
    |
    +-- App Cache
    |
    +-- Clean
    |
    +-- Cleanup Result
    |
    +-- Settings
```

A bottom navigation bar is not necessary for MVP.

The application should favor a simple hierarchical flow.

---

# 11. Core Screen — Dashboard

Example layout:

```text
CacheSweep

DEVICE STORAGE

104 GB used
24 GB available

████████████████░░░░

APPLICATION CACHE

Estimated cache
6.8 GB

312 apps scanned

Largest caches

Instagram              1.42 GB
Chrome                  1.08 GB
YouTube                  711 MB
Maps                     384 MB
Reddit                   298 MB

View all apps →

┌─────────────────────────────┐
│       CLEAN CACHE           │
│                             │
│   Ask Android to reclaim    │
│   disposable cache files    │
└─────────────────────────────┘

Last cleaned: 3 days ago
Freed last time: 2.1 GB
```

---

# 12. Functional Requirements

## FR-001 — Device storage information

The application must display:

* Total storage.
* Used storage.
* Free storage.
* Storage usage percentage.

Values should be refreshed:

* When the dashboard opens.
* After a cleanup completes.
* When the user manually refreshes.

---

## FR-002 — Application discovery

The application should discover installed user applications and relevant system applications where Android permits visibility.

Each application record should contain where available:

* Package name.
* Display name.
* Application icon.
* Version.
* User/system classification.
* Storage usage.
* Cache usage.

Package visibility behavior must be isolated from presentation logic because Android package visibility restrictions vary by configuration.

---

## FR-003 — Usage Access detection

If cache/storage statistics require Usage Access, CacheSweep must detect whether the permission is available.

If unavailable:

```text
Storage access needed

CacheSweep uses Android's Usage Access permission
to calculate storage and cache usage for installed apps.

CacheSweep does not collect browsing activity or send
usage information anywhere.

[ Grant Usage Access ]
```

The button should open the relevant Android settings page.

---

## FR-004 — Cache statistics

For each discoverable application, CacheSweep should attempt to retrieve:

* Cache bytes.
* Application bytes.
* Data bytes.
* Total storage footprint.

The UI must distinguish:

**Cache**

from:

**Total application storage**

to avoid misleading the user.

---

## FR-005 — Cache list

The cache list must support:

* Sort largest to smallest.
* Sort alphabetically.
* Search by application name.
* Refresh.
* App detail opening.

Default sorting:

> Largest cache first.

Apps reporting zero cache may be hidden by default with a toggle to display them.

---

## FR-006 — App row

Each row should display:

* App icon.
* App name.
* Cache size.
* Optional total size.
* Navigation indicator.

Example:

```text
[icon] Instagram
       Cache: 1.42 GB
       Total: 3.86 GB              >
```

---

## FR-007 — App detail

Selecting an application opens a detail page.

Example:

```text
Instagram

Cache               1.42 GB
App                  312 MB
User data            2.12 GB
Total                3.86 GB

Cache is temporary data that the app can
usually recreate when needed.

[ Open Android Storage Settings ]
```

The MVP should not display an automated per-app **Clear Cache** button unless the capability has been validated on the target Android build.

---

# 13. Shizuku Integration

## FR-008 — Shizuku status

The app must detect:

* Shizuku installed/not installed where detectable.
* Shizuku running/not running.
* Permission granted/denied.
* Binder/service availability.

UI states:

### Ready

```text
Shizuku
Connected
```

### Running but permission missing

```text
Shizuku permission required

[ Grant Permission ]
```

### Not running

```text
Shizuku isn't running.

Start Shizuku, then return to CacheSweep.

[ Check Again ]
```

### Unsupported/error

```text
Cache cleaning service unavailable.

You can still inspect cache usage and open
individual app storage settings.
```

---

# 14. Cleaning Strategy

## MVP strategy

CacheSweep should use a **system-wide cache trimming operation through Shizuku/shell privileges**.

The implementation must use a fixed, internally defined operation.

The app must **not accept arbitrary shell commands**.

Conceptually:

```text
CacheSweep
     |
     v
CleanerRepository
     |
     v
ShizukuCleaner
     |
     v
Android package/storage service
     |
     v
System cache trimming
```

Exact Android service invocation should be encapsulated behind the cleaner layer.

---

# 15. Critical Technical Constraint

Android does not expose a stable public API allowing an ordinary application to freely erase another application's private cache directory.

Therefore:

### MVP supports

* Per-app cache measurement where available.
* Global/system cache trimming.
* Direct links to individual Android app storage settings.

### MVP does not guarantee

* Automatic cache deletion for one specific package.
* Excluding specific applications from Android's global cache trimming.
* Exact bytes reclaimed.
* Identical cleaning behavior across manufacturers.

This limitation must influence both UX and marketing language.

---

# 16. Cleanup Flow

## FR-009 — Start cleaning

User taps:

**Clean Cache**

Display confirmation:

```text
Clean application caches?

Android will remove disposable files from
applications where possible.

Apps may recreate these files later.

Your accounts, messages, settings, and
personal files will not intentionally be removed.

Estimated cache currently reported:
6.8 GB

[ Cancel ]          [ Clean ]
```

Do not say:

> Free 6.8 GB

because 6.8 GB is an estimate of cache usage, not a guaranteed reclaim amount.

---

## FR-010 — Pre-clean snapshot

Immediately before cleaning, capture:

* Timestamp.
* Available storage.
* Used storage.
* Estimated aggregate app cache.
* Number of applications scanned.

This becomes the baseline for the result screen.

---

## FR-011 — Cleaning state

While cleaning:

```text
Cleaning cache…

Requesting Android to reclaim temporary
application files.

[ progress indicator ]

Do not close CacheSweep.
```

Progress must be indeterminate unless the platform provides meaningful progress information.

Do not create fake percentage progress.

---

## FR-012 — Post-clean rescan

After the cleaner returns:

1. Wait briefly for storage statistics to settle.
2. Refresh filesystem/device storage values.
3. Rescan app cache statistics.
4. Calculate differences.
5. Display results.

Implementation should allow multiple post-clean samples if Android storage statistics update asynchronously.

---

# 17. Cleanup Result

Example:

```text
Cleanup complete

2.14 GB freed

Before
Available              24.31 GB

After
Available              26.45 GB

Reported cache
Before                   6.8 GB
After                    4.4 GB

Some cache remains because Android decides
which cached files are currently safe to remove.

[ Done ]
```

---

## FR-013 — Result calculations

Two values should be tracked separately:

### Actual free-storage delta

```text
postAvailableBytes - preAvailableBytes
```

### Reported cache delta

```text
preCacheBytes - postCacheBytes
```

These values may differ.

The app should favor the **actual available storage difference** when stating:

> X GB freed

If results are noisy or negative:

```text
Cleanup finished

Storage measurements changed by less than
the amount needed to report a reliable result.
```

Do not display nonsensical results such as:

> -312 MB freed

---

# 18. Cleaning History

MVP may maintain local history for the most recent clean operations.

Record:

* Timestamp.
* Estimated cache before.
* Estimated cache after.
* Available space before.
* Available space after.
* Calculated reclaimed bytes.
* Result status.

Keep a maximum of approximately 20–50 records.

No cloud synchronization.

---

# 19. Settings

Settings screen:

```text
SETTINGS

Scanning
Show system apps                   [ ]
Show zero-cache apps               [ ]

Appearance
Use system theme                   [✓]

Privacy
No analytics
No cloud storage
No advertising

Shizuku
Status: Connected
Check connection

Data
Clear cleanup history

About
Version
Open-source licenses
```

---

# 20. Permissions

The project should use as few permissions as possible.

Potential capabilities include:

## Usage Access

Purpose:

* Read application storage/cache statistics.

User explanation must explicitly state that the application is using Usage Access for storage statistics rather than behavioral profiling.

## Package visibility

Because this is a personal sideloaded utility, the app may use broader installed-application visibility if required for comprehensive scanning.

The implementation must still minimize collection and never transmit the resulting app list.

## Shizuku permission

Purpose:

* Execute the restricted storage/cache maintenance operation.

The application must request Shizuku access only when required.

---

# 21. Explicitly Forbidden Capabilities

CacheSweep must never implement:

```text
User text input -> shell
```

or:

```text
Remote server -> shell command -> device
```

The shell interface must only expose strongly typed internal operations such as:

```kotlin
interface CacheCleaner {
    suspend fun trimCaches(): CleanResult
}
```

rather than:

```kotlin
fun execute(command: String)
```

in application-facing layers.

A low-level executor may internally need command strings, but they must come exclusively from hard-coded trusted application logic.

---

# 22. Suggested Technical Architecture

Use a layered architecture.

```text
UI
│
├── DashboardViewModel
├── AppsViewModel
├── CleanerViewModel
└── SettingsViewModel
        │
        v
DOMAIN
│
├── ScanStorageUseCase
├── ScanAppsUseCase
├── CleanCachesUseCase
└── GetCleanupResultUseCase
        │
        v
DATA
│
├── StorageRepository
├── AppStatsRepository
├── CleanupHistoryRepository
└── CleanerRepository
        │
        ├── AndroidStorageStatsDataSource
        ├── PackageManagerDataSource
        └── ShizukuCleanerDataSource
```

---

# 23. Recommended Technology Stack

## Language

Kotlin

## UI

Jetpack Compose

## Architecture

MVVM + repository/use-case separation.

## Asynchronous operations

Kotlin Coroutines + Flow.

## Local persistence

Room or lightweight DataStore depending on final history requirements.

Suggested:

* DataStore for settings.
* Room for cleanup history if detailed history is retained.

## Dependency injection

Hilt or manual dependency injection.

For a small personal utility, either is acceptable.

## Privileged bridge

Shizuku API.

---

# 24. Proposed Modules

For MVP, a single Android application module is sufficient.

Logical packages:

```text
app/
  ui/
  domain/
  data/
  storage/
  cleaner/
  shizuku/
  permissions/
  model/
  util/
```

If the codebase expands, modules can later become:

```text
:app
:core
:storage
:cleaner
:shizuku
```

Avoid premature modularization.

---

# 25. Domain Models

Example conceptual models:

```kotlin
data class AppCacheInfo(
    val packageName: String,
    val appName: String,
    val cacheBytes: Long,
    val appBytes: Long,
    val dataBytes: Long,
    val totalBytes: Long,
    val isSystemApp: Boolean
)
```

```kotlin
data class DeviceStorageInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long
)
```

```kotlin
data class CleanupSnapshot(
    val timestamp: Long,
    val availableBytes: Long,
    val estimatedCacheBytes: Long
)
```

```kotlin
data class CleanupResult(
    val before: CleanupSnapshot,
    val after: CleanupSnapshot,
    val reclaimedBytes: Long,
    val status: CleanupStatus
)
```

---

# 26. Scanner Behavior

## Scan initiation

Scanning occurs:

* On initial dashboard load after permission setup.
* On pull-to-refresh.
* After cleanup.
* When returning from Android's per-app storage settings.

## Scan execution

Application scanning must run outside the main UI thread.

## Partial results

If scanning hundreds of applications takes noticeable time, results may stream progressively.

Example:

```text
Scanning applications…

147 of 312
```

Unlike cleanup progress, this percentage is valid because the total app count is known.

---

# 27. Sorting

Supported sorting modes:

### Cache size

Default.

Largest first.

### App name

Alphabetical.

### Total storage

Optional MVP+.

---

# 28. Search

Users should be able to search installed applications.

Search fields:

* App display name.
* Package name.

Search should be local and immediate.

---

# 29. App Storage Settings

Each application detail view should include:

**Open Android Storage Settings**

This opens Android's native App Info / Storage UI for that package.

This acts as the safe fallback for users wanting selective cleanup.

---

# 30. Empty States

## No measurable cache

```text
Nothing significant to clean

Android currently reports very little
application cache on this device.
```

## Usage Access unavailable

```text
Cache sizes unavailable

Grant Usage Access to calculate storage
usage for installed applications.

[ Grant Access ]
```

## Shizuku unavailable

Scanning should still function where possible.

```text
Cache cleaning unavailable

Start Shizuku to enable one-tap cleaning.

You can still inspect storage usage.

[ Check Shizuku ]
```

---

# 31. Error Handling

Errors must be expressed in user terms.

Do not display raw Java stack traces.

Examples:

## Shizuku connection lost

```text
Cleaning couldn't start

The Shizuku connection was lost.

Start Shizuku and try again.

[ Try Again ]
```

## Permission denied

```text
Permission not granted

CacheSweep needs Shizuku permission to ask
Android to clean application caches.
```

## Storage stats failure

```text
Some apps couldn't be measured

Storage statistics were unavailable for
17 applications.

[ Continue ]
```

One failed package must not fail the entire scan.

---

# 32. Device Restart Behavior

Shizuku availability may change after reboot.

Therefore CacheSweep must:

* Check Shizuku state every launch.
* Never assume a previous session is still active.
* Keep scanning functionality separate from cleaning capability.
* Clearly display connection status.

The user should not need to reconfigure CacheSweep itself unnecessarily.

---

# 33. Privacy Requirements

## PR-001

No analytics SDK.

## PR-002

No advertising SDK.

## PR-003

No crash reports uploaded automatically.

Debug logs remain on-device.

## PR-004

No server API required.

## PR-005

Application package lists remain local.

## PR-006

Cache statistics remain local.

## PR-007

Cleanup history remains local.

## PR-008

No internet permission should be included unless a future feature explicitly requires it.

---

# 34. Security Requirements

## SR-001

Never provide arbitrary shell command execution through UI.

## SR-002

Never accept shell commands from intents/deep links.

## SR-003

Never accept remote commands.

## SR-004

Validate Shizuku availability immediately before privileged operations.

## SR-005

Avoid logging package-specific information in production unless needed for local debugging.

## SR-006

Do not request root privileges in MVP.

## SR-007

Do not delete application data.

## SR-008

All cleanup operations should fail closed when capabilities are uncertain.

---

# 35. Performance Requirements

### Dashboard

Initial shell/UI should render in:

**< 500 ms** on a modern device, excluding full app scan.

### Application scan

Target:

**< 5 seconds for approximately 300 installed applications** on typical modern hardware.

This is a target rather than hard guarantee.

### UI responsiveness

No storage query may block the Compose main thread.

### Memory

Avoid retaining application icons at full source resolution.

Use appropriately sized image representations/caching.

---

# 36. Accessibility

The UI should:

* Support Android font scaling.
* Provide semantic labels for application icons.
* Not communicate status solely through color.
* Maintain adequate contrast.
* Support TalkBack.
* Use touch targets of at least recommended Android dimensions.
* Format storage values clearly.

Example:

Visual:

> 1.42 GB

TalkBack:

> "One point four two gigabytes of cache."

---

# 37. Theme

Support:

* Light theme.
* Dark theme.
* Follow-system default.

Visual direction:

* Minimal.
* Native Android.
* Material 3.
* No aggressive red warning states for harmless cache.
* Avoid "speedometer" cleaner-app clichés.

---

# 38. Recommended Design Language

The application should communicate confidence through restraint.

Preferred language:

> 2.1 GB of cache reported

> Ask Android to clean cache

> 1.7 GB freed

Avoid:

> 6.8 GB JUNK FOUND!!!

> YOUR PHONE IS SLOW!

> BOOST NOW!!!

> DANGER: STORAGE CRITICAL!

unless Android actually reports critically low storage.

---

# 39. Storage Units

Use binary or decimal units consistently.

Recommended user-facing formatting:

```text
824 KB
83.4 MB
1.42 GB
```

Internally maintain all values as bytes using `Long`.

Never perform cleanup calculations using formatted strings.

---

# 40. First Launch Experience

### Screen 1

```text
CacheSweep

Understand and clean application cache
without rooting your phone.

• See which apps use the most cache
• Reclaim temporary storage
• Everything stays on your device

[ Continue ]
```

### Screen 2

```text
Storage statistics

Android requires Usage Access for CacheSweep
to calculate storage used by other apps.

CacheSweep does not collect your activity.

[ Grant Usage Access ]
```

### Screen 3

```text
Enable cleaning

CacheSweep uses Shizuku to request cache
cleanup with ADB privileges.

Shizuku remains in control of the permission.

[ Connect Shizuku ]
```

### Screen 4

```text
You're ready

[ Scan Device ]
```

---

# 41. Permission Philosophy

Permissions should be requested **contextually**.

Do not request every capability immediately at app startup.

Example:

Usage Access is requested when scanning is introduced.

Shizuku permission is requested when cleaning functionality is introduced.

---

# 42. Refresh Model

Dashboard should display:

```text
Last scanned
2 minutes ago
```

Pull-to-refresh:

1. Refresh device storage.
2. Refresh application list.
3. Refresh application stats.
4. Recalculate aggregate cache.

---

# 43. Aggregate Cache Calculation

The displayed aggregate cache should be the sum of valid cache measurements.

If some applications fail measurement:

```text
Estimated application cache
6.8 GB

295 of 312 apps measured
```

This is more transparent than silently pretending the scan is complete.

---

# 44. Cache Estimate Language

Always call pre-clean cache values:

* "Reported cache"
* "Estimated cache"
* "Cache currently reported"

Never:

* "Guaranteed reclaimable space"
* "Junk"

This matters because Android may keep certain cached resources after cleanup.

---

# 45. Compatibility Strategy

MVP target:

* Android 11
* Android 12
* Android 13
* Android 14
* Android 15
* Android 16

Actual support must be determined through testing.

Compatibility layers should handle differences in:

* Package visibility.
* StorageStats behavior.
* Shizuku availability.
* OEM storage behavior.
* Package-manager cache trimming.
* Background execution restrictions.

---

# 46. OEM Testing Matrix

At minimum test where devices are available:

| Vendor       | Android | Test focus                      |
| ------------ | ------: | ------------------------------- |
| Google Pixel |     14+ | Reference Android behavior      |
| Samsung      |     14+ | One UI package/storage behavior |
| Xiaomi/Redmi |  Recent | OEM restrictions                |
| OnePlus/Oppo |  Recent | Background/Shizuku behavior     |

The application does not need to claim support for devices that have not been tested.

---

# 47. Feasibility Spike — Required Before Full Implementation

Before polishing the full product, create a small technical prototype.

The prototype must answer:

### F-01

Can cache size be obtained reliably for installed applications using StorageStats on the target device?

### F-02

Does a Shizuku-authorized shell-level cache trim successfully reclaim application cache on the target device?

### F-03

How closely does:

```text
sum(per-app reported cache)
```

correlate with:

```text
actual bytes reclaimed
```

### F-04

Does cache trimming behave differently when apps are running?

### F-05

Does the operation work across multiple Android versions?

### F-06

Can Shizuku disconnect during the operation, and how should that condition be handled?

### F-07

Does any available system-level mechanism provide safe package-specific cache clearing?

If F-07 proves reliable across supported devices, selective per-app cleaning can become a later feature.

---

# 48. MVP Acceptance Criteria

The MVP is considered functional when all following criteria are satisfied.

## AC-001

User can install and launch the APK.

## AC-002

App detects whether Usage Access is available.

## AC-003

User can open the correct permission settings.

## AC-004

App enumerates installed applications within the intended visibility scope.

## AC-005

App displays cache usage for applications where statistics are available.

## AC-006

Apps can be sorted largest-cache-first.

## AC-007

User can search applications.

## AC-008

User can open the Android storage settings for an individual application.

## AC-009

App detects Shizuku connection state.

## AC-010

App can request Shizuku authorization.

## AC-011

Cache cleaning cannot start if Shizuku is unavailable.

## AC-012

User receives a clear confirmation before cleanup.

## AC-013

App records storage immediately before cleanup.

## AC-014

App performs the approved cache-trimming operation.

## AC-015

App rescans storage after cleanup.

## AC-016

App displays actual measured storage difference.

## AC-017

App never reports negative freed storage.

## AC-018

One app failing statistics collection does not crash the scan.

## AC-019

No user files are intentionally removed.

## AC-020

No complete application data is intentionally removed.

## AC-021

No network connection is required for scanning or cleaning.

## AC-022

No arbitrary shell command is exposed.

---

# 49. Definition of Done — MVP

The MVP is complete when:

* Primary acceptance criteria pass.
* Tested on at least one physical Android device.
* No crashes occur during a 300+ app scan.
* Cleanup produces measurable results on the target device.
* Shizuku disconnected state is handled.
* Permission denial is handled.
* Device reboot state is handled.
* Dark/light mode work.
* App survives process recreation.
* No internet permission exists.
* Release APK can be produced and sideloaded.

---

# 50. Phase 2 Features

Potential future features:

## Selective cache cleaning

Only if reliable package-specific deletion can be safely implemented.

Potential UX:

```text
Chrome                  1.1 GB   [✓]
Instagram               918 MB   [✓]
Spotify                 612 MB   [ ]
Maps                    481 MB   [ ]

Selected cache
2.0 GB

[ Clean Selected ]
```

This must not ship until technical feasibility is proven.

---

## Exclusion list

Example:

```text
Never automatically clean:

Spotify
Maps
YouTube Music
```

Again, only meaningful if the cleaner can reliably target packages.

---

## Automatic cleaning

Possible triggers:

* Free storage below threshold.
* Cache exceeds threshold.
* Weekly schedule.

Example:

```text
Auto-clean

When free storage falls below:
[ 10 GB ]

Maximum frequency:
[ Once per day ]
```

Requirements:

* Off by default.
* Clearly disclosed.
* Must handle Shizuku unavailable state.
* Must not repeatedly trigger failed operations.

---

## Root mode

Optional advanced mode for rooted devices.

Root support should be an independent cleaning backend:

```text
CacheCleaner
   |
   +-- ShizukuCacheCleaner
   |
   +-- RootCacheCleaner
```

Root should never be mandatory for normal product operation.

---

## Home-screen widget

Potential widget:

```text
Cache
4.8 GB

[ Clean ]
```

A confirmation step may still be required.

---

## Quick Settings tile

Potential tile:

```text
Clean Cache
```

Behavior must remain safe and transparent.

---

## Cleanup notifications

Example:

```text
CacheSweep

Cleanup finished
1.7 GB of storage reclaimed
```

---

# 51. Features Explicitly Deferred

The following are outside current scope:

* Duplicate photo detection.
* Download manager.
* Large-file browser.
* WhatsApp-specific cleaning.
* Social-media-specific cleaning.
* Browser history cleanup.
* Clipboard cleaning.
* RAM cleaning.
* Battery optimization.
* CPU cooling.
* Malware scanning.
* App hibernation.
* App uninstall recommendations.

These could turn CacheSweep into a generic "cleaner suite," which is not currently the goal.

---

# 52. Local Product Metrics

Because production analytics are intentionally absent, useful metrics can be maintained locally.

Examples:

* Last scan duration.
* Last scan package count.
* Number of scan failures.
* Cache before cleanup.
* Cache after cleanup.
* Actual storage reclaimed.
* Cleanup duration.
* Shizuku failure reason.

A developer/debug build may expose:

```text
Diagnostics

Apps scanned          312
Stats failed           4
Scan duration         2.8 s
Cleaner duration      1.1 s
Cache before          6.8 GB
Cache after           4.4 GB
Storage delta         2.1 GB
```

---

# 53. Debug Logging

Debug build may log:

* Lifecycle events.
* Scan duration.
* Package statistic errors.
* Shizuku connection status.
* Cleaner return codes.
* Storage snapshots.

Release build should minimize logs.

Never log:

* Arbitrary file contents.
* User documents.
* Authentication data.
* App-private databases.
* Personal messages.

---

# 54. Failure Scenarios

The product must be designed around these expected failures.

### Scenario A — Shizuku stopped

Scanning works.

Cleaning disabled.

### Scenario B — Usage Access revoked

Device storage still displays.

Per-app cache statistics become unavailable.

### Scenario C — Cache trim reports success but little storage changes

Show:

> Cleanup completed. Android did not reclaim a significant amount of storage.

### Scenario D — App cache grows immediately after cleaning

Expected if running apps recreate resources.

Do not treat this as an application error.

### Scenario E — One package cannot be queried

Skip package and mark scan partial.

### Scenario F — Device storage changed because another process wrote data during cleaning

Actual free-space delta may be noisy.

The UI should describe results as measured rather than guaranteed.

---

# 55. Product Risks

## Risk 1 — OEM inconsistency

System cache trimming may behave differently across manufacturers.

**Mitigation:** capability abstraction and testing matrix.

## Risk 2 — Misleading cache estimates

Reported cache may not equal reclaimable storage.

**Mitigation:** use "estimated/reported cache" language.

## Risk 3 — Shizuku setup complexity

New users may struggle with setup.

**Mitigation:** concise onboarding and connection-status diagnostics.

## Risk 4 — Android changes behavior

Future Android releases may restrict shell functionality.

**Mitigation:** cleaner abstraction and graceful fallback to manual app settings.

## Risk 5 — Users confuse cache with data

Could create fear of losing accounts/files.

**Mitigation:** explicitly state that cache cleanup is not app-data deletion.

---

# 56. UX Success Criteria

A new technical user should be able to:

1. Open CacheSweep.
2. Understand why Usage Access is needed.
3. Connect Shizuku.
4. Scan apps.
5. Identify large caches.
6. Clean cache.
7. Understand the result.

without needing to understand Android package-manager internals.

---

# 57. Main Product Flow

```text
Launch
  |
  v
Check permissions
  |
  +-- Usage Access missing
  |      |
  |      v
  |   Request access
  |
  v
Scan storage
  |
  v
Dashboard
  |
  +-----------------------+
  |                       |
  v                       v
View Apps            Clean Cache
  |                       |
  v                       v
App Detail          Check Shizuku
  |                       |
  v                       +-- unavailable --> explain
Android Settings          |
                          v
                      Confirmation
                          |
                          v
                    Pre-clean snapshot
                          |
                          v
                      Cache trim
                          |
                          v
                       Rescan
                          |
                          v
                       Result
```

---

# 58. Suggested MVP Screen List

Exactly seven screens should be sufficient:

1. Welcome.
2. Permission/setup.
3. Dashboard.
4. Application cache list.
5. Application detail.
6. Cleaning.
7. Cleanup result/settings.

Setup screens can eventually be combined where appropriate.

---

# 59. Recommended Initial Build Order

## Milestone 1 — Storage scanner

Implement:

* Device storage.
* Installed apps.
* StorageStats.
* Cache list.
* Sorting.
* Search.

No cleaning yet.

## Milestone 2 — Shizuku bridge

Implement:

* Shizuku availability.
* Permission.
* Connection handling.
* Fixed privileged test operation.

## Milestone 3 — Cleaner

Implement:

* Cache trim.
* Before snapshot.
* After snapshot.
* Result calculation.
* Failure handling.

## Milestone 4 — UX polish

Implement:

* Onboarding.
* Settings.
* History.
* Dark mode.
* Accessibility.
* Loading/error states.

## Milestone 5 — Device validation

Test multiple Android/OEM configurations and adjust compatibility logic.

---

# 60. Recommended Repository Structure

```text
CacheSweep/
├── app/
│   ├── src/main/
│   │   ├── java/.../
│   │   │   ├── cleaner/
│   │   │   ├── data/
│   │   │   ├── domain/
│   │   │   ├── model/
│   │   │   ├── permissions/
│   │   │   ├── shizuku/
│   │   │   ├── storage/
│   │   │   └── ui/
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   │
│   ├── src/test/
│   └── src/androidTest/
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
└── LICENSE
```

---

# 61. Open Technical Questions

These questions should be answered during the feasibility prototype:

1. What exact Shizuku-backed Android operation provides the most reliable global cache trim on the target device?
2. What shell/API behavior differs between Android 11–16?
3. Can package-specific cache cleaning be implemented reliably without root?
4. How accurately does StorageStats report cache for running applications?
5. How quickly do StorageStats values update after cleaning?
6. Does the target OEM impose additional restrictions?
7. What package visibility declaration is appropriate for the sideloaded build?
8. What happens when multiple Android users/work profiles are present?
9. Should system-app caches be included in the default aggregate?
10. How should cache statistics from external/adoptable storage be represented?

These are implementation questions rather than blockers to defining the product.

---

# 62. MVP Product Decision Summary

**CacheSweep v1 will:**

* Be designed for one user's own Android device.
* Be distributed as a sideloaded APK.
* Use Kotlin and Jetpack Compose.
* Use Shizuku instead of root.
* Measure cache per application.
* Display largest cache consumers.
* Perform a global Android cache trim.
* Measure storage before and after.
* Link to Android settings for selective app management.
* Operate locally.
* Contain no ads or analytics.
* Never intentionally clear complete app data.
* Never expose arbitrary shell execution.

**CacheSweep v1 will not promise:**

* Exact reclaimable cache.
* Automated selective per-app cache deletion.
* Exclusion lists.
* Automatic scheduled cleanup.
* Identical behavior on every Android manufacturer.

Those capabilities should only be added after technical validation.

---

# 63. Final MVP Definition

The smallest version of CacheSweep worth shipping is:

> An Android application that scans installed applications, shows where cache storage is being consumed, connects to Shizuku, asks Android to reclaim disposable application caches, and clearly reports the measured storage difference afterward—without root, telemetry, advertising, or deleting application data.

That should be the implementation target for version 1.0.
