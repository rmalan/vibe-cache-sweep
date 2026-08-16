package my.id.rmalan.cache.sweep.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanStateTest {

    @Test
    fun `Scanning progressFraction calculates accurately`() {
        val stateZero = ScanState.Scanning(
            scannedCount = 0,
            totalCount = 100
        )
        assertEquals(0f, stateZero.progressFraction, 0.001f)

        val stateHalf = ScanState.Scanning(
            scannedCount = 50,
            totalCount = 100
        )
        assertEquals(0.5f, stateHalf.progressFraction, 0.001f)

        val stateFull = ScanState.Scanning(
            scannedCount = 100,
            totalCount = 100
        )
        assertEquals(1.0f, stateFull.progressFraction, 0.001f)

        val stateEmpty = ScanState.Scanning(
            scannedCount = 0,
            totalCount = 0
        )
        assertEquals(0f, stateEmpty.progressFraction, 0.001f)
    }

    @Test
    fun `ScanResult EMPTY provides clean zero-initialized default`() {
        val empty = ScanResult.EMPTY
        assertEquals(0, empty.apps.size)
        assertEquals(0, empty.attemptedApps)
        assertEquals(0, empty.successfulApps)
        assertEquals(0L, empty.totalReportedCacheBytes)
        assertEquals(0L, empty.durationMillis)
    }
}
