package my.id.rmalan.cache.sweep.cleaner

import android.content.Context
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.scanner.CacheScanner
import my.id.rmalan.cache.sweep.util.ByteFormatter

data class FixtureStatusResult(
    val connected: Boolean,
    val cacheBytes: Long = 0L,
    val cacheFilesCount: Int = 0,
    val prefsIntact: Boolean = false,
    val filesIntact: Boolean = false,
    val dbIntact: Boolean = false,
    val isFullyPopulated: Boolean = false,
    val summary: String = ""
)

data class SafetyTestReport(
    val timestamp: Long,
    val fixturePackage: String,
    val fixtureConnected: Boolean,
    val initialFixtureStatus: FixtureStatusResult,
    val storageStatsBefore: AppCacheInfo?,
    val clearCommand: String,
    val clearSuccess: Boolean,
    val storageStatsAfter: AppCacheInfo?,
    val finalFixtureStatus: FixtureStatusResult,
    val cacheDecreased: Boolean,
    val prefsPreserved: Boolean,
    val filesPreserved: Boolean,
    val dbPreserved: Boolean,
    val passed: Boolean,
    val summary: String
)

data class GlobalTrimReport(
    val timestamp: Long,
    val physicalFreeBefore: Long,
    val reportedCacheBefore: Long,
    val desiredFreeTarget: Long,
    val trimSuccess: Boolean,
    val physicalFreeAfter: Long,
    val reportedCacheAfter: Long,
    val physicalFreedDelta: Long,
    val reportedCacheDelta: Long,
    val summary: String
)

class SafetyTestManager(private val context: Context) {

    companion object {
        const val FIXTURE_PACKAGE = "my.id.rmalan.cache.fixture"
        const val FIXTURE_AUTHORITY = "my.id.rmalan.cache.fixture.provider"
        private val FIXTURE_URI: Uri = Uri.parse("content://$FIXTURE_AUTHORITY")

        const val METHOD_POPULATE = "populate"
        const val METHOD_STATUS = "status"

        const val KEY_CACHE_BYTES = "cache_bytes"
        const val KEY_CACHE_FILES_COUNT = "cache_files_count"
        const val KEY_PREFS_INTACT = "prefs_intact"
        const val KEY_FILES_INTACT = "files_intact"
        const val KEY_DB_INTACT = "db_intact"
        const val KEY_IS_FULLY_POPULATED = "is_fully_populated"
        const val KEY_SUMMARY = "summary"
    }

    suspend fun populateFixture(): FixtureStatusResult = withContext(Dispatchers.IO) {
        callProvider(METHOD_POPULATE)
    }

    suspend fun queryFixtureStatus(): FixtureStatusResult = withContext(Dispatchers.IO) {
        callProvider(METHOD_STATUS)
    }

    private fun callProvider(method: String): FixtureStatusResult {
        return try {
            val result: Bundle? = context.contentResolver.call(FIXTURE_URI, method, null, null)
            if (result != null) {
                FixtureStatusResult(
                    connected = true,
                    cacheBytes = result.getLong(KEY_CACHE_BYTES, 0L),
                    cacheFilesCount = result.getInt(KEY_CACHE_FILES_COUNT, 0),
                    prefsIntact = result.getBoolean(KEY_PREFS_INTACT, false),
                    filesIntact = result.getBoolean(KEY_FILES_INTACT, false),
                    dbIntact = result.getBoolean(KEY_DB_INTACT, false),
                    isFullyPopulated = result.getBoolean(KEY_IS_FULLY_POPULATED, false),
                    summary = result.getString(KEY_SUMMARY, "") ?: ""
                )
            } else {
                FixtureStatusResult(connected = false, summary = "Fixture provider returned null")
            }
        } catch (e: Exception) {
            FixtureStatusResult(connected = false, summary = "Fixture provider error: ${e.message}")
        }
    }

    suspend fun runSafetyTest(
        cacheCleaner: CacheCleaner,
        cacheScanner: CacheScanner,
        userId: Int = 0
    ): SafetyTestReport = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()

        // 1. Populate Fixture Data
        val initialStatus = populateFixture()
        if (!initialStatus.connected) {
            return@withContext SafetyTestReport(
                timestamp = timestamp,
                fixturePackage = FIXTURE_PACKAGE,
                fixtureConnected = false,
                initialFixtureStatus = initialStatus,
                storageStatsBefore = null,
                clearCommand = "pm clear --user $userId --cache-only $FIXTURE_PACKAGE",
                clearSuccess = false,
                storageStatsAfter = null,
                finalFixtureStatus = initialStatus,
                cacheDecreased = false,
                prefsPreserved = false,
                filesPreserved = false,
                dbPreserved = false,
                passed = false,
                summary = "Test aborted: Test fixture app '$FIXTURE_PACKAGE' is not installed or unreachable."
            )
        }

        // 2. Query StorageStats Before
        val statsBefore = cacheScanner.scanPackage(FIXTURE_PACKAGE)

        // 3. Execute privileged clear with --cache-only
        val clearSuccess = cacheCleaner.clearPackage(FIXTURE_PACKAGE, userId)

        // 4. Settling delay (500ms)
        delay(500)

        // 5. Query StorageStats After
        val statsAfter = cacheScanner.scanPackage(FIXTURE_PACKAGE)

        // 6. Query Fixture Internal State
        val finalStatus = queryFixtureStatus()

        // 7. Verify Invariants
        val cacheDecreased = if (statsBefore != null && statsAfter != null) {
            statsAfter.cacheBytes < statsBefore.cacheBytes || finalStatus.cacheBytes == 0L
        } else {
            finalStatus.cacheBytes == 0L
        }

        val prefsPreserved = finalStatus.prefsIntact
        val filesPreserved = finalStatus.filesIntact
        val dbPreserved = finalStatus.dbIntact

        val passed = clearSuccess && cacheDecreased && prefsPreserved && filesPreserved && dbPreserved

        val summary = buildString {
            if (passed) {
                append("PASSED: Cache safely cleared (${ByteFormatter.format(initialStatus.cacheBytes)} -> ${ByteFormatter.format(finalStatus.cacheBytes)}). ")
                append("SharedPreferences, App Files, and SQLite DB are 100% INTACT.")
            } else {
                append("FAILED: ")
                if (!clearSuccess) append("Clear command returned failure. ")
                if (!cacheDecreased) append("Cache did not decrease. ")
                if (!prefsPreserved) append("CRITICAL: SharedPreferences were corrupted/deleted! ")
                if (!filesPreserved) append("CRITICAL: Application files were deleted! ")
                if (!dbPreserved) append("CRITICAL: SQLite database was deleted/corrupted! ")
            }
        }

        SafetyTestReport(
            timestamp = timestamp,
            fixturePackage = FIXTURE_PACKAGE,
            fixtureConnected = true,
            initialFixtureStatus = initialStatus,
            storageStatsBefore = statsBefore,
            clearCommand = "pm clear --user $userId --cache-only $FIXTURE_PACKAGE",
            clearSuccess = clearSuccess,
            storageStatsAfter = statsAfter,
            finalFixtureStatus = finalStatus,
            cacheDecreased = cacheDecreased,
            prefsPreserved = prefsPreserved,
            filesPreserved = filesPreserved,
            dbPreserved = dbPreserved,
            passed = passed,
            summary = summary
        )
    }
}
