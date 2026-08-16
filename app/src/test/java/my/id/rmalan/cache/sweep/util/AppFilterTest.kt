package my.id.rmalan.cache.sweep.util

import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.AppSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterTest {

    private val instagram = AppCacheInfo(
        packageName = "com.instagram.android",
        appName = "Instagram",
        cacheBytes = 1_500_000_000L,
        appBytes = 100_000_000L,
        dataBytes = 200_000_000L,
        isSystemApp = false,
        measurementAvailable = true
    )

    private val chrome = AppCacheInfo(
        packageName = "com.android.chrome",
        appName = "Chrome",
        cacheBytes = 1_000_000_000L,
        appBytes = 80_000_000L,
        dataBytes = 500_000_000L,
        isSystemApp = true,
        measurementAvailable = true
    )

    private val unusedApp = AppCacheInfo(
        packageName = "com.example.unused",
        appName = "Unused Utility",
        cacheBytes = 0L,
        appBytes = 5_000_000L,
        dataBytes = 10_000L,
        isSystemApp = false,
        measurementAvailable = true
    )

    private val sampleApps = listOf(instagram, chrome, unusedApp)

    @Test
    fun testFilterByAppNameQuery() {
        val result = AppFilter.filterAndSort(sampleApps, query = "insta")
        assertEquals(1, result.size)
        assertEquals("Instagram", result[0].appName)
    }

    @Test
    fun testFilterByPackageNameQuery() {
        val result = AppFilter.filterAndSort(sampleApps, query = "android.chrome")
        assertEquals(1, result.size)
        assertEquals("Chrome", result[0].appName)
    }

    @Test
    fun testFilterCaseInsensitive() {
        val result = AppFilter.filterAndSort(sampleApps, query = "CHROME")
        assertEquals(1, result.size)
        assertEquals("Chrome", result[0].appName)
    }

    @Test
    fun testFilterNoMatches() {
        val result = AppFilter.filterAndSort(sampleApps, query = "nonexistent_app_query")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testHideZeroCacheApps() {
        val result = AppFilter.filterAndSort(sampleApps, showZeroCacheApps = false)
        assertEquals(2, result.size)
        assertTrue(result.none { it.cacheBytes == 0L })
    }

    @Test
    fun testHideSystemApps() {
        val result = AppFilter.filterAndSort(sampleApps, showSystemApps = false)
        assertEquals(2, result.size)
        assertTrue(result.none { it.isSystemApp })
    }

    @Test
    fun testHideSystemAndZeroCacheApps() {
        val result = AppFilter.filterAndSort(
            sampleApps,
            showSystemApps = false,
            showZeroCacheApps = false
        )
        assertEquals(1, result.size)
        assertEquals("Instagram", result[0].appName)
    }

    @Test
    fun testFilterAndSortCombined() {
        val result = AppFilter.filterAndSort(
            sampleApps,
            query = "",
            sort = AppSort.TOTAL_DESC,
            showSystemApps = true,
            showZeroCacheApps = true
        )
        // Instagram: 1.8GB, Chrome: 1.58GB, Unused: ~5MB
        assertEquals(listOf(instagram, chrome, unusedApp), result)
    }

    @Test
    fun testBlankQueryReturnsAllMatchingToggles() {
        val result = AppFilter.filterAndSort(sampleApps, query = "   ")
        assertEquals(3, result.size)
    }
}
