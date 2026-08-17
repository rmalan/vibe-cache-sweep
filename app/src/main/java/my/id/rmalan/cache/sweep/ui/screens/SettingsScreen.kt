package my.id.rmalan.cache.sweep.ui.screens

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.model.ThemeMode
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.ui.components.NeoBadge
import my.id.rmalan.cache.sweep.ui.components.NeoButton
import my.id.rmalan.cache.sweep.ui.components.NeoCard
import my.id.rmalan.cache.sweep.ui.viewmodel.SettingsEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.SettingsViewModel
import my.id.rmalan.cache.sweep.util.SettingsFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    shizukuManager: ShizukuManager? = null,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(SettingsEvent.RefreshShizuku)
        onPauseOrDispose { }
    }

    LaunchedEffect(state.historyClearedMessage) {
        state.historyClearedMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(SettingsEvent.DismissHistoryMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Scanning Section (P4-11, P4-12, P4-13)
            SettingsSectionHeader(
                icon = Icons.Outlined.Tune,
                title = "Scanning"
            )

            NeoCard(
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // P4-11: Show system apps
                    SettingsSwitchRow(
                        title = "Show system applications",
                        subtitle = "Include Android OS and pre-installed system packages in scans",
                        checked = state.settings.showSystemApps,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleShowSystemApps(it)) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // P4-12: Show zero-cache apps
                    SettingsSwitchRow(
                        title = "Show zero-cache applications",
                        subtitle = "Display apps even if they currently report 0 B of cache",
                        checked = state.settings.showZeroCacheApps,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleShowZeroCacheApps(it)) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // P4-13: Sort preference
                    SettingsClickableRow(
                        title = "Default sort order",
                        subtitle = SettingsFormatter.sortModeLabel(state.settings.sortMode),
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        onClick = { showSortDialog = true }
                    )
                }
            }

            // 2. Appearance Section (P4-14)
            SettingsSectionHeader(
                icon = Icons.Outlined.ColorLens,
                title = "Appearance"
            )

            NeoCard(
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsClickableRow(
                    title = "App theme",
                    subtitle = SettingsFormatter.themeModeLabel(state.settings.themeMode),
                    icon = Icons.Outlined.ColorLens,
                    onClick = { showThemeDialog = true }
                )
            }

            // 3. Shizuku Service Section (PRD Section 19)
            SettingsSectionHeader(
                icon = Icons.Outlined.Security,
                title = "Shizuku Service"
            )

            NeoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Service Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (val s = state.shizukuState) {
                                    is ShizukuState.Ready -> "Connected (UID ${s.uid})"
                                    ShizukuState.PermissionRequired -> "Permission required"
                                    ShizukuState.Connecting -> "Connecting..."
                                    ShizukuState.NotRunning -> "Service not running"
                                    is ShizukuState.Error -> "Error: ${s.reason}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = when (state.shizukuState) {
                                    is ShizukuState.Ready -> MaterialTheme.colorScheme.onSurface
                                    ShizukuState.PermissionRequired -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        Icon(
                            imageVector = when (state.shizukuState) {
                                is ShizukuState.Ready -> Icons.Outlined.CheckCircle
                                ShizukuState.PermissionRequired -> Icons.Outlined.Warning
                                else -> Icons.Outlined.Info
                            },
                            contentDescription = null,
                            tint = when (state.shizukuState) {
                                is ShizukuState.Ready -> MaterialTheme.colorScheme.onSurface
                                ShizukuState.PermissionRequired -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.shizukuState == ShizukuState.PermissionRequired) {
                            NeoButton(
                                onClick = { viewModel.onEvent(SettingsEvent.RequestShizukuPermission) },
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Grant Permission")
                            }
                        } else if (state.shizukuState == ShizukuState.NotRunning) {
                            NeoButton(
                                onClick = {
                                    val launchIntent = shizukuManager?.createShizukuLaunchIntent()
                                    if (launchIntent != null) {
                                        try {
                                            context.startActivity(launchIntent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Failed to open Shizuku: ${e.localizedMessage ?: "Unknown error"}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Shizuku app not found. Please install Shizuku to continue.",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Shizuku")
                            }
                        }

                        NeoButton(
                            onClick = { viewModel.onEvent(SettingsEvent.RefreshShizuku) },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = (if (state.shizukuState is ShizukuState.Ready) Modifier.fillMaxWidth() else Modifier.weight(1f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Connection")
                        }
                    }
                }
            }

            // 4. Data & Local History Section (P4-15)
            SettingsSectionHeader(
                icon = Icons.Outlined.History,
                title = "Data & History"
            )

            NeoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cleanup History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = SettingsFormatter.historyCountLabel(state.historyCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        NeoBadge(
                            text = "${state.historyCount} RECORDS",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    NeoButton(
                        onClick = { showClearHistoryDialog = true },
                        enabled = state.historyCount > 0 && !state.isClearingHistory,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Cleanup History")
                    }
                }
            }

            // 5. Privacy & Security Section
            SettingsSectionHeader(
                icon = Icons.Outlined.Lock,
                title = "Privacy & Guarantees"
            )

            NeoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrivacyBulletItem(
                        title = "100% Offline & Private",
                        description = "No network permissions requested. Your device data never leaves your phone."
                    )
                    PrivacyBulletItem(
                        title = "Zero Telemetry or Ads",
                        description = "No tracking, advertising, analytics, or cloud dependencies."
                    )
                    PrivacyBulletItem(
                        title = "Strict Safety Constraints",
                        description = "CacheSweep never executes plain 'pm clear'. Only cache data is targeted."
                    )
                }
            }

            // 6. About Section
            SettingsSectionHeader(
                icon = Icons.Outlined.Info,
                title = "About"
            )

            NeoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CacheSweep",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        NeoBadge(
                            text = "v${state.appVersion}",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Personal Android cache-cleaning utility designed for precision, transparency, and safety.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Sort Preference Picker Dialog (P4-13)
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Default Sort Order",
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { heading() }
                )
            },
            text = {
                Column {
                    AppSort.entries.forEach { sort ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable(
                                    role = Role.RadioButton,
                                    onClick = {
                                        viewModel.onEvent(SettingsEvent.SetSortMode(sort))
                                        showSortDialog = false
                                    }
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (state.settings.sortMode == sort),
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.SetSortMode(sort))
                                    showSortDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = SettingsFormatter.sortModeLabel(sort),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = SettingsFormatter.sortModeDescription(sort),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSortDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            modifier = Modifier.border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
        )
    }

    // Theme Mode Picker Dialog (P4-14)
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "App Theme",
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { heading() }
                )
            },
            text = {
                Column {
                    ThemeMode.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable(
                                    role = Role.RadioButton,
                                    onClick = {
                                        viewModel.onEvent(SettingsEvent.SetThemeMode(theme))
                                        showThemeDialog = false
                                    }
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (state.settings.themeMode == theme),
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.SetThemeMode(theme))
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = SettingsFormatter.themeModeLabel(theme),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = SettingsFormatter.themeModeDescription(theme),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showThemeDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            modifier = Modifier.border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
        )
    }

    // Clear History Confirmation Dialog (P4-15)
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Clear Cleanup History?",
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { heading() }
                )
            },
            text = {
                Text(
                    "This will delete all ${state.historyCount} locally saved cleanup history records. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(SettingsEvent.ClearHistory)
                        showClearHistoryDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Text("Clear History", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearHistoryDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            },
            modifier = Modifier.border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun SettingsClickableRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PrivacyBulletItem(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
