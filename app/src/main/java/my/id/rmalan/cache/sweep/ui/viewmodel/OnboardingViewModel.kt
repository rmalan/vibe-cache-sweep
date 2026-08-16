package my.id.rmalan.cache.sweep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.permissions.UsageAccessManager
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.storage.UserSettingsRepository

enum class OnboardingStep(val stepNumber: Int, val totalSteps: Int = 4) {
    WELCOME(1),
    USAGE_ACCESS(2),
    SHIZUKU(3),
    FIRST_SCAN(4);

    val isFirst: Boolean get() = this == WELCOME
    val isLast: Boolean get() = this == FIRST_SCAN

    fun next(): OnboardingStep {
        val entries = entries
        val nextIdx = (ordinal + 1).coerceAtMost(entries.size - 1)
        return entries[nextIdx]
    }

    fun previous(): OnboardingStep {
        val entries = entries
        val prevIdx = (ordinal - 1).coerceAtLeast(0)
        return entries[prevIdx]
    }
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val hasUsageAccess: Boolean = false,
    val shizukuState: ShizukuState = ShizukuState.NotRunning,
    val isShizukuInstalled: Boolean = false,
    val isCompleting: Boolean = false
) {
    val progressFraction: Float get() = currentStep.stepNumber.toFloat() / currentStep.totalSteps.toFloat()
}

class OnboardingViewModel(
    private val usageAccessManager: UsageAccessManager,
    private val shizukuManager: ShizukuManager,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            hasUsageAccess = usageAccessManager.hasAccess(),
            shizukuState = shizukuManager.state.value,
            isShizukuInstalled = shizukuManager.isShizukuInstalled()
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            shizukuManager.state.collect { state ->
                _uiState.update { it.copy(shizukuState = state) }
            }
        }
        refreshState()
    }

    fun refreshState() {
        shizukuManager.updateState()
        val usageAccess = usageAccessManager.hasAccess()
        val shizukuInstalled = shizukuManager.isShizukuInstalled()
        _uiState.update {
            it.copy(
                hasUsageAccess = usageAccess,
                isShizukuInstalled = shizukuInstalled,
                shizukuState = shizukuManager.state.value
            )
        }
    }

    fun nextStep() {
        _uiState.update { it.copy(currentStep = it.currentStep.next()) }
    }

    fun previousStep() {
        _uiState.update { it.copy(currentStep = it.currentStep.previous()) }
    }

    fun goToStep(step: OnboardingStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    fun completeOnboarding(onCompleted: () -> Unit) {
        if (_uiState.value.isCompleting) return
        _uiState.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            try {
                userSettingsRepository.setOnboardingCompleted(true)
            } finally {
                _uiState.update { it.copy(isCompleting = false) }
                onCompleted()
            }
        }
    }

    class Factory(
        private val usageAccessManager: UsageAccessManager,
        private val shizukuManager: ShizukuManager,
        private val userSettingsRepository: UserSettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                return OnboardingViewModel(
                    usageAccessManager = usageAccessManager,
                    shizukuManager = shizukuManager,
                    userSettingsRepository = userSettingsRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
