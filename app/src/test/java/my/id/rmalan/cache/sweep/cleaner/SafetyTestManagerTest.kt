package my.id.rmalan.cache.sweep.cleaner

import kotlinx.coroutines.runBlocking
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.scanner.CacheScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyTestManagerTest {

    @Test
    fun safetyTestReport_verifiesPassingCondition() {
        val initialStatus = FixtureStatusResult(
            connected = true,
            cacheBytes = 20_971_520L,
            cacheFilesCount = 4,
            prefsIntact = true,
            filesIntact = true,
            dbIntact = true,
            isFullyPopulated = true
        )

        val finalStatus = FixtureStatusResult(
            connected = true,
            cacheBytes = 0L,
            cacheFilesCount = 0,
            prefsIntact = true,
            filesIntact = true,
            dbIntact = true,
            isFullyPopulated = false
        )

        val statsBefore = AppCacheInfo(
            packageName = "my.id.rmalan.cache.fixture",
            appName = "Fixture",
            cacheBytes = 20_971_520L,
            appBytes = 10_000_000L,
            dataBytes = 4_096L,
            isSystemApp = false,
            measurementAvailable = true
        )

        val statsAfter = AppCacheInfo(
            packageName = "my.id.rmalan.cache.fixture",
            appName = "Fixture",
            cacheBytes = 0L,
            appBytes = 10_000_000L,
            dataBytes = 4_096L,
            isSystemApp = false,
            measurementAvailable = true
        )

        val report = SafetyTestReport(
            timestamp = 1000L,
            fixturePackage = "my.id.rmalan.cache.fixture",
            fixtureConnected = true,
            initialFixtureStatus = initialStatus,
            storageStatsBefore = statsBefore,
            clearCommand = "pm clear --user 0 --cache-only my.id.rmalan.cache.fixture",
            clearSuccess = true,
            storageStatsAfter = statsAfter,
            finalFixtureStatus = finalStatus,
            cacheDecreased = true,
            prefsPreserved = true,
            filesPreserved = true,
            dbPreserved = true,
            passed = true,
            summary = "PASSED"
        )

        assertTrue(report.passed)
        assertTrue(report.prefsPreserved)
        assertTrue(report.filesPreserved)
        assertTrue(report.dbPreserved)
        assertTrue(report.cacheDecreased)
    }

    @Test
    fun globalTrimReport_calculatesStorageDeltasCorrectly() {
        val report = GlobalTrimReport(
            timestamp = 1000L,
            physicalFreeBefore = 50_000_000_000L,
            reportedCacheBefore = 2_000_000_000L,
            desiredFreeTarget = 52_000_000_000L,
            trimSuccess = true,
            physicalFreeAfter = 51_500_000_000L,
            reportedCacheAfter = 500_000_000L,
            physicalFreedDelta = 1_500_000_000L,
            reportedCacheDelta = 1_500_000_000L,
            summary = "Trimmed"
        )

        assertTrue(report.trimSuccess)
        assertEquals(1_500_000_000L, report.physicalFreedDelta)
        assertEquals(1_500_000_000L, report.reportedCacheDelta)
    }
}
