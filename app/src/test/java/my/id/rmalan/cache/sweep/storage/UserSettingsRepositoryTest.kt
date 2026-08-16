package my.id.rmalan.cache.sweep.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.ThemeMode
import my.id.rmalan.cache.sweep.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UserSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreUserSettingsRepository

    @Before
    fun setUp() {
        val testFile = File(tempFolder.root, "test_user_settings.preferences_pb")
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        repository = DataStoreUserSettingsRepository(testDataStore)
    }

    @Test
    fun defaultSettings_matchExpectedDefaults() = runTest(testDispatcher) {
        val settings = repository.getSettings()

        assertFalse(settings.onboardingCompleted)
        assertFalse(settings.showSystemApps)
        assertTrue(settings.showZeroCacheApps)
        assertEquals(AppSort.CACHE_DESC, settings.sortMode)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
    }

    @Test
    fun setOnboardingCompleted_persistsSuccessfully() = runTest(testDispatcher) {
        assertFalse(repository.getSettings().onboardingCompleted)

        repository.setOnboardingCompleted(true)
        assertTrue(repository.getSettings().onboardingCompleted)

        repository.setOnboardingCompleted(false)
        assertFalse(repository.getSettings().onboardingCompleted)
    }

    @Test
    fun setShowSystemApps_persistsSuccessfully() = runTest(testDispatcher) {
        assertFalse(repository.getSettings().showSystemApps)

        repository.setShowSystemApps(true)
        assertTrue(repository.getSettings().showSystemApps)
    }

    @Test
    fun setShowZeroCacheApps_persistsSuccessfully() = runTest(testDispatcher) {
        assertTrue(repository.getSettings().showZeroCacheApps)

        repository.setShowZeroCacheApps(false)
        assertFalse(repository.getSettings().showZeroCacheApps)
    }

    @Test
    fun setSortMode_persistsSuccessfully() = runTest(testDispatcher) {
        assertEquals(AppSort.CACHE_DESC, repository.getSettings().sortMode)

        repository.setSortMode(AppSort.NAME_ASC)
        assertEquals(AppSort.NAME_ASC, repository.getSettings().sortMode)

        repository.setSortMode(AppSort.TOTAL_DESC)
        assertEquals(AppSort.TOTAL_DESC, repository.getSettings().sortMode)
    }

    @Test
    fun setThemeMode_persistsSuccessfully() = runTest(testDispatcher) {
        assertEquals(ThemeMode.SYSTEM, repository.getSettings().themeMode)

        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.getSettings().themeMode)

        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.getSettings().themeMode)
    }

    @Test
    fun settingsFlow_emitsUpdatesReactively() = runTest(testDispatcher) {
        val initial = repository.settings.first()
        assertFalse(initial.onboardingCompleted)

        repository.setOnboardingCompleted(true)
        val updated = repository.settings.first()
        assertTrue(updated.onboardingCompleted)
    }
}
