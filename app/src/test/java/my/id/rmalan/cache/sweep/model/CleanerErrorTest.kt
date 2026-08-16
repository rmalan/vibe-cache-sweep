package my.id.rmalan.cache.sweep.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanerErrorTest {

    @Test
    fun `cleaner error variants have informative descriptions`() {
        val unavailable = CleanerError.ShizukuUnavailable
        assertTrue(unavailable.description.contains("unreachable") || unavailable.description.contains("running"))

        val denied = CleanerError.PermissionDenied
        assertTrue(denied.description.contains("permission"))

        val selective = CleanerError.SelectiveUnsupported
        assertTrue(selective.description.contains("Selective"))

        val global = CleanerError.GlobalTrimUnsupported
        assertTrue(global.description.contains("Global"))

        val invalid = CleanerError.PackageInvalid("bad package")
        assertTrue(invalid.description.contains("bad package"))

        val selfClean = CleanerError.SelfCleanProhibited("my.id.rmalan.cache.sweep")
        assertTrue(selfClean.description.contains("prohibited"))

        val cmdFailed = CleanerError.CommandFailed(1, "error clearing")
        assertTrue(cmdFailed.description.contains("exit code 1"))
        assertTrue(cmdFailed.description.contains("error clearing"))

        val unexpected = CleanerError.Unexpected(IllegalStateException("boom"))
        assertEquals("boom", unexpected.description)
    }

    @Test
    fun `cleaning progress computes progress fraction`() {
        val progress = CleaningProgress(
            current = 5,
            total = 10,
            currentPackageName = "com.test.app",
            currentAppName = "Test App"
        )
        assertEquals(0.5f, progress.progressFraction, 0.001f)
        assertEquals("com.test.app", progress.currentPackageName)
        assertEquals("Test App", progress.currentAppName)

        val zeroProgress = CleaningProgress(
            current = 0,
            total = 0,
            currentPackageName = null
        )
        assertEquals(0f, zeroProgress.progressFraction, 0.001f)
    }

    @Test
    fun `cleaner batch result tracks success, partial success, and failure`() {
        val completeSuccess = CleanerBatchResult(
            totalAttempted = 3,
            successfulPackages = listOf("pkg.a", "pkg.b", "pkg.c"),
            failedPackages = emptyList()
        )
        assertTrue(completeSuccess.isCompleteSuccess)
        assertFalse(completeSuccess.isPartialSuccess)
        assertFalse(completeSuccess.isCompleteFailure)

        val partial = CleanerBatchResult(
            totalAttempted = 3,
            successfulPackages = listOf("pkg.a", "pkg.b"),
            failedPackages = listOf("pkg.c"),
            errors = mapOf("pkg.c" to CleanerError.PackageInvalid("pkg.c"))
        )
        assertFalse(partial.isCompleteSuccess)
        assertTrue(partial.isPartialSuccess)
        assertFalse(partial.isCompleteFailure)

        val completeFailure = CleanerBatchResult(
            totalAttempted = 2,
            successfulPackages = emptyList(),
            failedPackages = listOf("pkg.a", "pkg.b"),
            errors = mapOf(
                "pkg.a" to CleanerError.ShizukuUnavailable,
                "pkg.b" to CleanerError.ShizukuUnavailable
            )
        )
        assertFalse(completeFailure.isCompleteSuccess)
        assertFalse(completeFailure.isPartialSuccess)
        assertTrue(completeFailure.isCompleteFailure)
    }

    @Test
    fun `single result factory handles success and failure correctly`() {
        val success = CleanerBatchResult.single("com.example.app", true)
        assertEquals(1, success.totalAttempted)
        assertEquals(listOf("com.example.app"), success.successfulPackages)
        assertTrue(success.failedPackages.isEmpty())
        assertTrue(success.isCompleteSuccess)

        val failError = CleanerError.PackageInvalid("invalid.pkg")
        val failure = CleanerBatchResult.single("invalid.pkg", false, failError)
        assertEquals(1, failure.totalAttempted)
        assertTrue(failure.successfulPackages.isEmpty())
        assertEquals(listOf("invalid.pkg"), failure.failedPackages)
        assertEquals(failError, failure.errors["invalid.pkg"])
        assertTrue(failure.isCompleteFailure)
    }
}
