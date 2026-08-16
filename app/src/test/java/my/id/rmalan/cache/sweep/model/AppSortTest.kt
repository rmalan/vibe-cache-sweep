package my.id.rmalan.cache.sweep.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSortTest {

    private val appA = AppCacheInfo(
        packageName = "com.example.app_a",
        appName = "Alpha",
        cacheBytes = 500L,
        appBytes = 1000L,
        dataBytes = 500L,
        isSystemApp = false,
        measurementAvailable = true
    ) // total: 2000L, cache: 500L

    private val appB = AppCacheInfo(
        packageName = "com.example.app_b",
        appName = "Beta",
        cacheBytes = 2000L,
        appBytes = 500L,
        dataBytes = 500L,
        isSystemApp = false,
        measurementAvailable = true
    ) // total: 3000L, cache: 2000L

    private val appC = AppCacheInfo(
        packageName = "com.example.app_c",
        appName = "Charlie",
        cacheBytes = 1000L,
        appBytes = 3000L,
        dataBytes = 1000L,
        isSystemApp = true,
        measurementAvailable = true
    ) // total: 5000L, cache: 1000L

    @Test
    fun testSortCacheDesc() {
        val list = listOf(appA, appB, appC)
        val sorted = AppSort.CACHE_DESC.sort(list)

        // Beta (2000), Charlie (1000), Alpha (500)
        assertEquals(listOf(appB, appC, appA), sorted)
    }

    @Test
    fun testSortTotalDesc() {
        val list = listOf(appA, appB, appC)
        val sorted = AppSort.TOTAL_DESC.sort(list)

        // Charlie (5000), Beta (3000), Alpha (2000)
        assertEquals(listOf(appC, appB, appA), sorted)
    }

    @Test
    fun testSortNameAsc() {
        val list = listOf(appC, appA, appB)
        val sorted = AppSort.NAME_ASC.sort(list)

        // Alpha, Beta, Charlie
        assertEquals(listOf(appA, appB, appC), sorted)
    }

    @Test
    fun testSortNameAsc_caseInsensitive() {
        val lowerA = appA.copy(appName = "alpha")
        val upperB = appB.copy(appName = "BETA")
        val mixedC = appC.copy(appName = "charlie")

        val sorted = AppSort.NAME_ASC.sort(listOf(mixedC, upperB, lowerA))
        assertEquals(listOf(lowerA, upperB, mixedC), sorted)
    }

    @Test
    fun testSortWithEmptyList() {
        assertEquals(emptyList<AppCacheInfo>(), AppSort.CACHE_DESC.sort(emptyList()))
        assertEquals(emptyList<AppCacheInfo>(), AppSort.TOTAL_DESC.sort(emptyList()))
        assertEquals(emptyList<AppCacheInfo>(), AppSort.NAME_ASC.sort(emptyList()))
    }
}
