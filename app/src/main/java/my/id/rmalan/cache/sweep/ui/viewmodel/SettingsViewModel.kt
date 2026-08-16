package my.id.rmalan.cache.sweep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.CleanupHistoryEntry
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.model.ThemeMode
import my.id.rmalan.cache.sweep.model.UserSettings
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.storage.CleanupHistoryRepository
import my.id.rmalan.cache.sweep.storage.UserSettingsRepository

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val shizukuState: ShizukuState = ShizukuState.NotRunning,
    val historyEntries: List<CleanupHistoryEntry> = emptyList(),
    val isClearingHistory: Boolean = false,
    val historyClearedMessage: String? = null,
    val appVersion: String = "1.0.0"
) {
    val historyCount: Int
        get() = historyEntries.size
}

sealed interface SettingsEvent {
    data class ToggleShowSystemApps(val show: Boolean) : SettingsEvent
    data class ToggleShowZeroCacheApps(val show: Boolean) : SettingsEvent
    data class SetSortMode(val sort: AppSort) : SettingsEvent
    data class SetThemeMode(val theme: ThemeMode) : SettingsEvent
    data object ClearHistory : SettingsEvent
    data object DismissHistoryMessage : SettingsEvent
    data object RequestShizukuPermission : SettingsEvent
    data object RefreshShizuku : SettingsEvent
}

class SettingsViewModel(
    private val userSettingsRepository: UserSettingsRepository,
    private val cleanupHistoryRepository: CleanupHistoryRepository,
    private val shizukuManager: ShizukuManager? = null,
    appVersion: String = "1.0.0"
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(appVersion = appVersion))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        observeHistory()
        observeShizukuState()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            userSettingsRepository.settings
                .catch { /* use defaults on error */ }
                .collect { settings ->
                    _uiState.update { it.copy(settings = settings) }
                }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            cleanupHistoryRepository.history
                .catch { /* use empty list on error */ }
                .collect { history ->
                    _uiState.update { it.copy(historyEntries = history) }
                }
        }
    }

    private fun observeShizukuState() {
        val manager = shizukuManager ?: return
        viewModelScope.launch {
            manager.state.collect { state ->
                _uiState.update { it.copy(shizukuState = state) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleShowSystemApps -> {
                viewModelScope.launch {
                    userSettingsRepository.setShowSystemApps(event.show)
                }
            }
            is SettingsEvent.ToggleShowZeroCacheApps -> {
                viewModelScope.launch {
                    userSettingsRepository.setShowZeroCacheApps(event.show)
                }
            }
            is SettingsEvent.SetSortMode -> {
                viewModelScope.launch {
                    userSettingsRepository.setSortMode(event.sort)
                }
            }
            is SettingsEvent.SetThemeMode -> {
                viewModelScope.launch {
                    userSettingsRepository.setThemeMode(event.theme)
                }
            }
            is SettingsEvent.ClearHistory -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isClearingHistory = true) }
                    try {
                        cleanupHistoryRepository.clearHistory()
                        _uiState.update {
                            it.copy(
                                isClearingHistory = false,
                                historyClearedMessage = "Cleanup history cleared"
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isClearingHistory = false,
                                historyClearedMessage = "Failed to clear history: ${e.message}"
                            )
                        }
                    }
                }
            }
            is SettingsEvent.DismissHistoryMessage -> {
                _uiState.update { it.copy(historyClearedMessage = null) }
            }
            is SettingsEvent.RequestShizukuPermission -> {
                shizukuManager?.requestPermission()
            }
            is SettingsEvent.RefreshShizuku -> {
                shizukuManager?.updateState()
            }
        }
    }

    class Factory(
        private val userSettingsRepository: UserSettingsRepository,
        private val cleanupHistoryRepository: CleanupHistoryRepository,
        private val shizukuManager: ShizukuManager? = null,
        private val appVersion: String = "1.0.0"
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                userSettingsRepository = userSettingsRepository,
                cleanupHistoryRepository = cleanupHistoryRepository,
                shizukuManager = shizukuManager,
                appVersion = appVersion
            ) as T
        }
    }
}
