package my.id.rmalan.cache.sweep.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.launch
import my.id.rmalan.cache.sweep.di.AppContainer
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.util.ByteFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shizukuState by container.shizukuManager.state.collectAsState()

    var hasUsageAccess by remember { mutableStateOf(container.usageAccessManager.hasAccess()) }
    var storageInfo by remember { mutableStateOf<DeviceStorageInfo?>(null) }
    var capabilities by remember { mutableStateOf<CleanerCapabilities?>(null) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun refreshAll() {
        hasUsageAccess = container.usageAccessManager.hasAccess()
        storageInfo = container.deviceStorageRepository.snapshot()
        container.shizukuManager.updateState()
        capabilities = container.shizukuManager.getCapabilities()
    }

    LaunchedEffect(Unit) {
        refreshAll()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CacheSweep Diagnostic Spike") }
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
            // System Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("System & Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    DiagnosticRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    DiagnosticRow("Device Model", "${Build.MANUFACTURER} ${Build.MODEL}")
                    storageInfo?.let {
                        DiagnosticRow("Free Storage", ByteFormatter.format(it.availableBytes))
                        DiagnosticRow("Total Storage", ByteFormatter.format(it.totalBytes))
                    }
                }
            }

            // Usage Access Card
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

            // Shizuku Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shizuku & Privileges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    val stateText = when (val s = shizukuState) {
                        is ShizukuState.Ready -> "Ready (UID ${s.uid})"
                        is ShizukuState.PermissionRequired -> "Permission Required"
                        is ShizukuState.Connecting -> "Connecting..."
                        is ShizukuState.NotRunning -> "Not Running"
                        is ShizukuState.Error -> "Error: ${s.reason}"
                    }
                    DiagnosticRow("Shizuku State", stateText)

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
                }
            }

            // Scanner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("StorageStats Scanner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()

                    if (isScanning) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Scanning installed applications...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        scanResult?.let { res ->
                            DiagnosticRow("Attempted Apps", "${res.attemptedApps}")
                            DiagnosticRow("Measured Apps", "${res.successfulApps}")
                            DiagnosticRow("Reported Cache", ByteFormatter.format(res.totalReportedCacheBytes))
                            DiagnosticRow("Scan Duration", "${res.durationMillis} ms")
                        } ?: Text("No scan performed yet.")

                        Button(
                            onClick = {
                                scope.launch {
                                    isScanning = true
                                    try {
                                        scanResult = container.cacheScanner.scan()
                                        refreshAll()
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
                            Text("Run Cache Scan")
                        }
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
