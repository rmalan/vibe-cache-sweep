package my.id.rmalan.cache.sweep.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.scanner.PackageRepository
import my.id.rmalan.cache.sweep.ui.components.AppCacheRow
import my.id.rmalan.cache.sweep.ui.components.AppDetailBottomSheet
import my.id.rmalan.cache.sweep.ui.components.CleanupConfirmationDialog
import my.id.rmalan.cache.sweep.ui.components.NeoBadge
import my.id.rmalan.cache.sweep.ui.components.NeoProgressBar
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsViewModel
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerViewModel
import my.id.rmalan.cache.sweep.util.ByteFormatter

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCacheListScreen(
    viewModel: AppsViewModel,
    cleanerViewModel: CleanerViewModel? = null,
    packageRepository: PackageRepository? = null,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val cleanerState = cleanerViewModel?.uiState?.collectAsState()?.value
    val context = LocalContext.current

    var isSelectionMode by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        cleanerViewModel?.loadCapabilities()
        viewModel.loadCapabilities()
        if (state.hasUsageAccess && state.rawApps.isEmpty() && !state.isScanning) {
            viewModel.scan()
        }
        onPauseOrDispose { }
    }

    // Trigger initial scan if apps list is empty
    LaunchedEffect(Unit) {
        if (state.rawApps.isEmpty() && !state.isScanning) {
            viewModel.scan()
        }
        cleanerViewModel?.loadCapabilities()
    }

    // Full screen overlay for cleaning progress (P3-15)
    if (cleanerState != null && cleanerState.isCleaning) {
        CleaningProgressScreen(state = cleanerState.cleaningState)
        return
    }

    // Full screen for cleanup result (P3-16, P3-17)
    if (cleanerState != null && cleanerState.isCompleted && cleanerState.lastResult != null) {
        CleanupResultScreen(
            result = cleanerState.lastResult,
            packageRepository = packageRepository,
            onDone = {
                cleanerViewModel.onEvent(CleanerEvent.DismissResult)
                viewModel.onEvent(AppsEvent.ClearSelection)
                isSelectionMode = false
                viewModel.scan()
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isSelectionMode) "${state.selectedPackages.size} Selected" else "Application Cache",
                            modifier = Modifier.semantics { heading() }
                        )
                        if (state.displayedApps.isNotEmpty()) {
                            val subtitle = if (isSelectionMode) {
                                "${ByteFormatter.format(state.selectedCacheBytes)} estimated cache"
                            } else {
                                "${state.displayedApps.size} apps • ${ByteFormatter.format(state.totalReportedCacheBytes)} reported"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            isSelectionMode = false
                            viewModel.onEvent(AppsEvent.ClearSelection)
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Selection Mode")
                        }
                    } else if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        TextButton(
                            onClick = {
                                if (state.selectedPackages.size == state.displayedApps.size) {
                                    viewModel.onEvent(AppsEvent.ClearSelection)
                                } else {
                                    viewModel.onEvent(AppsEvent.SelectAll)
                                }
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(
                                text = if (state.selectedPackages.size == state.displayedApps.size) "Deselect All" else "Select All"
                            )
                        }
                    } else {
                        if (state.supportsSelectiveCleaning) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = true
                                }
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Select Apps")
                            }
                        }
                        if (onOpenSettings != null) {
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                            }
                        }
                        IconButton(
                            onClick = { viewModel.onEvent(AppsEvent.Refresh) },
                            enabled = !state.isScanning
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Scan")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (cleanerViewModel != null && state.displayedApps.isNotEmpty() && !state.isScanning) {
                val hasSelected = state.selectedPackages.isNotEmpty()
                val targetApps = if (hasSelected) {
                    state.rawApps.filter { it.packageName in state.selectedPackages }
                } else {
                    state.displayedApps.filter { it.cacheBytes > 0 }
                }

                if (targetApps.isNotEmpty() || state.totalReportedCacheBytes > 0) {
                    val totalCache = if (hasSelected) {
                        targetApps.sumOf { it.cacheBytes }
                    } else {
                        state.totalReportedCacheBytes
                    }

                    val label = if (hasSelected && state.supportsSelectiveCleaning) {
                        "Clean Selected (${targetApps.size})"
                    } else {
                        "Clean All Cache"
                    }

                    ExtendedFloatingActionButton(
                        onClick = {
                            val plan = if (state.supportsSelectiveCleaning) {
                                if (targetApps.isNotEmpty()) {
                                    CleanupPlan.fromApps(targetApps)
                                } else {
                                    CleanupPlan.globalTrim(
                                        estimatedCacheBytes = totalCache
                                    )
                                }
                            } else {
                                CleanupPlan.globalTrim(
                                    estimatedCacheBytes = totalCache
                                )
                            }
                            cleanerViewModel.onEvent(CleanerEvent.RequestClean(plan))
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(12.dp),
                        icon = {
                            Icon(Icons.Outlined.CleaningServices, contentDescription = null)
                        },
                        text = {
                            Text(
                                text = "$label • ${ByteFormatter.format(totalCache)}",
                                fontWeight = FontWeight.Black
                            )
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isScanning,
            onRefresh = { viewModel.onEvent(AppsEvent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Usage Access Required Banner (P5-06)
                if (!state.hasUsageAccess) {
                    UsageAccessRequiredBanner(
                        onGrantAccess = {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Could not open Usage Access settings",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Search Input Field (P1-21)
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.onEvent(AppsEvent.SearchChanged(it)) },
                    placeholder = { Text("Search by name or package...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onEvent(AppsEvent.SearchChanged("")) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Sort & Filter Chips Row (P1-18, P1-19, P1-20)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppSort.entries.forEach { sortOption ->
                        FilterChip(
                            selected = state.sort == sortOption,
                            onClick = { viewModel.onEvent(AppsEvent.SortChanged(sortOption)) },
                            label = { Text(sortOption.label, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = state.sort == sortOption,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MaterialTheme.colorScheme.outline,
                                borderWidth = 1.5.dp,
                                selectedBorderWidth = 2.dp
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    FilterChip(
                        selected = !state.showZeroCacheApps,
                        onClick = { viewModel.onEvent(AppsEvent.ToggleShowZeroCache(!state.showZeroCacheApps)) },
                        label = { Text("Hide 0 B", fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(8.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = !state.showZeroCacheApps,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.outline,
                            borderWidth = 1.5.dp,
                            selectedBorderWidth = 2.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )

                    FilterChip(
                        selected = state.showSystemApps,
                        onClick = { viewModel.onEvent(AppsEvent.ToggleShowSystem(!state.showSystemApps)) },
                        label = { Text("System Apps", fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(8.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.showSystemApps,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.outline,
                            borderWidth = 1.5.dp,
                            selectedBorderWidth = 2.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }

                // Scanning Progress Bar (P1-12)
                val scanState = state.scanState
                if (state.isScanning && scanState is ScanState.Scanning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        NeoProgressBar(
                            progress = scanState.progressFraction,
                            height = 10.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Scanning ${scanState.scannedCount}/${scanState.totalCount} apps",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            scanState.currentAppName?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                // Application List (P1-17)
                if (state.displayedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isScanning) {
                            Text(
                                text = "Scanning applications...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No applications found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.semantics { heading() }
                                )
                                Text(
                                    text = if (state.query.isNotBlank()) {
                                        "No apps match '${state.query}'"
                                    } else {
                                        "Try adjusting your filters or refreshing the scan."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = state.displayedApps,
                            key = { it.packageName }
                        ) { app ->
                            AppCacheRow(
                                app = app,
                                packageRepository = packageRepository,
                                onClick = {
                                    if (isSelectionMode) {
                                        viewModel.onEvent(AppsEvent.ToggleSelected(app.packageName))
                                    } else {
                                        viewModel.onEvent(AppsEvent.AppClicked(app))
                                    }
                                },
                                isSelected = app.packageName in state.selectedPackages,
                                showCheckbox = isSelectionMode,
                                onToggleSelect = {
                                    viewModel.onEvent(AppsEvent.ToggleSelected(app.packageName))
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(88.dp))
                        }
                    }
                }
            }
        }
    }

    // App Detail Bottom Sheet (P1-23, P1-24)
    state.selectedAppDetail?.let { selectedApp ->
        AppDetailBottomSheet(
            app = selectedApp,
            packageRepository = packageRepository,
            onDismiss = { viewModel.onEvent(AppsEvent.DismissDetail) },
            supportsSelectiveCleaning = state.supportsSelectiveCleaning,
            onClearCacheClick = if (cleanerViewModel != null) {
                {
                    val plan = CleanupPlan.selectiveSingle(
                        packageName = selectedApp.packageName,
                        estimatedCacheBytes = selectedApp.cacheBytes
                    )
                    viewModel.onEvent(AppsEvent.DismissDetail)
                    cleanerViewModel.onEvent(CleanerEvent.RequestClean(plan))
                }
            } else null
        )
    }

    // Cleanup Confirmation Dialog (P3-14)
    if (cleanerState != null && cleanerState.showConfirmation && cleanerState.pendingPlan != null) {
        CleanupConfirmationDialog(
            plan = cleanerState.pendingPlan,
            onConfirm = { cleanerViewModel.onEvent(CleanerEvent.ConfirmClean) },
            onDismiss = { cleanerViewModel.onEvent(CleanerEvent.DismissConfirmation) }
        )
    }

    // Error Dialog for failed cleaner state
    if (cleanerState != null && cleanerState.isFailed && cleanerState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { cleanerViewModel.onEvent(CleanerEvent.DismissResult) },
            title = { Text("Cleanup Failed", modifier = Modifier.semantics { heading() }) },
            text = { Text(cleanerState.errorMessage) },
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
