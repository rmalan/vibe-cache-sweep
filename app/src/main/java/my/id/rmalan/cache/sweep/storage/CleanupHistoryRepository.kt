package my.id.rmalan.cache.sweep.storage

import kotlinx.coroutines.flow.Flow
import my.id.rmalan.cache.sweep.model.CleanupHistoryEntry

interface CleanupHistoryRepository {
    /**
     * Observable stream of all recorded cleanup history entries, ordered newest to oldest.
     */
    val history: Flow<List<CleanupHistoryEntry>>

    /**
     * Retrieve the current history snapshot.
     */
    suspend fun getHistory(): List<CleanupHistoryEntry>

    /**
     * Add a completed cleanup entry to history, maintaining maximum capacity (25 records).
     */
    suspend fun addEntry(entry: CleanupHistoryEntry)

    /**
     * Clear all recorded cleanup history.
     */
    suspend fun clearHistory()
}
