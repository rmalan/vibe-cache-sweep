# CacheSweep

CacheSweep is a personal, offline Android cache-cleaning utility built with Kotlin and Jetpack Compose for Android 11+ (API 30+).

It provides transparent per-application storage inspection and safe system-wide cache reclamation using Shizuku and `StorageStatsManager`, with zero cloud dependencies, zero analytics, and zero internet permissions.

---

## Key Features

- **Offline & Private**: Zero internet permissions (`android.permission.INTERNET` is not requested or declared), zero telemetry, zero analytics tracking, and zero cloud synchronization.
- **Accurate Storage Visibility**: Scans and measures exact per-package storage footprints (Cache, Application/Code, User Data) using Android's native `StorageStatsManager` with bounded background concurrency.
- **Safe Cache Reclamation**: Targets only regenerable application cache files. Never deletes personal accounts, login sessions, messages, photos, SQLite databases, or private application data.
- **Shizuku Integration**: Executes privileged system cache trimming (`pm trim-caches`) via Shizuku ADB shell privileges without requiring root.
- **Neobrutalism Design System**: High-contrast, tactile UI in both Light and Cyber-Brutalist Dark themes with full dynamic font scaling and TalkBack screen reader support.
- **Local History & Preferences**: Retains up to 25 local cleanup audit records in local DataStore Preferences with one-tap history clearing.

---

## System Requirements

- **Operating System**: Android 11 (API level 30) or higher.
- **Permissions**:
  - **Usage Access** (`PACKAGE_USAGE_STATS`): Required to query `StorageStatsManager` per-package byte statistics.
  - **Shizuku Service**: Optional but recommended for automated system-level cache trimming without root.

---

## Building from Source

### Prerequisites

- Java Development Kit (JDK) 17
- Android SDK Platform 36/37 & Build-Tools
- Gradle 8.11+ (or use the included `./gradlew` wrapper)

### Debug Build

```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### Release Signing Configuration

By default, `./gradlew assembleRelease` falls back to the local debug keystore (`~/.android/debug.keystore`) for convenient, reproducible offline builds.

To sign with a custom release keystore, provide the following environment variables or Gradle properties:

```bash
export RELEASE_KEYSTORE_PATH="/path/to/release.keystore"
export RELEASE_KEYSTORE_PASSWORD="keystore_password"
export RELEASE_KEY_ALIAS="key_alias"
export RELEASE_KEY_PASSWORD="key_password"

./gradlew assembleRelease
```

---

## Installation & Setup

For step-by-step setup, Shizuku configuration, and ADB installation commands, see **[Installation & User Guide](docs/INSTALL.md)**.

---

## Running Unit Tests & Audits

CacheSweep contains 260+ automated unit tests, safety invariant verifications, and architectural security audit suites:

```bash
./gradlew testDebugUnitTest
```

Audit tests verify:
- Zero `android.permission.INTERNET` or network socket usage.
- Zero analytics or third-party tracking SDKs.
- Zero arbitrary shell execution or `sh -c` invocations.
- Enforced `--cache-only` invariants on all cache command builders.
- Zero production log leakage (`android.util.Log`, `System.out`).

---

## License

Personal utility project.
