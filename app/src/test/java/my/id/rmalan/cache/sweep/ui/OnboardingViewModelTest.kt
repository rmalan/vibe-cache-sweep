package my.id.rmalan.cache.sweep.ui

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.model.ThemeMode
import my.id.rmalan.cache.sweep.model.UserSettings
import my.id.rmalan.cache.sweep.permissions.UsageAccessManager
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.storage.UserSettingsRepository
import my.id.rmalan.cache.sweep.ui.viewmodel.OnboardingStep
import my.id.rmalan.cache.sweep.ui.viewmodel.OnboardingViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeUsageAccessManager(
    var accessGranted: Boolean = false
) : UsageAccessManager {
    override fun hasAccess(): Boolean = accessGranted
    override fun createSettingsIntent(): Intent = Intent("test.ACTION_USAGE_ACCESS_SETTINGS")
}

private class FakeUserSettingsRepository(
    initialSettings: UserSettings = UserSettings()
) : UserSettingsRepository {
    private val _settingsFlow = MutableStateFlow(initialSettings)
    override val settings: Flow<UserSettings> = _settingsFlow.asStateFlow()

    override suspend fun getSettings(): UserSettings = _settingsFlow.value

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _settingsFlow.update { it.copy(onboardingCompleted = completed) }
    }

    override suspend fun setShowSystemApps(show: Boolean) {
        _settingsFlow.update { it.copy(showSystemApps = show) }
    }

    override suspend fun setShowZeroCacheApps(show: Boolean) {
        _settingsFlow.update { it.copy(showZeroCacheApps = show) }
    }

    override suspend fun setSortMode(sort: AppSort) {
        _settingsFlow.update { it.copy(sortMode = sort) }
    }

    override suspend fun setThemeMode(theme: ThemeMode) {
        _settingsFlow.update { it.copy(themeMode = theme) }
    }
}

private class FakeShizukuManager : ShizukuManager(
    context = android.app.Application()
) {
    var stateFlow = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    var installed: Boolean = false
    var permissionRequested: Boolean = false

    override val state = stateFlow.asStateFlow()

    override fun updateState() {
        // Controlled via stateFlow in tests
    }

    override fun requestPermission() {
        permissionRequested = true
    }

    override fun isShizukuInstalled(): Boolean = installed

    override fun cleanup() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeUsageAccessManager: FakeUsageAccessManager
    private lateinit var fakeShizukuManager: FakeShizukuManager
    private lateinit var fakeSettingsRepository: FakeUserSettingsRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        fakeUsageAccessManager = FakeUsageAccessManager(accessGranted = false)
        fakeShizukuManager = FakeShizukuManager()
        fakeSettingsRepository = FakeUserSettingsRepository()

        viewModel = OnboardingViewModel(
            usageAccessManager = fakeUsageAccessManager,
            shizukuManager = fakeShizukuManager,
            userSettingsRepository = fakeSettingsRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_beginsAtWelcomeStepWithInitialStates() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(OnboardingStep.WELCOME, state.currentStep)
        assertFalse(state.hasUsageAccess)
        assertEquals(ShizukuState.NotRunning, state.shizukuState)
        assertFalse(state.isShizukuInstalled)
        assertFalse(state.isCompleting)
        assertEquals(0.25f, state.progressFraction, 0.001f)
    }

    @Test
    fun stepTransitions_advanceSequentiallyAndClampAtEnd() = runTest(testDispatcher) {
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(OnboardingStep.USAGE_ACCESS, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(OnboardingStep.SHIZUKU, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(OnboardingStep.FIRST_SCAN, viewModel.uiState.value.currentStep)

        // Attempting to advance beyond FIRST_SCAN stays on FIRST_SCAN
        viewModel.nextStep()
        assertEquals(OnboardingStep.FIRST_SCAN, viewModel.uiState.value.currentStep)
    }

    @Test
    fun stepTransitions_retreatSequentiallyAndClampAtStart() = runTest(testDispatcher) {
        viewModel.goToStep(OnboardingStep.FIRST_SCAN)
        assertEquals(OnboardingStep.FIRST_SCAN, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(OnboardingStep.SHIZUKU, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(OnboardingStep.USAGE_ACCESS, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.currentStep)

        // Attempting to go back before WELCOME stays on WELCOME
        viewModel.previousStep()
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.currentStep)
    }

    @Test
    fun refreshState_updatesLivePermissionsAndShizuku() = runTest(testDispatcher) {
        assertFalse(viewModel.uiState.value.hasUsageAccess)

        fakeUsageAccessManager.accessGranted = true
        fakeShizukuManager.installed = true
        fakeShizukuManager.stateFlow.value = ShizukuState.Ready(2000)

        viewModel.refreshState()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasUsageAccess)
        assertTrue(state.isShizukuInstalled)
        assertEquals(ShizukuState.Ready(2000), state.shizukuState)
    }

    @Test
    fun requestShizukuPermission_delegatesToShizukuManager() {
        assertFalse(fakeShizukuManager.permissionRequested)

        viewModel.requestShizukuPermission()

        assertTrue(fakeShizukuManager.permissionRequested)
    }

    @Test
    fun completeOnboarding_persistsSettingAndTriggersCallback() = runTest(testDispatcher) {
        var completedCallbackTriggered = false
        assertFalse(fakeSettingsRepository.getSettings().onboardingCompleted)

        viewModel.completeOnboarding {
            completedCallbackTriggered = true
        }

        advanceUntilIdle()

        assertTrue(fakeSettingsRepository.getSettings().onboardingCompleted)
        assertTrue(completedCallbackTriggered)
        assertFalse(viewModel.uiState.value.isCompleting)
    }
}
