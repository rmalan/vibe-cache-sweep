package my.id.rmalan.cache.sweep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.scanner.CacheScanner
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.storage.DeviceStorageRepository

import my.id.rmalan.cache.sweep.permissions.UsageAccessManager

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val deviceStorage: DeviceStorageInfo? = null,
    val totalReportedCacheBytes: Long = 0L,
    val scannedAppsCount: Int = 0,
    val measuredAppsCount: Int = 0,
    val largestApps: List<AppCacheInfo> = emptyList(),
    val allScannedApps: List<AppCacheInfo> = emptyList(),
    val shizukuState: ShizukuState = ShizukuState.NotRunning,
    val isShizukuInstalled: Boolean = false,
    val hasUsageAccess: Boolean = true,
    val cleanerCapabilities: CleanerCapabilities? = null,
    val scanDurationMillis: Long = 0L,
    val lastScanTimeMillis: Long = 0L,
    val scanState: ScanState = ScanState.Idle,
    val scanResult: ScanResult? = null,
    val selectedAppDetail: AppCacheInfo? = null,
    val errorMessage: String? = null
) {
    val usedStoragePercentage: Float
        get() = deviceStorage?.let {
            if (it.totalBytes > 0) (it.usedBytes.toFloat() / it.totalBytes).coerceIn(0f, 1f) else 0f
        } ?: 0f

    val hasPartialFailures: Boolean
        get() = scannedAppsCount > 0 && measuredAppsCount < scannedAppsCount

    val supportsSelectiveCleaning: Boolean
        get() = cleanerCapabilities?.supportsSelectiveCacheClear == true

    val isShizukuReady: Boolean
        get() = shizukuState is ShizukuState.Ready
}

sealed interface DashboardEvent {
    data object Refresh : DashboardEvent
    data class AppClicked(val app: AppCacheInfo) : DashboardEvent
    data object DismissDetail : DashboardEvent
    data object ClearError : DashboardEvent
}

class DashboardViewModel(
    private val deviceStorageRepository: DeviceStorageRepository,
    private val cacheScanner: CacheScanner,
    private val shizukuManager: ShizukuManager? = null,
    private val usageAccessManager: UsageAccessManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
        refreshStorage()
        observeShizukuState()
        loadCapabilities()
        scan()
    }

    fun refreshStatus() {
        val hasAccess = usageAccessManager?.hasAccess() ?: true
        val isInstalled = shizukuManager?.isShizukuInstalled() ?: false
        _uiState.update {
            it.copy(
                hasUsageAccess = hasAccess,
                isShizukuInstalled = isInstalled
            )
        }
    }

    fun refreshStorage() {
        refreshStatus()
        try {
            val storage = deviceStorageRepository.snapshot()
            _uiState.update { it.copy(deviceStorage = storage) }
        } catch (e: Exception) {
            // Non-fatal: StatFs read failure
        }
    }

    private fun observeShizukuState() {
        if (shizukuManager != null) {
            viewModelScope.launch {
                shizukuManager.state.collect { state ->
                    val isInstalled = shizukuManager.isShizukuInstalled()
                    _uiState.update { it.copy(shizukuState = state, isShizukuInstalled = isInstalled) }
                    if (state is ShizukuState.Ready) {
                        loadCapabilities()
                    }
                }
            }
        }
    }

    fun loadCapabilities() {
        viewModelScope.launch {
            try {
                val caps = shizukuManager?.fetchCapabilities()
                _uiState.update { it.copy(cleanerCapabilities = caps) }
            } catch (e: Exception) {
                // Non-fatal capability query error
            }
        }
    }

    fun scan() {
        viewModelScope.launch {
            refreshStorage()
            val hasAccess = usageAccessManager?.hasAccess() ?: true
            if (!hasAccess) {
                _uiState.update {
                    it.copy(
                        hasUsageAccess = false,
                        isScanning = false,
                        isLoading = false,
                        errorMessage = "Usage Access permission is required to calculate cache usage."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isScanning = true, errorMessage = null) }
            try {
                cacheScanner.scanFlow().collect { scanState ->
                    _uiState.update { state ->
                        val updatedState = state.copy(scanState = scanState)
                        if (scanState is ScanState.Complete) {
                            val result = scanState.result
                            val sortedWithCache = result.apps
                                .filter { it.cacheBytes > 0 }
                                .sortedByDescending { it.cacheBytes }
                                .take(5)

                            updatedState.copy(
                                isLoading = false,
                                isScanning = false,
                                scanResult = result,
                                allScannedApps = result.apps,
                                totalReportedCacheBytes = result.totalReportedCacheBytes,
                                scannedAppsCount = result.attemptedApps,
                                measuredAppsCount = result.successfulApps,
                                largestApps = sortedWithCache,
                                scanDurationMillis = result.durationMillis,
                                lastScanTimeMillis = System.currentTimeMillis()
                            )
                        } else if (scanState is ScanState.Failed) {
                            updatedState.copy(
                                isLoading = false,
                                isScanning = false,
                                errorMessage = scanState.message
                            )
                        } else {
                            updatedState
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isScanning = false,
                        errorMessage = e.localizedMessage ?: "Scanning failed"
                    )
                }
            }
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Refresh -> {
                refreshStorage()
                loadCapabilities()
                scan()
            }
            is DashboardEvent.AppClicked -> {
                _uiState.update { it.copy(selectedAppDetail = event.app) }
            }
            is DashboardEvent.DismissDetail -> {
                _uiState.update { it.copy(selectedAppDetail = null) }
            }
            is DashboardEvent.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    class Factory(
        private val deviceStorageRepository: DeviceStorageRepository,
        private val cacheScanner: CacheScanner,
        private val shizukuManager: ShizukuManager? = null,
        private val usageAccessManager: UsageAccessManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                deviceStorageRepository = deviceStorageRepository,
                cacheScanner = cacheScanner,
                shizukuManager = shizukuManager,
                usageAccessManager = usageAccessManager
            ) as T
        }
    }
}
