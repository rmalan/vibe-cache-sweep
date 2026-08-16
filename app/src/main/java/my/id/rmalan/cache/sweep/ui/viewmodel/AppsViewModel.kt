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
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.scanner.CacheScanner
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.util.AppFilter

data class AppsUiState(
    val rawApps: List<AppCacheInfo> = emptyList(),
    val displayedApps: List<AppCacheInfo> = emptyList(),
    val query: String = "",
    val sort: AppSort = AppSort.CACHE_DESC,
    val showSystemApps: Boolean = true,
    val showZeroCacheApps: Boolean = true,
    val selectedPackages: Set<String> = emptySet(),
    val selectedAppDetail: AppCacheInfo? = null,
    val isScanning: Boolean = false,
    val scanState: ScanState = ScanState.Idle,
    val scanResult: ScanResult? = null,
    val supportsSelectiveCleaning: Boolean = false,
    val errorMessage: String? = null
) {
    val totalReportedCacheBytes: Long
        get() = displayedApps.sumOf { it.cacheBytes }

    val selectedCacheBytes: Long
        get() = rawApps.filter { it.packageName in selectedPackages }.sumOf { it.cacheBytes }
}

sealed interface AppsEvent {
    data class SearchChanged(val query: String) : AppsEvent
    data class SortChanged(val sort: AppSort) : AppsEvent
    data class ToggleShowSystem(val show: Boolean) : AppsEvent
    data class ToggleShowZeroCache(val show: Boolean) : AppsEvent
    data class ToggleSelected(val packageName: String) : AppsEvent
    data object SelectAll : AppsEvent
    data object ClearSelection : AppsEvent
    data class AppClicked(val app: AppCacheInfo) : AppsEvent
    data object DismissDetail : AppsEvent
    data object Refresh : AppsEvent
    data object ClearError : AppsEvent
}

class AppsViewModel(
    private val cacheScanner: CacheScanner,
    private val shizukuManager: ShizukuManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        loadCapabilities()
    }

    private fun loadCapabilities() {
        viewModelScope.launch {
            val caps = shizukuManager?.fetchCapabilities()
            _uiState.update {
                it.copy(supportsSelectiveCleaning = caps?.supportsSelectiveCacheClear == true)
            }
        }
    }

    fun onEvent(event: AppsEvent) {
        when (event) {
            is AppsEvent.SearchChanged -> {
                _uiState.update { state ->
                    val newDisplayed = AppFilter.filterAndSort(
                        apps = state.rawApps,
                        query = event.query,
                        sort = state.sort,
                        showSystemApps = state.showSystemApps,
                        showZeroCacheApps = state.showZeroCacheApps
                    )
                    state.copy(query = event.query, displayedApps = newDisplayed)
                }
            }
            is AppsEvent.SortChanged -> {
                _uiState.update { state ->
                    val newDisplayed = AppFilter.filterAndSort(
                        apps = state.rawApps,
                        query = state.query,
                        sort = event.sort,
                        showSystemApps = state.showSystemApps,
                        showZeroCacheApps = state.showZeroCacheApps
                    )
                    state.copy(sort = event.sort, displayedApps = newDisplayed)
                }
            }
            is AppsEvent.ToggleShowSystem -> {
                _uiState.update { state ->
                    val newDisplayed = AppFilter.filterAndSort(
                        apps = state.rawApps,
                        query = state.query,
                        sort = state.sort,
                        showSystemApps = event.show,
                        showZeroCacheApps = state.showZeroCacheApps
                    )
                    state.copy(showSystemApps = event.show, displayedApps = newDisplayed)
                }
            }
            is AppsEvent.ToggleShowZeroCache -> {
                _uiState.update { state ->
                    val newDisplayed = AppFilter.filterAndSort(
                        apps = state.rawApps,
                        query = state.query,
                        sort = state.sort,
                        showSystemApps = state.showSystemApps,
                        showZeroCacheApps = event.show
                    )
                    state.copy(showZeroCacheApps = event.show, displayedApps = newDisplayed)
                }
            }
            is AppsEvent.ToggleSelected -> {
                _uiState.update { state ->
                    val next = if (event.packageName in state.selectedPackages) {
                        state.selectedPackages - event.packageName
                    } else {
                        state.selectedPackages + event.packageName
                    }
                    state.copy(selectedPackages = next)
                }
            }
            is AppsEvent.SelectAll -> {
                _uiState.update { state ->
                    state.copy(selectedPackages = state.displayedApps.map { it.packageName }.toSet())
                }
            }
            is AppsEvent.ClearSelection -> {
                _uiState.update { state ->
                    state.copy(selectedPackages = emptySet())
                }
            }
            is AppsEvent.AppClicked -> {
                _uiState.update { it.copy(selectedAppDetail = event.app) }
            }
            is AppsEvent.DismissDetail -> {
                _uiState.update { it.copy(selectedAppDetail = null) }
            }
            is AppsEvent.Refresh -> {
                scan()
            }
            is AppsEvent.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    fun scan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, errorMessage = null) }
            try {
                cacheScanner.scanFlow().collect { scanState ->
                    _uiState.update { state ->
                        val updatedState = state.copy(scanState = scanState)
                        if (scanState is ScanState.Complete) {
                            val sorted = AppFilter.filterAndSort(
                                apps = scanState.result.apps,
                                query = state.query,
                                sort = state.sort,
                                showSystemApps = state.showSystemApps,
                                showZeroCacheApps = state.showZeroCacheApps
                            )
                            updatedState.copy(
                                rawApps = scanState.result.apps,
                                displayedApps = sorted,
                                scanResult = scanState.result,
                                isScanning = false
                            )
                        } else if (scanState is ScanState.Failed) {
                            updatedState.copy(
                                isScanning = false,
                                errorMessage = scanState.message
                            )
                        } else {
                            updatedState
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanning = false, errorMessage = e.message) }
            }
        }
    }

    class Factory(
        private val cacheScanner: CacheScanner,
        private val shizukuManager: ShizukuManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppsViewModel(cacheScanner, shizukuManager) as T
        }
    }
}
