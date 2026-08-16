package my.id.rmalan.cache.sweep.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import my.id.rmalan.cache.sweep.model.CleanupHistoryEntry
import my.id.rmalan.cache.sweep.model.CleanupMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CleanupHistoryRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreCleanupHistoryRepository

    @Before
    fun setUp() {
        val testFile = File(tempFolder.root, "test_cleanup_history.preferences_pb")
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        repository = DataStoreCleanupHistoryRepository(testDataStore)
    }

    @Test
    fun initialHistory_isEmpty() = runTest(testDispatcher) {
        val history = repository.getHistory()
        assertTrue(history.isEmpty())
    }

    @Test
    fun addEntry_persistsAndRetrievesEntry() = runTest(testDispatcher) {
        val entry = CleanupHistoryEntry(
            timestampMillis = 1000L,
            mode = CleanupMode.SELECTIVE,
            packagesAttempted = 5,
            packagesSucceeded = 5,
            measuredFreedBytes = 1024L * 1024L * 50L,
            reportedCacheReductionBytes = 1024L * 1024L * 60L,
            durationMillis = 1200L
        )

        repository.addEntry(entry)
        val history = repository.getHistory()

        assertEquals(1, history.size)
        val saved = history.first()
        assertEquals(1000L, saved.timestampMillis)
        assertEquals(CleanupMode.SELECTIVE, saved.mode)
        assertEquals(5, saved.packagesAttempted)
        assertEquals(5, saved.packagesSucceeded)
        assertEquals(1024L * 1024L * 50L, saved.measuredFreedBytes)
        assertEquals(1024L * 1024L * 60L, saved.reportedCacheReductionBytes)
        assertEquals(1200L, saved.durationMillis)
        assertTrue(saved.isCompleteSuccess)
    }

    @Test
    fun multipleEntries_orderedNewestFirst() = runTest(testDispatcher) {
        val entry1 = CleanupHistoryEntry(timestampMillis = 1000L, mode = CleanupMode.SELECTIVE)
        val entry2 = CleanupHistoryEntry(timestampMillis = 2000L, mode = CleanupMode.GLOBAL_TRIM)

        repository.addEntry(entry1)
        repository.addEntry(entry2)

        val history = repository.getHistory()
        assertEquals(2, history.size)
        assertEquals(2000L, history[0].timestampMillis)
        assertEquals(CleanupMode.GLOBAL_TRIM, history[0].mode)
        assertEquals(1000L, history[1].timestampMillis)
        assertEquals(CleanupMode.SELECTIVE, history[1].mode)
    }

    @Test
    fun historyCapacity_boundedAt25EntriesMax() = runTest(testDispatcher) {
        for (i in 1..30) {
            repository.addEntry(
                CleanupHistoryEntry(
                    timestampMillis = i * 1000L,
                    packagesAttempted = i
                )
            )
        }

        val history = repository.getHistory()
        assertEquals(25, history.size)
        // Most recent should be entry 30
        assertEquals(30000L, history[0].timestampMillis)
        assertEquals(30, history[0].packagesAttempted)
        // Oldest kept should be entry 6
        assertEquals(6000L, history.last().timestampMillis)
        assertEquals(6, history.last().packagesAttempted)
    }

    @Test
    fun clearHistory_removesAllEntries() = runTest(testDispatcher) {
        repository.addEntry(CleanupHistoryEntry(timestampMillis = 1000L))
        repository.addEntry(CleanupHistoryEntry(timestampMillis = 2000L))
        assertEquals(2, repository.getHistory().size)

        repository.clearHistory()
        assertTrue(repository.getHistory().isEmpty())
    }

    @Test
    fun corruptedData_recoversGracefullyWithEmptyList() = runTest(testDispatcher) {
        testDataStore.edit { preferences ->
            preferences[DataStoreCleanupHistoryRepository.KEY_CLEANUP_HISTORY] = "corrupted||invalid||||data"
        }

        val history = repository.getHistory()
        assertTrue(history.isEmpty())
    }
}
