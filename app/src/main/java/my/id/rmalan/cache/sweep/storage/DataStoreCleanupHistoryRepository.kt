package my.id.rmalan.cache.sweep.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import my.id.rmalan.cache.sweep.model.CleanupHistoryEntry
import my.id.rmalan.cache.sweep.model.CleanupMode
import java.io.IOException

private val Context.cleanupHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "cleanup_history")

class DataStoreCleanupHistoryRepository(
    private val dataStore: DataStore<Preferences>
) : CleanupHistoryRepository {

    constructor(context: Context) : this(context.cleanupHistoryDataStore)

    companion object {
        val KEY_CLEANUP_HISTORY = stringPreferencesKey("cleanup_history_records")
        const val MAX_HISTORY_ENTRIES = 25
        private const val FIELD_DELIMITER = "|"
        private const val RECORD_DELIMITER = "\n"
    }

    override val history: Flow<List<CleanupHistoryEntry>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val raw = preferences[KEY_CLEANUP_HISTORY] ?: ""
            deserializeHistory(raw)
        }

    override suspend fun getHistory(): List<CleanupHistoryEntry> {
        return history.first()
    }

    override suspend fun addEntry(entry: CleanupHistoryEntry) {
        dataStore.edit { preferences ->
            val currentRaw = preferences[KEY_CLEANUP_HISTORY] ?: ""
            val currentList = deserializeHistory(currentRaw)
            val updatedList = (listOf(entry) + currentList).take(MAX_HISTORY_ENTRIES)
            preferences[KEY_CLEANUP_HISTORY] = serializeHistory(updatedList)
        }
    }

    override suspend fun clearHistory() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_CLEANUP_HISTORY)
        }
    }

    private fun serializeHistory(entries: List<CleanupHistoryEntry>): String {
        return entries.joinToString(separator = RECORD_DELIMITER) { entry ->
            "${entry.timestampMillis}$FIELD_DELIMITER" +
                "${entry.mode.name}$FIELD_DELIMITER" +
                "${entry.packagesAttempted}$FIELD_DELIMITER" +
                "${entry.packagesSucceeded}$FIELD_DELIMITER" +
                "${entry.measuredFreedBytes}$FIELD_DELIMITER" +
                "${entry.reportedCacheReductionBytes}$FIELD_DELIMITER" +
                "${entry.durationMillis}"
        }
    }

    private fun deserializeHistory(raw: String): List<CleanupHistoryEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split(RECORD_DELIMITER)
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@mapNotNull null
                val parts = trimmed.split(FIELD_DELIMITER)
                if (parts.size < 6) return@mapNotNull null
                try {
                    val timestamp = parts[0].toLongOrNull() ?: return@mapNotNull null
                    val mode = try {
                        CleanupMode.valueOf(parts[1])
                    } catch (e: Exception) {
                        CleanupMode.SELECTIVE
                    }
                    val attempted = parts[2].toIntOrNull() ?: 0
                    val succeeded = parts[3].toIntOrNull() ?: 0
                    val freed = parts[4].toLongOrNull() ?: 0L
                    val reduction = parts[5].toLongOrNull() ?: 0L
                    val duration = if (parts.size >= 7) parts[6].toLongOrNull() ?: 0L else 0L

                    CleanupHistoryEntry(
                        timestampMillis = timestamp,
                        mode = mode,
                        packagesAttempted = attempted,
                        packagesSucceeded = succeeded,
                        measuredFreedBytes = freed,
                        reportedCacheReductionBytes = reduction,
                        durationMillis = duration
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }
}
