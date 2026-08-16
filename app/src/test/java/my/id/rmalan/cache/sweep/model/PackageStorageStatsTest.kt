package my.id.rmalan.cache.sweep.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageStorageStatsTest {

    @Test
    fun totalBytes_computesCorrectSum() {
        val stats = PackageStorageStats(
            cacheBytes = 15_000_000L,
            appBytes = 25_000_000L,
            dataBytes = 60_000_000L,
            measurementAvailable = true
        )
        assertEquals(100_000_000L, stats.totalBytes)
        assertTrue(stats.measurementAvailable)
        assertNull(stats.errorMessage)
    }

    @Test
    fun zero_returnsAllZeroValues() {
        val zero = PackageStorageStats.ZERO
        assertEquals(0L, zero.cacheBytes)
        assertEquals(0L, zero.appBytes)
        assertEquals(0L, zero.dataBytes)
        assertEquals(0L, zero.totalBytes)
        assertTrue(zero.measurementAvailable)
        assertNull(zero.errorMessage)
    }

    @Test
    fun failed_returnsUnmeasuredWithErrorMessage() {
        val failed = PackageStorageStats.failed("Permission denied")
        assertEquals(0L, failed.cacheBytes)
        assertEquals(0L, failed.appBytes)
        assertEquals(0L, failed.dataBytes)
        assertEquals(0L, failed.totalBytes)
        assertFalse(failed.measurementAvailable)
        assertEquals("Permission denied", failed.errorMessage)
    }
}
