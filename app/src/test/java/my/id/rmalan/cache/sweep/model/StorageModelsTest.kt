package my.id.rmalan.cache.sweep.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageModelsTest {

    @Test
    fun deviceStorageInfo_calculatesUsedBytesCorrectly() {
        val storage = DeviceStorageInfo(
            totalBytes = 128_000_000_000L,
            availableBytes = 40_000_000_000L
        )
        assertEquals(88_000_000_000L, storage.usedBytes)
    }

    @Test
    fun appCacheInfo_calculatesTotalBytesCorrectly() {
        val app = AppCacheInfo(
            packageName = "com.example.app",
            appName = "Example App",
            cacheBytes = 100_000L,
            appBytes = 200_000L,
            dataBytes = 300_000L,
            isSystemApp = false,
            measurementAvailable = true
        )
        assertEquals(600_000L, app.totalBytes)
        assertFalse(app.isSystemApp)
        assertTrue(app.measurementAvailable)
    }

    @Test
    fun appCacheInfo_failedMeasurement_preservesPackageAndAppInfo() {
        val app = AppCacheInfo(
            packageName = "com.example.system",
            appName = "System Service",
            cacheBytes = 0L,
            appBytes = 0L,
            dataBytes = 0L,
            isSystemApp = true,
            measurementAvailable = false
        )
        assertEquals(0L, app.totalBytes)
        assertTrue(app.isSystemApp)
        assertFalse(app.measurementAvailable)
    }

    @Test
    fun scanResult_holdsAggregationCorrectly() {
        val app1 = AppCacheInfo(
            packageName = "com.example.one",
            appName = "App One",
            cacheBytes = 10_000_000L,
            appBytes = 5_000_000L,
            dataBytes = 2_000_000L,
            isSystemApp = false,
            measurementAvailable = true
        )
        val app2 = AppCacheInfo(
            packageName = "com.example.two",
            appName = "App Two",
            cacheBytes = 20_000_000L,
            appBytes = 15_000_000L,
            dataBytes = 5_000_000L,
            isSystemApp = false,
            measurementAvailable = true
        )

        val result = ScanResult(
            apps = listOf(app1, app2),
            attemptedApps = 2,
            successfulApps = 2,
            totalReportedCacheBytes = 30_000_000L,
            durationMillis = 150L
        )

        assertEquals(2, result.attemptedApps)
        assertEquals(2, result.successfulApps)
        assertEquals(30_000_000L, result.totalReportedCacheBytes)
        assertEquals(150L, result.durationMillis)
        assertEquals(2, result.apps.size)
    }
}
