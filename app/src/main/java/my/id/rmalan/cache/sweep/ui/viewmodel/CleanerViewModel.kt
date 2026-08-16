package my.id.rmalan.cache.sweep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.id.rmalan.cache.sweep.cleaner.CleanupCoordinator
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.CleaningState
import my.id.rmalan.cache.sweep.model.CleanupMode
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.model.CleanupResult
import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager

data class CleanerUiState(
    val cleaningState: CleaningState = CleaningState.Idle,
    val pendingPlan: CleanupPlan? = null,
    val showConfirmation: Boolean = false,
    val capabilities: CleanerCapabilities? = null,
    val lastResult: CleanupResult? = null,
    val errorMessage: String? = null
) {
    val isCleaning: Boolean
        get() = cleaningState is CleaningState.Validating ||
                cleaningState is CleaningState.SnapshotBefore ||
                cleaningState is CleaningState.Clearing ||
                cleaningState is CleaningState.WaitingForStats ||
                cleaningState is CleaningState.SnapshotAfter

    val isCompleted: Boolean
        get() = cleaningState is CleaningState.Completed

    val isFailed: Boolean
        get() = cleaningState is CleaningState.Failed
}

sealed interface CleanerEvent {
    data class RequestClean(val plan: CleanupPlan) : CleanerEvent
    data object ConfirmClean : CleanerEvent
    data object DismissConfirmation : CleanerEvent
    data object DismissResult : CleanerEvent
    data object Retry : CleanerEvent
    data class RequestGlobalTrimFallback(
        val deviceStorage: DeviceStorageInfo,
        val estimatedCacheBytes: Long
    ) : CleanerEvent
    data object ClearError : CleanerEvent
}

class CleanerViewModel(
    private val coordinator: CleanupCoordinator,
    private val shizukuManager: ShizukuManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState.asStateFlow()

    init {
        loadCapabilities()
    }

    fun loadCapabilities() {
        viewModelScope.launch {
            try {
                val caps = shizukuManager?.fetchCapabilities()
                _uiState.update { it.copy(capabilities = caps) }
            } catch (e: Exception) {
                // Ignore capability load error
            }
        }
    }

    fun onEvent(event: CleanerEvent) {
        when (event) {
            is CleanerEvent.RequestClean -> {
                _uiState.update {
                    it.copy(
                        pendingPlan = event.plan,
                        showConfirmation = true,
                        errorMessage = null
                    )
                }
            }

            is CleanerEvent.ConfirmClean -> {
                val plan = _uiState.value.pendingPlan
                if (plan != null) {
                    _uiState.update { it.copy(showConfirmation = false) }
                    executeClean(plan)
                }
            }

            is CleanerEvent.DismissConfirmation -> {
                _uiState.update {
                    it.copy(
                        pendingPlan = null,
                        showConfirmation = false
                    )
                }
            }

            is CleanerEvent.DismissResult -> {
                _uiState.update {
                    it.copy(
                        cleaningState = CleaningState.Idle,
                        pendingPlan = null,
                        lastResult = null
                    )
                }
            }

            is CleanerEvent.Retry -> {
                val plan = _uiState.value.pendingPlan
                if (plan != null) {
                    executeClean(plan)
                }
            }

            is CleanerEvent.RequestGlobalTrimFallback -> {
                val plan = CleanupPlan.globalTrim(
                    deviceStorage = event.deviceStorage,
                    estimatedCacheBytes = event.estimatedCacheBytes
                )
                _uiState.update {
                    it.copy(
                        pendingPlan = plan,
                        showConfirmation = true,
                        errorMessage = null
                    )
                }
            }

            is CleanerEvent.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun executeClean(plan: CleanupPlan) {
        viewModelScope.launch {
            try {
                coordinator.clean(
                    plan = plan,
                    scannedPackageSet = if (plan.mode == CleanupMode.SELECTIVE) {
                        plan.selectedPackages.toSet()
                    } else null,
                    onProgress = { state ->
                        _uiState.update { current ->
                            val updated = current.copy(cleaningState = state)
                            if (state is CleaningState.Completed) {
                                updated.copy(lastResult = state.result)
                            } else if (state is CleaningState.Failed) {
                                updated.copy(errorMessage = state.message)
                            } else {
                                updated
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cleaningState = CleaningState.Failed(
                            error = null,
                            message = e.localizedMessage ?: "Cleanup failed"
                        ),
                        errorMessage = e.localizedMessage ?: "Cleanup failed"
                    )
                }
            }
        }
    }

    class Factory(
        private val coordinator: CleanupCoordinator,
        private val shizukuManager: ShizukuManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CleanerViewModel(coordinator, shizukuManager) as T
        }
    }
}
