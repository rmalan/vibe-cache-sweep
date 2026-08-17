# CacheSweep Installation & User Guide

This guide provides instructions for installing, configuring, and using CacheSweep on an Android device.

---

## 1. Installation

### Option A: Install via ADB (Recommended)

1. Connect your Android 11+ device via USB or Wireless ADB.
2. Verify device connection:
   ```bash
   adb devices
   ```
3. Install the signed release APK:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

### Option B: Sideload APK directly on Device

1. Copy `app-release.apk` to your device storage (via USB file transfer, LocalSend, or file manager).
2. Open the APK with your device file manager.
3. If prompted, enable "Install unknown apps" for your file manager.
4. Tap **Install**.

---

## 2. Initial Setup & Permissions

When opening CacheSweep for the first time, a 4-step onboarding wizard will guide you:

### Step 1: Welcome & Overview
Review the core value propositions and privacy guarantees:
- **Storage Visibility**: View exact cache and storage footprints for all apps.
- **Safe & Controlled**: Cleans only regenerable cache; never touches user data, logins, or files.
- **100% Private & Local**: Zero internet permission, zero telemetry.

### Step 2: Grant Usage Access
Android requires Usage Access (`PACKAGE_USAGE_STATS`) to query `StorageStatsManager` for per-package storage byte counts:
1. Tap **Grant Usage Access**.
2. Android Settings will open to the *Usage Access* screen.
3. Locate **CacheSweep** in the list and toggle **Permit usage access** ON.
4. Return to CacheSweep.

*Note: You can grant this via ADB directly with:*
```bash
adb shell appops set my.id.rmalan.cache.sweep GET_USAGE_STATS allow
```

### Step 3: Shizuku Privileged Bridge (Optional for Automated Cleaning)
Shizuku provides ADB shell privileges without requiring root, enabling CacheSweep to trigger Android's system cache trimmer (`pm trim-caches`):
1. Ensure the **Shizuku** app is installed and running on your device (via Wireless debugging or ADB).
2. Tap **Grant Shizuku Permission**.
3. In the Shizuku authorization dialog, tap **Allow all the time**.
4. The status will update to **Shizuku Ready (UID 2000)**.

*Note: If Shizuku is not running, CacheSweep remains fully functional for inspecting cache footprints and opening native Android storage settings shortcuts for individual apps.*

### Step 4: First Scan
1. Review the Setup Summary (Usage Access and Shizuku statuses).
2. Tap **Start First Scan** to analyze installed applications.

---

## 3. Daily Usage

### Dashboard
- **Device Storage Card**: Visualizes total physical disk usage, free space, and usage percentage.
- **Application Cache Card**: Displays aggregate estimated cache across all scanned apps, scan count, and scan duration.
- **Hero Clean Button**: One-tap trigger to initiate safe system cache trimming.
- **Largest Caches Preview**: Quick access to the top cache-consuming applications.

### Cleaning Workflow
1. Tap **CLEAN CACHE** on the Dashboard.
2. In the confirmation dialog, review the estimated cache and safety notes.
3. Tap **Clean Cache**.
4. CacheSweep will execute the cleanup state machine:
   - Capture pre-clean physical storage & cache snapshots.
   - Execute system cache trimming.
   - Wait 750ms for Android storage statistics to settle.
   - Rescan packages to measure actual storage reclaimed.
5. The **Cleanup Result** screen displays:
   - Physical storage reclaimed (measured delta).
   - Reported cache reduction.
   - Execution duration.

### App Cache List & Search
- Tap **VIEW ALL APPS** from the Dashboard to view the full application list.
- **Search**: Filter apps in real-time by app name or package identifier.
- **Sort**: Sort by **Cache Size** (descending), **Total Storage** (descending), or **App Name** (A-Z).
- **Hide 0 B**: Toggle to hide apps that currently have 0 B of cache.
- **App Details**: Tap any application row to view the storage breakdown (Cache, App/Code, User Data, Total Storage) and tap **Open Android Storage Settings** to manually clear individual caches.

### Settings & Preferences
- **Show system applications**: Toggle to include/exclude Android OS and pre-installed packages.
- **Show zero-cache applications**: Configure default visibility of apps with 0 B cache.
- **Default sort order**: Choose default sorting preference.
- **App theme**: Choose System default, Light, or Dark (Cyber-Brutalist) theme.
- **Cleanup History**: View saved local cleanup records or tap **Clear Cleanup History** to purge local logs.
