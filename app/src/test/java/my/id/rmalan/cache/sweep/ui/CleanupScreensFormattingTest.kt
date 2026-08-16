package my.id.rmalan.cache.sweep.ui

import my.id.rmalan.cache.sweep.model.CleanerError
import my.id.rmalan.cache.sweep.model.CleaningState
import my.id.rmalan.cache.sweep.model.CleanupMode
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.model.CleanupResult
import my.id.rmalan.cache.sweep.util.ByteFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupScreensFormattingTest {

    @Test
    fun testSignificantReclaimResultDisplay() {
        val result = CleanupResult(
            startedAtMillis = 1000L,
            physicalFreeBefore = 20L * 1024L * 1024L * 1024L,
            physicalFreeAfter = 22L * 1024L * 1024L * 1024L, // +2 GB
            cacheBefore = 5L * 1024L * 1024L * 1024L,
            cacheAfter = 3L * 1024L * 1024L * 1024L,
            attemptedPackages = 10,
            successfulPackages = 10,
            failedPackages = emptyList(),
            mode = CleanupMode.SELECTIVE,
            completedAtMillis = 3500L
        )

        assertTrue(result.isSignificantReclaim)
        assertEquals(2L * 1024L * 1024L * 1024L, result.measuredFreedBytes)
        assertEquals("2.00 GB", ByteFormatter.format(result.measuredFreedBytes))
        assertEquals(2500L, result.durationMillis)
        assertTrue(result.isCompleteSuccess)
        assertFalse(result.hasFailures)
    }

    @Test
    fun testNoiseThresholdResultDisplay() {
        val result = CleanupResult(
            startedAtMillis = 1000L,
            physicalFreeBefore = 20_000_000_000L,
            physicalFreeAfter = 20_005_000_000L, // +5 MB (< 16MB threshold)
            cacheBefore = 500_000_000L,
            cacheAfter = 495_000_000L,
            attemptedPackages = 5,
            successfulPackages = 5,
            failedPackages = emptyList(),
            mode = CleanupMode.SELECTIVE,
            completedAtMillis = 2000L
        )

        assertFalse(result.isSignificantReclaim)
        assertEquals(5_000_000L, result.measuredFreedBytes)
    }

    @Test
    fun testPartialFailuresResultDisplay() {
        val failedPkg1 = "com.android.systemui"
        val failedPkg2 = "com.google.android.gms"
        val errors = mapOf(
            failedPkg1 to CleanerError.CommandFailed(1, "Permission restricted"),
            failedPkg2 to CleanerError.SelectiveUnsupported
        )

        val result = CleanupResult(
            startedAtMillis = 1000L,
            physicalFreeBefore = 20_000_000_000L,
            physicalFreeAfter = 21_000_000_000L,
            cacheBefore = 2_000_000_000L,
            cacheAfter = 1_000_000_000L,
            attemptedPackages = 5,
            successfulPackages = 3,
            failedPackages = listOf(failedPkg1, failedPkg2),
            errors = errors,
            mode = CleanupMode.SELECTIVE,
            completedAtMillis = 2000L
        )

        assertTrue(result.hasFailures)
        assertTrue(result.isPartialSuccess)
        assertFalse(result.isCompleteSuccess)
        assertEquals(2, result.failedPackages.size)
        assertEquals("Permission restricted", (result.errors[failedPkg1] as CleanerError.CommandFailed).rawError)
        assertEquals(CleanerError.SelectiveUnsupported, result.errors[failedPkg2])
    }

    @Test
    fun testCleaningStateProgressFractions() {
        val state1 = CleaningState.Clearing(
            current = 0,
            total = 10,
            currentPackage = "com.app.zero"
        )
        assertEquals(0f, state1.progressFraction, 0.001f)

        val state2 = CleaningState.Clearing(
            current = 5,
            total = 10,
            currentPackage = "com.app.five"
        )
        assertEquals(0.5f, state2.progressFraction, 0.001f)

        val state3 = CleaningState.Clearing(
            current = 10,
            total = 10,
            currentPackage = "com.app.ten"
        )
        assertEquals(1.0f, state3.progressFraction, 0.001f)

        val stateZero = CleaningState.Clearing(
            current = 0,
            total = 0,
            currentPackage = null
        )
        assertEquals(0f, stateZero.progressFraction, 0.001f)
    }

    @Test
    fun testConfirmationDialogPlanMetadata() {
        val selectivePlan = CleanupPlan.selective(
            packages = listOf("com.app.a", "com.app.b", "com.app.c"),
            estimatedCacheBytes = 1_500_000_000L
        )
        assertEquals(CleanupMode.SELECTIVE, selectivePlan.mode)
        assertEquals(3, selectivePlan.selectedPackages.size)
        assertEquals("1.40 GB", ByteFormatter.format(selectivePlan.estimatedCacheBytes))

        val singlePlan = CleanupPlan.selectiveSingle(
            packageName = "com.single.app",
            estimatedCacheBytes = 250_000_000L
        )
        assertEquals(1, singlePlan.selectedPackages.size)
        assertEquals("com.single.app", singlePlan.selectedPackages.first())
    }
}
