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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.scanner.PackageRepository
import my.id.rmalan.cache.sweep.ui.components.AppCacheRow
import my.id.rmalan.cache.sweep.ui.components.AppDetailBottomSheet
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsViewModel
import my.id.rmalan.cache.sweep.util.ByteFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCacheListScreen(
    viewModel: AppsViewModel,
    packageRepository: PackageRepository?,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    // Trigger initial scan if apps list is empty
    LaunchedEffect(Unit) {
        if (state.rawApps.isEmpty() && !state.isScanning) {
            viewModel.scan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Application Cache")
                        if (state.displayedApps.isNotEmpty()) {
                            Text(
                                text = "${state.displayedApps.size} apps • ${ByteFormatter.format(state.totalReportedCacheBytes)} reported",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(AppsEvent.Refresh) },
                        enabled = !state.isScanning
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Scan")
                    }
                }
            )
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
                                onClick = { viewModel.onEvent(AppsEvent.AppClicked(app)) },
                                isSelected = app.packageName in state.selectedPackages,
                                showCheckbox = false
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
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
            supportsSelectiveCleaning = state.supportsSelectiveCleaning
        )
    }
}
