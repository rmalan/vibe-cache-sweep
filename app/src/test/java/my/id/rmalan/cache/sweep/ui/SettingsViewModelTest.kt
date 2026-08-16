package my.id.rmalan.cache.sweep.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.CleanupHistoryEntry
import my.id.rmalan.cache.sweep.model.CleanupMode
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.model.ThemeMode
import my.id.rmalan.cache.sweep.model.UserSettings
import my.id.rmalan.cache.sweep.storage.CleanupHistoryRepository
import my.id.rmalan.cache.sweep.storage.UserSettingsRepository
import my.id.rmalan.cache.sweep.ui.viewmodel.SettingsEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.SettingsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeSettingsUserSettingsRepository(
    initialSettings: UserSettings = UserSettings()
) : UserSettingsRepository {
    private val _settingsFlow = MutableStateFlow(initialSettings)
    override val settings: Flow<UserSettings> = _settingsFlow.asStateFlow()

    override suspend fun getSettings(): UserSettings = _settingsFlow.value

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _settingsFlow.value = _settingsFlow.value.copy(onboardingCompleted = completed)
    }

    override suspend fun setShowSystemApps(show: Boolean) {
        _settingsFlow.value = _settingsFlow.value.copy(showSystemApps = show)
    }

    override suspend fun setShowZeroCacheApps(show: Boolean) {
        _settingsFlow.value = _settingsFlow.value.copy(showZeroCacheApps = show)
    }

    override suspend fun setSortMode(sort: AppSort) {
        _settingsFlow.value = _settingsFlow.value.copy(sortMode = sort)
    }

    override suspend fun setThemeMode(theme: ThemeMode) {
        _settingsFlow.value = _settingsFlow.value.copy(themeMode = theme)
    }
}

private class FakeCleanupHistoryRepository(
    initialHistory: List<CleanupHistoryEntry> = emptyList()
) : CleanupHistoryRepository {
    private val _historyFlow = MutableStateFlow(initialHistory)
    override val history: Flow<List<CleanupHistoryEntry>> = _historyFlow.asStateFlow()

    override suspend fun getHistory(): List<CleanupHistoryEntry> = _historyFlow.value

    override suspend fun addEntry(entry: CleanupHistoryEntry) {
        _historyFlow.value = (listOf(entry) + _historyFlow.value).take(25)
    }

    override suspend fun clearHistory() {
        _historyFlow.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSettingsRepository: FakeSettingsUserSettingsRepository
    private lateinit var fakeHistoryRepository: FakeCleanupHistoryRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSettingsRepository = FakeSettingsUserSettingsRepository()
        fakeHistoryRepository = FakeCleanupHistoryRepository()
        viewModel = SettingsViewModel(
            userSettingsRepository = fakeSettingsRepository,
            cleanupHistoryRepository = fakeHistoryRepository,
            shizukuManager = null,
            appVersion = "1.0.0"
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsDefaultsAndAppVersion() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("1.0.0", state.appVersion)
        assertFalse(state.settings.showSystemApps)
        assertTrue(state.settings.showZeroCacheApps)
        assertEquals(AppSort.CACHE_DESC, state.settings.sortMode)
        assertEquals(ThemeMode.SYSTEM, state.settings.themeMode)
        assertEquals(0, state.historyCount)
        assertNull(state.historyClearedMessage)
    }

    @Test
    fun toggleShowSystemApps_updatesStateAndRepository() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.showSystemApps)

        viewModel.onEvent(SettingsEvent.ToggleShowSystemApps(true))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.settings.showSystemApps)
        assertTrue(fakeSettingsRepository.getSettings().showSystemApps)
    }

    @Test
    fun toggleShowZeroCacheApps_updatesStateAndRepository() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.settings.showZeroCacheApps)

        viewModel.onEvent(SettingsEvent.ToggleShowZeroCacheApps(false))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.settings.showZeroCacheApps)
        assertFalse(fakeSettingsRepository.getSettings().showZeroCacheApps)
    }

    @Test
    fun setSortMode_updatesStateAndRepository() = runTest {
        advanceUntilIdle()
        assertEquals(AppSort.CACHE_DESC, viewModel.uiState.value.settings.sortMode)

        viewModel.onEvent(SettingsEvent.SetSortMode(AppSort.TOTAL_DESC))
        advanceUntilIdle()

        assertEquals(AppSort.TOTAL_DESC, viewModel.uiState.value.settings.sortMode)
        assertEquals(AppSort.TOTAL_DESC, fakeSettingsRepository.getSettings().sortMode)
    }

    @Test
    fun setThemeMode_updatesStateAndRepository() = runTest {
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.settings.themeMode)

        viewModel.onEvent(SettingsEvent.SetThemeMode(ThemeMode.DARK))
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.settings.themeMode)
        assertEquals(ThemeMode.DARK, fakeSettingsRepository.getSettings().themeMode)
    }

    @Test
    fun clearHistory_removesEntriesAndSetsMessage() = runTest {
        fakeHistoryRepository.addEntry(
            CleanupHistoryEntry(
                timestampMillis = 1000L,
                mode = CleanupMode.SELECTIVE,
                packagesAttempted = 5,
                packagesSucceeded = 5
            )
        )
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.historyCount)

        viewModel.onEvent(SettingsEvent.ClearHistory)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.historyCount)
        assertEquals("Cleanup history cleared", viewModel.uiState.value.historyClearedMessage)

        viewModel.onEvent(SettingsEvent.DismissHistoryMessage)
        assertNull(viewModel.uiState.value.historyClearedMessage)
    }
}
