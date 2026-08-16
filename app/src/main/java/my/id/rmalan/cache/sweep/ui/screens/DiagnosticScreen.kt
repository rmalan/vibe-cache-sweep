package my.id.rmalan.cache.sweep.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import my.id.rmalan.cache.sweep.di.AppContainer
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.shizuku.PrivilegedBackendInfo
import my.id.rmalan.cache.sweep.util.ByteFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    container: AppContainer,
    modifier: Modifier = Modifier,
    onOpenAppList: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shizukuState by container.shizukuManager.state.collectAsState()
    val isUserServiceConnected by container.shizukuManager.userServiceConnected.collectAsState()

    var hasUsageAccess by remember { mutableStateOf(container.usageAccessManager.hasAccess()) }
    var storageInfo by remember { mutableStateOf<DeviceStorageInfo?>(null) }
    var capabilities by remember { mutableStateOf<CleanerCapabilities?>(null) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    var currentScanState by remember { mutableStateOf<ScanState?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Privileged Backend state (P0-25 to P0-34)
    var privilegedBackendInfo by remember { mutableStateOf<PrivilegedBackendInfo?>(null) }
    var isPingingBackend by remember { mutableStateOf(false) }

    // Test package storage inspector state (P0-15)
    var testPackageInput by remember { mutableStateOf(context.packageName) }
    var inspectedAppInfo by remember { mutableStateOf<AppCacheInfo?>(null) }
    var isInspectingPackage by remember { mutableStateOf(false) }
    var inspectError by remember { mutableStateOf<String?>(null) }

    fun refreshAll() {
        hasUsageAccess = container.usageAccessManager.hasAccess()
        storageInfo = container.deviceStorageRepository.snapshot()
        container.shizukuManager.updateState()
        scope.launch {
            capabilities = container.shizukuManager.fetchCapabilities()
        }
    }

    LifecycleResumeEffect(Unit) {
        refreshAll()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CacheSweep Diagnostic Spike") },
                actions = {
                    if (onOpenAppList != null) {
                        Button(
                            onClick = onOpenAppList,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("App List")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System & Storage Card (P0-12)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("System & Storage (StatFs)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    DiagnosticRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    DiagnosticRow("Device Model", "${Build.MANUFACTURER} ${Build.MODEL}")
                    storageInfo?.let {
                        DiagnosticRow("Free Storage", ByteFormatter.format(it.availableBytes))
                        DiagnosticRow("Used Storage", ByteFormatter.format(it.usedBytes))
                        DiagnosticRow("Total Storage", ByteFormatter.format(it.totalBytes))
                    }
                }
            }

            // Usage Access Card (P0-08 to P0-11)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Usage Access Permission", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    DiagnosticRow("Status", if (hasUsageAccess) "GRANTED" else "NOT GRANTED")
                    if (!hasUsageAccess) {
                        Button(
                            onClick = {
                                context.startActivity(container.usageAccessManager.createSettingsIntent())
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Usage Access Settings")
                        }
                    }
                }
            }

            // Test Package StorageStats Inspector Card (P0-14, P0-15)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Package StorageStats Inspector", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()

                    OutlinedTextField(
                        value = testPackageInput,
                        onValueChange = {
                            testPackageInput = it
                            inspectError = null
                        },
                        label = { Text("Target Package Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                testPackageInput = context.packageName
                                inspectError = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Self App")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val pkg = testPackageInput.trim()
                                    if (pkg.isBlank()) {
                                        inspectError = "Please enter a valid package name."
                                        return@launch
                                    }
                                    isInspectingPackage = true
                                    inspectError = null
                                    try {
                                        val info = container.cacheScanner.scanPackage(pkg)
                                        if (info != null) {
                                            inspectedAppInfo = info
                                        } else {
                                            inspectError = "Package '$pkg' not found on device."
                                        }
                                    } catch (e: Exception) {
                                        inspectError = "Error querying StorageStats: ${e.message}"
                                    } finally {
                                        isInspectingPackage = false
                                    }
                                }
                            },
                            enabled = hasUsageAccess && !isInspectingPackage,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isInspectingPackage) "Inspecting..." else "Inspect Stats")
                        }
                    }

                    inspectError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    inspectedAppInfo?.let { app ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DiagnosticRow("App Label", app.appName)
                        DiagnosticRow("Package", app.packageName)
                        DiagnosticRow("App / Code Size", "${ByteFormatter.format(app.appBytes)} (${app.appBytes} B)")
                        DiagnosticRow("Cache Size", "${ByteFormatter.format(app.cacheBytes)} (${app.cacheBytes} B)")
                        DiagnosticRow("Data Size", "${ByteFormatter.format(app.dataBytes)} (${app.dataBytes} B)")
                        DiagnosticRow("Total Storage", "${ByteFormatter.format(app.totalBytes)} (${app.totalBytes} B)")
                        DiagnosticRow("App Classification", if (app.isSystemApp) "System App" else "User App")
                        DiagnosticRow("Measurement Status", if (app.measurementAvailable) "SUCCESS" else "FAILED / INACCESSIBLE")
                    }
                }
            }

            // Shizuku & Privileged Backend Card (P0-17 to P0-34)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shizuku & Privileged Backend (AIDL)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    val stateText = when (val s = shizukuState) {
                        is ShizukuState.Ready -> "Ready (UID ${s.uid})"
                        is ShizukuState.PermissionRequired -> "Permission Required"
                        is ShizukuState.Connecting -> "Connecting..."
                        is ShizukuState.NotRunning -> "Not Running"
                        is ShizukuState.Error -> "Error: ${s.reason}"
                    }
                    DiagnosticRow("Shizuku State", stateText)
                    DiagnosticRow("UserService Binder", if (isUserServiceConnected) "BOUND & CONNECTED" else "DISCONNECTED")

                    capabilities?.let { cap ->
                        DiagnosticRow("Selective Clear (--cache-only)", if (cap.supportsSelectiveCacheClear) "SUPPORTED" else "UNSUPPORTED")
                        DiagnosticRow("Global Trim (trim-caches)", if (cap.supportsGlobalTrim) "SUPPORTED" else "UNSUPPORTED")
                    }

                    if (shizukuState is ShizukuState.PermissionRequired) {
                        Button(
                            onClick = { container.shizukuManager.requestPermission() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Request Shizuku Permission")
                        }
                    }

                    if (shizukuState is ShizukuState.Ready) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isPingingBackend = true
                                    try {
                                        privilegedBackendInfo = container.shizukuManager.pingPrivilegedBackend()
                                        capabilities = container.shizukuManager.fetchCapabilities()
                                    } finally {
                                        isPingingBackend = false
                                    }
                                }
                            },
                            enabled = !isPingingBackend,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isPingingBackend) "Pinging UserService..." else "Ping Privileged AIDL Service")
                        }

                        privilegedBackendInfo?.let { info ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "AIDL IPC Ping Verification:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            DiagnosticRow("IPC Connected", if (info.connected) "SUCCESS (Alive)" else "FAILED")
                            info.protocolVersion?.let { DiagnosticRow("AIDL Protocol Version", "$it") }
                            info.privilegedUid?.let { DiagnosticRow("AIDL Privileged UID", "$it (shell/adb)") }
                            DiagnosticRow("Probe Selective Clear", if (info.selectiveClearSupported) "SUPPORTED" else "UNSUPPORTED")
                            DiagnosticRow("Probe Global Trim", if (info.globalTrimSupported) "SUPPORTED" else "UNSUPPORTED")
                            info.lastError?.let { DiagnosticRow("Backend Last Error", it) }
                        }
                    }
                }
            }

            // Full Scanner Card (P0-13, P0-14, P0-15)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Full StorageStats Scanner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()

                    if (isScanning) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val scanState = currentScanState
                            if (scanState is ScanState.Scanning) {
                                LinearProgressIndicator(
                                    progress = { scanState.progressFraction },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Scanning applications: ${scanState.scannedCount} of ${scanState.totalCount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                scanState.currentAppName?.let { appName ->
                                    Text(
                                        text = appName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Reported Cache: ${ByteFormatter.format(scanState.runningReportedCacheBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "Discovering installed applications...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        scanResult?.let { res ->
                            DiagnosticRow("Attempted Apps", "${res.attemptedApps}")
                            DiagnosticRow("Measured Apps", "${res.successfulApps}")
                            DiagnosticRow("Reported Cache", ByteFormatter.format(res.totalReportedCacheBytes))
                            DiagnosticRow("Scan Duration", "${res.durationMillis} ms")

                            if (res.apps.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Top Scanned Apps (Cache Size):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                val topApps = res.apps
                                    .filter { it.measurementAvailable }
                                    .sortedByDescending { it.cacheBytes }
                                    .take(8)

                                topApps.forEach { app ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                testPackageInput = app.packageName
                                                inspectedAppInfo = app
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = app.appName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = ByteFormatter.format(app.cacheBytes),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = app.packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "App: ${ByteFormatter.format(app.appBytes)} | Data: ${ByteFormatter.format(app.dataBytes)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } ?: Text("No scan performed yet.")

                        Button(
                            onClick = {
                                scope.launch {
                                    isScanning = true
                                    try {
                                        container.cacheScanner.scanFlow().collect { state ->
                                            currentScanState = state
                                            when (state) {
                                                is ScanState.Complete -> {
                                                    scanResult = state.result
                                                    refreshAll()
                                                }
                                                is ScanState.Failed -> {
                                                    statusMessage = "Scan error: ${state.message}"
                                                }
                                                else -> Unit
                                            }
                                        }
                                    } catch (e: Exception) {
                                        statusMessage = "Scan error: ${e.message}"
                                    } finally {
                                        isScanning = false
                                    }
                                }
                            },
                            enabled = hasUsageAccess && !isScanning,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Run Full Cache Scan")
                        }

                        if (onOpenAppList != null) {
                            OutlinedButton(
                                onClick = onOpenAppList,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Full App Cache List (Search & Sort)")
                            }
                        }
                    }
                }
            }

            // Safety Verification Card (P0-35 to P0-44)
            var safetyTestReport by remember { mutableStateOf<my.id.rmalan.cache.sweep.cleaner.SafetyTestReport?>(null) }
            var isRunningSafetyTest by remember { mutableStateOf(false) }
            var fixtureStatus by remember { mutableStateOf<my.id.rmalan.cache.sweep.cleaner.FixtureStatusResult?>(null) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selective Cache Clearing Safety Test (P0-35 - P0-44)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Text(
                        text = "Verifies that executing selective cache clearing using --cache-only safely removes cache files while keeping SharedPreferences, SQLite databases, and app files 100% intact.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    DiagnosticRow("Target Fixture", my.id.rmalan.cache.sweep.cleaner.SafetyTestManager.FIXTURE_PACKAGE)

                    fixtureStatus?.let { fs ->
                        DiagnosticRow("Fixture Reachable", if (fs.connected) "CONNECTED" else "UNREACHABLE")
                        if (fs.connected) {
                            DiagnosticRow("Fixture Cache", "${ByteFormatter.format(fs.cacheBytes)} (${fs.cacheFilesCount} files)")
                            DiagnosticRow("Fixture Prefs", if (fs.prefsIntact) "INTACT" else "CORRUPT / MISSING")
                            DiagnosticRow("Fixture Files", if (fs.filesIntact) "INTACT" else "CORRUPT / MISSING")
                            DiagnosticRow("Fixture SQLite DB", if (fs.dbIntact) "INTACT" else "CORRUPT / MISSING")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    fixtureStatus = container.safetyTestManager.populateFixture()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Populate Fixture")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isRunningSafetyTest = true
                                    try {
                                        safetyTestReport = container.safetyTestManager.runSafetyTest(
                                            cacheCleaner = container.cacheCleaner,
                                            cacheScanner = container.cacheScanner,
                                            userId = 0
                                        )
                                        fixtureStatus = safetyTestReport?.finalFixtureStatus
                                        refreshAll()
                                    } finally {
                                        isRunningSafetyTest = false
                                    }
                                }
                            },
                            enabled = shizukuState is ShizukuState.Ready && !isRunningSafetyTest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isRunningSafetyTest) "Testing..." else "Run Safety Pipeline")
                        }
                    }

                    safetyTestReport?.let { report ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = if (report.passed) "SAFETY VERIFICATION PASSED" else "SAFETY VERIFICATION FAILED",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (report.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        DiagnosticRow("Command Executed", report.clearCommand)
                        DiagnosticRow("Exit Code Success", if (report.clearSuccess) "YES (0)" else "NO")
                        DiagnosticRow("Cache Decreased", if (report.cacheDecreased) "YES (Cache Cleaned)" else "NO")
                        DiagnosticRow("SharedPreferences Preserved", if (report.prefsPreserved) "YES (100% INTACT)" else "NO / LOST")
                        DiagnosticRow("App Files Preserved", if (report.filesPreserved) "YES (100% INTACT)" else "NO / LOST")
                        DiagnosticRow("SQLite Database Preserved", if (report.dbPreserved) "YES (100% INTACT)" else "NO / LOST")
                        Text(
                            text = report.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (report.passed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Global Cache Trimming Card (P0-45 to P0-49)
            var isTrimmingGlobally by remember { mutableStateOf(false) }
            var globalTrimReport by remember { mutableStateOf<my.id.rmalan.cache.sweep.cleaner.GlobalTrimReport?>(null) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Global Cache Trimming (P0-45 - P0-49)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Text(
                        text = "Tests the fallback global cache trimming operation ('pm trim-caches <DESIRED_FREE_SPACE>') and measures physical storage deltas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isTrimmingGlobally = true
                                try {
                                    val beforeStorage = container.deviceStorageRepository.snapshot()
                                    val beforeScan = scanResult?.totalReportedCacheBytes ?: 0L

                                    // Target: ask Android to free up space (current available + 1GB or reported cache)
                                    val targetFreeBytes = beforeStorage.availableBytes + maxOf(beforeScan, 1024L * 1024L * 1024L)

                                    val success = container.cacheCleaner.trimGlobally(targetFreeBytes)
                                    kotlinx.coroutines.delay(800) // Settling delay

                                    val afterStorage = container.deviceStorageRepository.snapshot()
                                    val afterScanResult = container.cacheScanner.scan()
                                    scanResult = afterScanResult
                                    val afterScan = afterScanResult.totalReportedCacheBytes

                                    val physicalFreed = maxOf(0L, afterStorage.availableBytes - beforeStorage.availableBytes)
                                    val cacheDelta = maxOf(0L, beforeScan - afterScan)

                                    globalTrimReport = my.id.rmalan.cache.sweep.cleaner.GlobalTrimReport(
                                        timestamp = System.currentTimeMillis(),
                                        physicalFreeBefore = beforeStorage.availableBytes,
                                        reportedCacheBefore = beforeScan,
                                        desiredFreeTarget = targetFreeBytes,
                                        trimSuccess = success,
                                        physicalFreeAfter = afterStorage.availableBytes,
                                        reportedCacheAfter = afterScan,
                                        physicalFreedDelta = physicalFreed,
                                        reportedCacheDelta = cacheDelta,
                                        summary = if (success) {
                                            "Global trim executed successfully. Physical free space: ${ByteFormatter.format(beforeStorage.availableBytes)} -> ${ByteFormatter.format(afterStorage.availableBytes)} (Delta: ${ByteFormatter.format(physicalFreed)})."
                                        } else {
                                            "Global trim operation failed."
                                        }
                                    )
                                    refreshAll()
                                } finally {
                                    isTrimmingGlobally = false
                                }
                            }
                        },
                        enabled = shizukuState is ShizukuState.Ready && !isTrimmingGlobally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isTrimmingGlobally) "Trimming Caches..." else "Execute Global Cache Trim (pm trim-caches)")
                    }

                    globalTrimReport?.let { trim ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DiagnosticRow("Operation Status", if (trim.trimSuccess) "SUCCESS (Exit 0)" else "FAILED")
                        DiagnosticRow("Physical Free Before", ByteFormatter.format(trim.physicalFreeBefore))
                        DiagnosticRow("Physical Free After", ByteFormatter.format(trim.physicalFreeAfter))
                        DiagnosticRow("Physical Storage Freed", ByteFormatter.format(trim.physicalFreedDelta))
                        DiagnosticRow("Reported Cache Delta", ByteFormatter.format(trim.reportedCacheDelta))
                        Text(
                            text = trim.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Actions & Status
            statusMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedButton(
                onClick = { refreshAll() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Diagnostic Info")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
