package my.id.rmalan.cache.sweep.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import java.util.Locale
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.scanner.PackageRepository
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.ui.components.AppDetailBottomSheet
import my.id.rmalan.cache.sweep.ui.components.AppIcon
import my.id.rmalan.cache.sweep.ui.components.CleanupConfirmationDialog
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerViewModel
import my.id.rmalan.cache.sweep.ui.viewmodel.DashboardEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.DashboardViewModel
import my.id.rmalan.cache.sweep.util.ByteFormatter
import my.id.rmalan.cache.sweep.util.DashboardTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    cleanerViewModel: CleanerViewModel,
    shizukuManager: ShizukuManager,
    packageRepository: PackageRepository? = null,
    onOpenAppList: () -> Unit,
    onOpenDiagnostic: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val cleanerState by cleanerViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        shizukuManager.updateState()
        viewModel.refreshStorage()
        cleanerViewModel.loadCapabilities()
        onPauseOrDispose { }
    }

    // Full screen overlay for cleaning progress (P3-15)
    if (cleanerState.isCleaning) {
        CleaningProgressScreen(state = cleanerState.cleaningState)
        return
    }

    // Full screen for cleanup result (P3-16, P3-17)
    val lastResult = cleanerState.lastResult
    if (cleanerState.isCompleted && lastResult != null) {
        CleanupResultScreen(
            result = lastResult,
            packageRepository = packageRepository,
            onDone = {
                cleanerViewModel.onEvent(CleanerEvent.DismissResult)
                viewModel.onEvent(DashboardEvent.Refresh)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CacheSweep",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenDiagnostic) {
                        Icon(
                            imageVector = Icons.Outlined.BugReport,
                            contentDescription = "Diagnostics"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.onEvent(DashboardEvent.Refresh) },
                        enabled = !state.isScanning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isScanning,
            onRefresh = { viewModel.onEvent(DashboardEvent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scanning indicator banner
                if (state.isScanning) {
                    ScanningStatusBanner(scanState = state.scanState)
                }

                // 1. Device Storage Card (P4-05)
                DeviceStorageCard(
                    storageInfo = state.deviceStorage,
                    usedPercentage = state.usedStoragePercentage
                )

                // 2. Application Cache Summary Card (P4-06, P4-09)
                ApplicationCacheCard(
                    totalCacheBytes = state.totalReportedCacheBytes,
                    scannedAppsCount = state.scannedAppsCount,
                    measuredAppsCount = state.measuredAppsCount,
                    hasPartialFailures = state.hasPartialFailures,
                    scanDurationMillis = state.scanDurationMillis,
                    lastScanTimeMillis = state.lastScanTimeMillis
                )

                // 3. Shizuku Status Card (P4-08)
                ShizukuStatusCard(
                    shizukuState = state.shizukuState,
                    supportsSelectiveCleaning = state.supportsSelectiveCleaning,
                    onGrantPermission = { shizukuManager.requestPermission() },
                    onOpenShizuku = {
                        val launchIntent = shizukuManager.createShizukuLaunchIntent()
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    },
                    onCheckAgain = {
                        shizukuManager.updateState()
                        cleanerViewModel.loadCapabilities()
                        viewModel.refreshStorage()
                    }
                )

                // 4. Primary Hero Cleanup Action (P4-10)
                PrimaryCleanupHero(
                    totalCacheBytes = state.totalReportedCacheBytes,
                    isShizukuReady = state.isShizukuReady,
                    shizukuState = state.shizukuState,
                    isScanning = state.isScanning,
                    onCleanClick = {
                        if (state.isShizukuReady) {
                            val plan = if (state.supportsSelectiveCleaning) {
                                val targetApps = state.allScannedApps.filter { it.cacheBytes > 0 }
                                if (targetApps.isNotEmpty()) {
                                    CleanupPlan.fromApps(targetApps)
                                } else {
                                    CleanupPlan.globalTrim(
                                        deviceStorage = state.deviceStorage ?: DeviceStorageInfo(0L, 0L),
                                        estimatedCacheBytes = state.totalReportedCacheBytes
                                    )
                                }
                            } else {
                                CleanupPlan.globalTrim(
                                    deviceStorage = state.deviceStorage ?: DeviceStorageInfo(0L, 0L),
                                    estimatedCacheBytes = state.totalReportedCacheBytes
                                )
                            }
                            cleanerViewModel.onEvent(CleanerEvent.RequestClean(plan))
                        } else if (state.shizukuState is ShizukuState.PermissionRequired) {
                            shizukuManager.requestPermission()
                        } else {
                            val launchIntent = shizukuManager.createShizukuLaunchIntent()
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            }
                        }
                    }
                )

                // 5. Largest Cache Consumers Preview (P4-07)
                LargestCachesCard(
                    largestApps = state.largestApps,
                    scannedAppsCount = state.scannedAppsCount,
                    packageRepository = packageRepository,
                    onAppClick = { viewModel.onEvent(DashboardEvent.AppClicked(it)) },
                    onViewAllClick = onOpenAppList
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // App Detail Bottom Sheet (P1-23, P1-24)
    state.selectedAppDetail?.let { selectedApp ->
        AppDetailBottomSheet(
            app = selectedApp,
            packageRepository = packageRepository,
            onDismiss = { viewModel.onEvent(DashboardEvent.DismissDetail) },
            supportsSelectiveCleaning = state.supportsSelectiveCleaning,
            onClearCacheClick = {
                val plan = CleanupPlan.selectiveSingle(
                    packageName = selectedApp.packageName,
                    estimatedCacheBytes = selectedApp.cacheBytes
                )
                viewModel.onEvent(DashboardEvent.DismissDetail)
                cleanerViewModel.onEvent(CleanerEvent.RequestClean(plan))
            }
        )
    }

    // Cleanup Confirmation Dialog (P3-14)
    val pendingPlan = cleanerState.pendingPlan
    if (cleanerState.showConfirmation && pendingPlan != null) {
        CleanupConfirmationDialog(
            plan = pendingPlan,
            onConfirm = { cleanerViewModel.onEvent(CleanerEvent.ConfirmClean) },
            onDismiss = { cleanerViewModel.onEvent(CleanerEvent.DismissConfirmation) }
        )
    }

    // Error Dialog for failed cleaner state
    val cleanerErrorMsg = cleanerState.errorMessage
    if (cleanerState.isFailed && cleanerErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { cleanerViewModel.onEvent(CleanerEvent.DismissResult) },
            title = { Text("Cleanup Failed") },
            text = { Text(cleanerErrorMsg) },
            confirmButton = {
                Button(onClick = { cleanerViewModel.onEvent(CleanerEvent.DismissResult) }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { cleanerViewModel.onEvent(CleanerEvent.Retry) }) {
                    Text("Retry")
                }
            }
        )
    }
}

@Composable
private fun ScanningStatusBanner(scanState: ScanState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (scanState) {
                is ScanState.Scanning -> {
                    LinearProgressIndicator(
                        progress = { scanState.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Scanning ${scanState.scannedCount}/${scanState.totalCount} apps...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        scanState.currentAppName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                is ScanState.Discovering -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "Discovering installed applications...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "Updating cache measurements...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Device storage visualization (P4-05)
 */
@Composable
fun DeviceStorageCard(
    storageInfo: DeviceStorageInfo?,
    usedPercentage: Float,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "DEVICE STORAGE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                )
            }

            if (storageInfo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${ByteFormatter.format(storageInfo.usedBytes)} used",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${ByteFormatter.format(storageInfo.availableBytes)} available of ${ByteFormatter.format(storageInfo.totalBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.0f%%", usedPercentage * 100),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { usedPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = if (usedPercentage > 0.90f) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                Text(
                    text = "Reading storage information...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

/**
 * Aggregate Application Cache Summary (P4-06, P4-09)
 */
@Composable
fun ApplicationCacheCard(
    totalCacheBytes: Long,
    scannedAppsCount: Int,
    measuredAppsCount: Int,
    hasPartialFailures: Boolean,
    scanDurationMillis: Long,
    lastScanTimeMillis: Long,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CleaningServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "APPLICATION CACHE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Estimated cache",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = ByteFormatter.format(totalCacheBytes),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val appCountText = if (hasPartialFailures) {
                    "$measuredAppsCount of $scannedAppsCount apps measured"
                } else if (scannedAppsCount > 0) {
                    "$scannedAppsCount apps scanned"
                } else {
                    "Scanning apps..."
                }

                Text(
                    text = appCountText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                if (scanDurationMillis > 0) {
                    Text(
                        text = "Scanned in ${DashboardTimeFormatter.formatDuration(scanDurationMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (lastScanTimeMillis > 0) {
                Text(
                    text = "Last scanned: ${DashboardTimeFormatter.formatLastScanned(lastScanTimeMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Text(
                text = "Cache is disposable data created by applications that can usually be recreated when needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Shizuku Connection Status Card (P4-08)
 */
@Composable
fun ShizukuStatusCard(
    shizukuState: ShizukuState,
    supportsSelectiveCleaning: Boolean,
    onGrantPermission: () -> Unit,
    onOpenShizuku: () -> Unit,
    onCheckAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (shizukuState) {
        is ShizukuState.Ready -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Shizuku Connected (UID ${shizukuState.uid})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (supportsSelectiveCleaning) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Selective Clear") }
                            )
                        }
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Global Trimming") }
                        )
                    }
                }
            }
        }

        is ShizukuState.PermissionRequired -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Shizuku Permission Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Permission is required to execute system-level cache clearing operations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onGrantPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }

        is ShizukuState.Connecting -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(
                        text = "Connecting to Shizuku service...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        is ShizukuState.NotRunning -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Shizuku Isn't Running",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Start Shizuku to enable system cache cleanup without root. You can still inspect storage and open individual app settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenShizuku,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Shizuku")
                        }

                        FilledTonalButton(
                            onClick = onCheckAgain,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Check Again")
                        }
                    }
                }
            }
        }

        is ShizukuState.Error -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                ),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Privileged Service Disconnected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "The Shizuku binder connection encountered an error: ${shizukuState.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        onClick = onCheckAgain,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reconnect")
                    }
                }
            }
        }
    }
}

/**
 * Primary Cleanup Hero Button (P4-10)
 */
@Composable
fun PrimaryCleanupHero(
    totalCacheBytes: Long,
    isShizukuReady: Boolean,
    shizukuState: ShizukuState,
    isScanning: Boolean,
    onCleanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = !isScanning && (totalCacheBytes > 0L || !isShizukuReady)

    Button(
        onClick = onCleanClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CleaningServices,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "CLEAN CACHE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isShizukuReady) {
                            "Ask Android to reclaim disposable cache"
                        } else {
                            "Setup Shizuku to clean cache"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }

            if (totalCacheBytes > 0L) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = ByteFormatter.format(totalCacheBytes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Largest Cache Consumers Preview List (P4-07)
 */
@Composable
fun LargestCachesCard(
    largestApps: List<AppCacheInfo>,
    scannedAppsCount: Int,
    packageRepository: PackageRepository?,
    onAppClick: (AppCacheInfo) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Largest caches",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (scannedAppsCount > 0) {
                TextButton(onClick = onViewAllClick) {
                    Text("View all ($scannedAppsCount) →")
                }
            }
        }

        if (largestApps.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (scannedAppsCount > 0) "No applications reporting cache." else "Scanning applications...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    largestApps.forEachIndexed { index, app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClick(app) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(
                                packageName = app.packageName,
                                packageRepository = packageRepository,
                                size = 40.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = ByteFormatter.format(app.cacheBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "View details",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (index < largestApps.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewAllClick() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "View all $scannedAppsCount applications",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
