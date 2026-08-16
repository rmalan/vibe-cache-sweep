package my.id.rmalan.cache.sweep.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.scanner.PackageRepository
import my.id.rmalan.cache.sweep.ui.components.AppCacheRow
import my.id.rmalan.cache.sweep.ui.components.AppDetailBottomSheet
import my.id.rmalan.cache.sweep.ui.components.CleanupConfirmationDialog
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsViewModel
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerViewModel
import my.id.rmalan.cache.sweep.util.ByteFormatter

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

    var isSelectionMode by remember { mutableStateOf(false) }

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
                        Text(if (isSelectionMode) "${state.selectedPackages.size} Selected" else "Application Cache")
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
                        TextButton(onClick = {
                            if (state.selectedPackages.size == state.displayedApps.size) {
                                viewModel.onEvent(AppsEvent.ClearSelection)
                            } else {
                                viewModel.onEvent(AppsEvent.SelectAll)
                            }
                        }) {
                            Text(
                                text = if (state.selectedPackages.size == state.displayedApps.size) "Deselect All" else "Select All"
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                isSelectionMode = true
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Select Apps")
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

                if (targetApps.isNotEmpty()) {
                    val totalCache = targetApps.sumOf { it.cacheBytes }
                    val label = if (hasSelected) {
                        "Clean Selected (${targetApps.size})"
                    } else {
                        "Clean All Cache"
                    }

                    ExtendedFloatingActionButton(
                        onClick = {
                            val plan = if (!hasSelected && !state.supportsSelectiveCleaning) {
                                CleanupPlan.globalTrim(
                                    desiredFreeBytes = 0L,
                                    estimatedCacheBytes = totalCache
                                )
                            } else {
                                CleanupPlan.fromApps(targetApps)
                            }
                            cleanerViewModel.onEvent(CleanerEvent.RequestClean(plan))
                        },
                        icon = {
                            Icon(Icons.Outlined.CleaningServices, contentDescription = null)
                        },
                        text = {
                            Text("$label • ${ByteFormatter.format(totalCache)}")
                        }
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
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                            label = { Text(sortOption.label) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }

                    FilterChip(
                        selected = !state.showZeroCacheApps,
                        onClick = { viewModel.onEvent(AppsEvent.ToggleShowZeroCache(!state.showZeroCacheApps)) },
                        label = { Text("Hide 0 B") }
                    )

                    FilterChip(
                        selected = state.showSystemApps,
                        onClick = { viewModel.onEvent(AppsEvent.ToggleShowSystem(!state.showSystemApps)) },
                        label = { Text("System Apps") }
                    )
                }

                // Scanning Progress Bar (P1-12)
                val scanState = state.scanState
                if (state.isScanning && scanState is ScanState.Scanning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { scanState.progressFraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Scanning ${scanState.scannedCount}/${scanState.totalCount} apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            scanState.currentAppName?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

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
                                    fontWeight = FontWeight.Bold
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
            title = { Text("Cleanup Failed") },
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
