package my.id.rmalan.cache.sweep.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupResultTest {

    @Test
    fun `measuredFreedBytes calculates positive delta correctly`() {
        val result = CleanupResult(
            startedAtMillis = 1000L,
            physicalFreeBefore = 10_000_000_000L,
            physicalFreeAfter = 12_000_000_000L,
            cacheBefore = 3_000_000_000L,
            cacheAfter = 1_000_000_000L,
            attemptedPackages = 5,
            successfulPackages = 5,
            failedPackages = emptyList()
        )

        assertEquals(2_000_000_000L, result.measuredFreedBytes)
        assertEquals(2_000_000_000L, result.reportedCacheReduction)
        assertTrue(result.isSignificantReclaim)
        assertTrue(result.isCompleteSuccess)
        assertFalse(result.isPartialSuccess)
        assertFalse(result.isCompleteFailure)
        assertFalse(result.hasFailures)
    }

    @Test
    fun `measuredFreedBytes clamps negative delta to zero`() {
        // Physical free decreased (e.g. background download occurred)
        val result = CleanupResult(
            startedAtMillis = 1000L,
            physicalFreeBefore = 12_000_000_000L,
            physicalFreeAfter = 10_000_000_000L,
            cacheBefore = 1_000_000_000L,
            cacheAfter = 2_000_000_000L,
            attemptedPackages = 3,
            successfulPackages = 3,
            failedPackages = emptyList()
        )

        assertEquals(0L, result.measuredFreedBytes)
        assertEquals(0L, result.reportedCacheReduction)
        assertFalse(result.isSignificantReclaim)
    }

    @Test
    fun `isSignificantReclaim evaluates correctly against 16MB threshold`() {
        val belowThreshold = CleanupResult(
            startedAtMillis = 1000L,
            physicalFreeBefore = 10_000_000L,
            physicalFreeAfter = 10_000_000L + (15L * 1024L * 1024L), // 15 MB
            cacheBefore = 20_000_000L,
            cacheAfter = 5_000_000L,
            attemptedPackages = 1,
            successfulPackages = 1,
            failedPackages = emptyList()
        )
        assertFalse(belowThreshold.isSignificantReclaim)

        val exactThreshold = CleanupResult(
            startedAtMillis = 1000L,
            physicalFreeBefore = 10_000_000L,
            physicalFreeAfter = 10_000_000L + (16L * 1024L * 1024L), // 16 MB
            cacheBefore = 20_000_000L,
            cacheAfter = 4_000_000L,
            attemptedPackages = 1,
            successfulPackages = 1,
            failedPackages = emptyList()
        )
        assertTrue(exactThreshold.isSignificantReclaim)

        val aboveThreshold = CleanupResult(
            startedAtMillis = 1000L,
            physicalFreeBefore = 10_000_000L,
            physicalFreeAfter = 10_000_000L + (100L * 1024L * 1024L), // 100 MB
            cacheBefore = 150_000_000L,
            cacheAfter = 50_000_000L,
            attemptedPackages = 1,
            successfulPackages = 1,
            failedPackages = emptyList()
        )
        assertTrue(aboveThreshold.isSignificantReclaim)
    }

    @Test
    fun `partial and complete failures are accurately classified`() {
        val partial = CleanupResult(
            startedAtMillis = 1000L,
            physicalFreeBefore = 5_000_000L,
            physicalFreeAfter = 6_000_000L,
            cacheBefore = 2_000_000L,
            cacheAfter = 1_000_000L,
            attemptedPackages = 3,
            successfulPackages = 2,
            failedPackages = listOf("com.failed.pkg"),
            errors = mapOf("com.failed.pkg" to CleanerError.CommandFailed(1, "Permission error"))
        )
        assertTrue(partial.hasFailures)
        assertTrue(partial.isPartialSuccess)
        assertFalse(partial.isCompleteSuccess)
        assertFalse(partial.isCompleteFailure)

        val totalFail = CleanupResult(
            startedAtMillis = 1000L,
            physicalFreeBefore = 5_000_000L,
            physicalFreeAfter = 5_000_000L,
            cacheBefore = 2_000_000L,
            cacheAfter = 2_000_000L,
            attemptedPackages = 3,
            successfulPackages = 0,
            failedPackages = listOf("pkg.a", "pkg.b", "pkg.c")
        )
        assertTrue(totalFail.hasFailures)
        assertFalse(totalFail.isPartialSuccess)
        assertFalse(totalFail.isCompleteSuccess)
        assertTrue(totalFail.isCompleteFailure)
    }

    @Test
    fun `failed factory produces proper result with error map`() {
        val failedResult = CleanupResult.failed(
            startedAtMillis = 1000L,
            message = "Shizuku not running",
            error = CleanerError.ShizukuUnavailable,
            mode = CleanupMode.GLOBAL_TRIM
        )

        assertEquals(1000L, failedResult.startedAtMillis)
        assertEquals(0, failedResult.attemptedPackages)
        assertEquals(0, failedResult.successfulPackages)
        assertEquals(CleanupMode.GLOBAL_TRIM, failedResult.mode)
        assertEquals(CleanerError.ShizukuUnavailable, failedResult.errors["cleanup"])
    }
}
