package my.id.rmalan.cache.sweep.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CleaningStateTest {

    @Test
    fun `Clearing progressFraction calculates accurately`() {
        val clearingZero = CleaningState.Clearing(
            current = 0,
            total = 10,
            currentPackage = "com.example.app"
        )
        assertEquals(0f, clearingZero.progressFraction, 0.001f)

        val clearingHalf = CleaningState.Clearing(
            current = 5,
            total = 10,
            currentPackage = "com.example.app",
            currentAppName = "Example App"
        )
        assertEquals(0.5f, clearingHalf.progressFraction, 0.001f)
        assertEquals("Example App", clearingHalf.currentAppName)
        assertEquals("com.example.app", clearingHalf.currentPackage)

        val clearingComplete = CleaningState.Clearing(
            current = 10,
            total = 10,
            currentPackage = "com.example.app"
        )
        assertEquals(1.0f, clearingComplete.progressFraction, 0.001f)

        val clearingEmpty = CleaningState.Clearing(
            current = 0,
            total = 0,
            currentPackage = null
        )
        assertEquals(0f, clearingEmpty.progressFraction, 0.001f)
        assertNull(clearingEmpty.currentPackage)
    }

    @Test
    fun `CleaningState covers all expected states`() {
        val idle: CleaningState = CleaningState.Idle
        val validating: CleaningState = CleaningState.Validating
        val snapshotBefore: CleaningState = CleaningState.SnapshotBefore
        val waiting: CleaningState = CleaningState.WaitingForStats
        val snapshotAfter: CleaningState = CleaningState.SnapshotAfter

        assertTrue(idle is CleaningState.Idle)
        assertTrue(validating is CleaningState.Validating)
        assertTrue(snapshotBefore is CleaningState.SnapshotBefore)
        assertTrue(waiting is CleaningState.WaitingForStats)
        assertTrue(snapshotAfter is CleaningState.SnapshotAfter)

        val failed = CleaningState.Failed(
            error = CleanerError.ShizukuUnavailable,
            message = "Shizuku not running"
        )
        assertEquals(CleanerError.ShizukuUnavailable, failed.error)
        assertEquals("Shizuku not running", failed.message)
    }
}
