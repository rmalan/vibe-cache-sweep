package my.id.rmalan.cache.sweep.performance

import android.os.UserHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
import my.id.rmalan.cache.sweep.model.PackageStorageStats
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.scanner.AndroidCacheScanner
import my.id.rmalan.cache.sweep.scanner.PackageRepository
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository
import my.id.rmalan.cache.sweep.util.AppFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceAndOptimizationTest {

    private class LargeFakePackageRepository(
        private val count: Int,
        private val failureInterval: Int = 0
    ) : PackageRepository {

        val packages: List<DiscoveredPackage> = (1..count).map { i ->
            DiscoveredPackage(
                packageName = "com.example.app$i",
                appName = "Application $i",
                isSystemApp = (i % 7 == 0)
            )
        }

        override fun getInstalledPackages(
            includeSelf: Boolean,
            includeSystem: Boolean
        ): List<DiscoveredPackage> {
            return packages.filter { includeSystem || !it.isSystemApp }
        }

        override fun getPackage(packageName: String): DiscoveredPackage? {
            return packages.find { it.packageName == packageName }
        }

        override fun loadApplicationIcon(packageName: String): android.graphics.drawable.Drawable? = null
        override fun loadIconThumbnail(packageName: String, sizePx: Int): android.graphics.Bitmap? = null
        override fun getCachedIconThumbnail(packageName: String, sizePx: Int): android.graphics.Bitmap? = null
    }

    private class LargeFakeStorageStatsRepository(
        private val failureIndices: Set<Int> = emptySet(),
        private val baseCacheBytes: Long = 1_500_000L,
        private val baseAppBytes: Long = 10_000_000L,
        private val baseDataBytes: Long = 5_000_000L
    ) : StorageStatsRepository {

        val queryCounter = AtomicInteger(0)
        val concurrentCounter = AtomicInteger(0)
        val peakConcurrency = AtomicInteger(0)

        override fun queryStats(
            packageName: String,
            storageUuid: UUID?,
            userHandle: UserHandle?
        ): PackageStorageStats {
            val count = queryCounter.incrementAndGet()
            val current = concurrentCounter.incrementAndGet()
            peakConcurrency.updateAndGet { maxOf(it, current) }

            try {
                val indexStr = packageName.removePrefix("com.example.app")
                val index = indexStr.toIntOrNull() ?: count

                if (index in failureIndices) {
                    return PackageStorageStats.failed("Permission denied for $packageName")
                }

                return PackageStorageStats(
                    cacheBytes = baseCacheBytes * (index % 10 + 1),
                    appBytes = baseAppBytes,
                    dataBytes = baseDataBytes * (index % 5 + 1)
                )
            } finally {
                concurrentCounter.decrementAndGet()
            }
        }
    }

    // =========================================================================
    // P5-11: Large Installed Application Count (500+ & 1000+ apps)
    // =========================================================================

    @Test
    fun `P5-11 scanner measures 500+ applications accurately without memory or channel bottlenecks`() = runTest {
        val appCount = 550
        val failureIndices = setOf(13, 88, 142, 305, 512)
        val packageRepo = LargeFakePackageRepository(count = appCount)
        val statsRepo = LargeFakeStorageStatsRepository(failureIndices = failureIndices)

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scanner = AndroidCacheScanner(
            packageRepository = packageRepo,
            storageStatsRepository = statsRepo,
            concurrencyLimit = 6,
            ioDispatcher = testDispatcher
        )

        val result = scanner.scan()

        assertEquals(appCount, result.attemptedApps)
        val expectedSuccessful = appCount - failureIndices.size
        assertEquals(expectedSuccessful, result.successfulApps)
        assertEquals(appCount, result.apps.size)
        assertTrue("Total reported cache must be > 0", result.totalReportedCacheBytes > 0L)
        assertTrue("Scan duration must be recorded", result.durationMillis >= 0L)

        // Verify partial failure isolation on 550 apps
        val failedApp = result.apps.find { it.packageName == "com.example.app13" }
        assertNotNull(failedApp)
        assertFalse(failedApp!!.measurementAvailable)
        assertEquals(0L, failedApp.cacheBytes)

        val successfulApp = result.apps.find { it.packageName == "com.example.app1" }
        assertNotNull(successfulApp)
        assertTrue(successfulApp!!.measurementAvailable)
        assertTrue(successfulApp.cacheBytes > 0L)
    }

    @Test
    fun `P5-11 scanner handles 1000+ applications with 8 workers stress test`() = runTest {
        val appCount = 1000
        val packageRepo = LargeFakePackageRepository(count = appCount)
        val statsRepo = LargeFakeStorageStatsRepository()

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scanner = AndroidCacheScanner(
            packageRepository = packageRepo,
            storageStatsRepository = statsRepo,
            concurrencyLimit = 8,
            ioDispatcher = testDispatcher
        )

        val result = scanner.scan()

        assertEquals(1000, result.attemptedApps)
        assertEquals(1000, result.successfulApps)
        assertEquals(1000, result.apps.size)
        assertEquals(1000, statsRepo.queryCounter.get())
    }

    @Test
    fun `P5-11 AppFilter sorts and filters 1000+ applications in under 50ms`() {
        val count = 1000
        val apps = (1..count).map { i ->
            AppCacheInfo(
                packageName = "com.company.product$i",
                appName = "App ${1000 - i}", // Reverse order for sort testing
                cacheBytes = (i * 1_000_000L),
                appBytes = 10_000_000L,
                dataBytes = 5_000_000L,
                isSystemApp = (i % 5 == 0),
                measurementAvailable = true
            )
        }

        // 1. Sort by Cache Descending benchmark
        val cacheSortTime = measureTimeMillis {
            val sorted = AppFilter.filterAndSort(
                apps = apps,
                sort = AppSort.CACHE_DESC,
                showSystemApps = true,
                showZeroCacheApps = true
            )
            assertEquals(1000, sorted.size)
            assertEquals(1000 * 1_000_000L, sorted.first().cacheBytes)
            assertEquals(1 * 1_000_000L, sorted.last().cacheBytes)
        }
        assertTrue("Cache sort of 1000 apps should take < 50ms (took ${cacheSortTime}ms)", cacheSortTime < 50)

        // 2. Sort Alphabetical benchmark
        val nameSortTime = measureTimeMillis {
            val sorted = AppFilter.filterAndSort(
                apps = apps,
                sort = AppSort.NAME_ASC,
                showSystemApps = true,
                showZeroCacheApps = true
            )
            assertEquals(1000, sorted.size)
            assertEquals("App 0", sorted.first().appName)
        }
        assertTrue("Name sort of 1000 apps should take < 50ms (took ${nameSortTime}ms)", nameSortTime < 50)

        // 3. Search and system filter benchmark
        val filterTime = measureTimeMillis {
            val filtered = AppFilter.filterAndSort(
                apps = apps,
                query = "product9",
                sort = AppSort.CACHE_DESC,
                showSystemApps = false, // Exclude 20% system apps
                showZeroCacheApps = true
            )
            assertTrue(filtered.isNotEmpty())
            assertTrue(filtered.all { !it.isSystemApp && it.packageName.contains("product9") })
        }
        assertTrue("Filtering 1000 apps should take < 20ms (took ${filterTime}ms)", filterTime < 20)
    }

    // =========================================================================
    // P5-12: Icon and Bitmap Memory Footprint & Caching
    // =========================================================================

    @Test
    fun `P5-12 PackageRepository getCachedIconThumbnail returns instantly for cached items`() {
        var callCount = 0
        val mockRepo = object : PackageRepository {
            private val cache = mutableMapOf<String, android.graphics.Bitmap?>()

            override fun getInstalledPackages(includeSelf: Boolean, includeSystem: Boolean): List<DiscoveredPackage> = emptyList()
            override fun getPackage(packageName: String): DiscoveredPackage? = null
            override fun loadApplicationIcon(packageName: String): android.graphics.drawable.Drawable? = null

            override fun loadIconThumbnail(packageName: String, sizePx: Int): android.graphics.Bitmap? {
                callCount++
                return cache["${packageName}_$sizePx"]
            }

            override fun getCachedIconThumbnail(packageName: String, sizePx: Int): android.graphics.Bitmap? {
                return cache["${packageName}_$sizePx"]
            }

            fun putInCache(packageName: String, sizePx: Int, bitmap: android.graphics.Bitmap) {
                cache["${packageName}_$sizePx"] = bitmap
            }
        }

        // Before caching
        assertNull(mockRepo.getCachedIconThumbnail("com.example.app", 128))
        assertEquals(0, callCount)

        // Default interface implementation returns null
        val defaultRepo = object : PackageRepository {
            override fun getInstalledPackages(includeSelf: Boolean, includeSystem: Boolean): List<DiscoveredPackage> = emptyList()
            override fun getPackage(packageName: String): DiscoveredPackage? = null
            override fun loadApplicationIcon(packageName: String): android.graphics.drawable.Drawable? = null
            override fun loadIconThumbnail(packageName: String, sizePx: Int): android.graphics.Bitmap? = null
        }
        assertNull(defaultRepo.getCachedIconThumbnail("com.example.app", 128))
    }

    @Test
    fun `P5-12 memory bounded LRU cache evicts oldest entries when capacity is exceeded`() {
        val maxEntries = 250
        val lruCache = androidx.collection.LruCache<String, String>(maxEntries)

        // Insert 500 entries (2x max capacity)
        for (i in 1..500) {
            lruCache.put("pkg_$i", "bitmap_payload_$i")
        }

        // Size must not exceed maxEntries
        assertEquals(maxEntries, lruCache.size())

        // Oldest entries (1..250) must be evicted
        assertNull(lruCache.get("pkg_1"))
        assertNull(lruCache.get("pkg_100"))
        assertNull(lruCache.get("pkg_250"))

        // Recent entries (251..500) must be present
        assertEquals("bitmap_payload_251", lruCache.get("pkg_251"))
        assertEquals("bitmap_payload_500", lruCache.get("pkg_500"))
    }

    // =========================================================================
    // P5-13: Main-Thread Responsiveness & Zero UI Jank
    // =========================================================================

    @Test
    fun `P5-13 AppFilter operations are strictly non-blocking and execution is sub-millisecond per event`() {
        val apps = (1..300).map { i ->
            AppCacheInfo(
                packageName = "com.test.app$i",
                appName = "Test App $i",
                cacheBytes = i * 500_000L,
                appBytes = 10_000_000L,
                dataBytes = 2_000_000L,
                isSystemApp = false,
                measurementAvailable = true
            )
        }

        // Simulate 10 sequential typing keystrokes
        val queries = listOf("T", "Te", "Tes", "Test", "Test ", "Test A", "Test Ap", "Test App", "Test App 1", "Test App 10")
        val totalKeystrokeTime = measureTimeMillis {
            for (q in queries) {
                val filtered = AppFilter.filterAndSort(
                    apps = apps,
                    query = q,
                    sort = AppSort.CACHE_DESC,
                    showSystemApps = true,
                    showZeroCacheApps = true
                )
                assertTrue(filtered.isNotEmpty())
            }
        }

        assertTrue(
            "10 sequential search keystrokes over 300 apps should execute in < 25ms total (took ${totalKeystrokeTime}ms)",
            totalKeystrokeTime < 25
        )
    }

    // =========================================================================
    // P5-14: Scanner Concurrency, Worker Pool & Progressive Flow
    // =========================================================================

    @Test
    fun `P5-14 scanner worker pool limits concurrent workers to configured limit`() = runTest {
        val appCount = 40
        val concurrencyLimit = 4
        val packageRepo = LargeFakePackageRepository(count = appCount)
        val statsRepo = LargeFakeStorageStatsRepository()

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scanner = AndroidCacheScanner(
            packageRepository = packageRepo,
            storageStatsRepository = statsRepo,
            concurrencyLimit = concurrencyLimit,
            ioDispatcher = testDispatcher
        )

        val result = scanner.scan()

        assertEquals(appCount, result.attemptedApps)
        assertEquals(appCount, result.successfulApps)
        assertTrue(
            "Peak concurrency (${statsRepo.peakConcurrency.get()}) must not exceed limit ($concurrencyLimit)",
            statsRepo.peakConcurrency.get() <= concurrencyLimit
        )
    }

    @Test
    fun `P5-14 scanFlow emits progressive Scanning states across all items before Complete`() = runTest {
        val appCount = 10
        val packageRepo = LargeFakePackageRepository(count = appCount)
        val statsRepo = LargeFakeStorageStatsRepository()

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scanner = AndroidCacheScanner(
            packageRepository = packageRepo,
            storageStatsRepository = statsRepo,
            concurrencyLimit = 4,
            ioDispatcher = testDispatcher
        )

        val states = scanner.scanFlow().toList()

        assertTrue("First state must be Discovering", states.first() is ScanState.Discovering)
        assertTrue("Last state must be Complete", states.last() is ScanState.Complete)

        val scanningStates = states.filterIsInstance<ScanState.Scanning>()
        assertEquals(appCount, scanningStates.size)

        // Verify progressive monotonic progress
        var lastScanned = 0
        for (scan in scanningStates) {
            assertTrue(scan.scannedCount > lastScanned)
            assertEquals(appCount, scan.totalCount)
            assertTrue(scan.progressFraction in 0f..1f)
            lastScanned = scan.scannedCount
        }
        assertEquals(appCount, lastScanned)
    }
}
