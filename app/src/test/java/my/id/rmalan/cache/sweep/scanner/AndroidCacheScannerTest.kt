package my.id.rmalan.cache.sweep.scanner

import android.os.UserHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
import my.id.rmalan.cache.sweep.model.PackageStorageStats
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidCacheScannerTest {

    private class FakePackageRepository(
        private val packages: List<DiscoveredPackage> = emptyList(),
        private val throwOnGetPackages: Throwable? = null
    ) : PackageRepository {
        override fun getInstalledPackages(includeSelf: Boolean, includeSystem: Boolean): List<DiscoveredPackage> {
            throwOnGetPackages?.let { throw it }
            return packages
        }

        override fun getPackage(packageName: String): DiscoveredPackage? = packages.find { it.packageName == packageName }
        override fun loadApplicationIcon(packageName: String): android.graphics.drawable.Drawable? = null
        override fun loadIconThumbnail(packageName: String, sizePx: Int): android.graphics.Bitmap? = null
    }

    private class FakeStorageStatsRepository(
        private val statsMap: Map<String, PackageStorageStats> = emptyMap(),
        private val failingPackages: Set<String> = emptySet(),
        private val throwingPackages: Set<String> = emptySet(),
        private val onQueryDelayMs: Long = 0L,
        private val onQuery: (() -> Unit)? = null
    ) : StorageStatsRepository {
        override fun queryStats(packageName: String, storageUuid: UUID?, userHandle: UserHandle?): PackageStorageStats {
            onQuery?.invoke()
            if (throwingPackages.contains(packageName)) {
                throw SecurityException("Direct permission failure for $packageName")
            }
            if (failingPackages.contains(packageName)) {
                return PackageStorageStats.failed("Access denied for $packageName")
            }
            return statsMap[packageName] ?: PackageStorageStats.failed("Unknown package: $packageName")
        }
    }

    @Test
    fun `scan handles successful and failed package queries accurately`() = runTest {
        val packages = listOf(
            DiscoveredPackage("com.example.app1", "App 1", isSystemApp = false),
            DiscoveredPackage("com.example.failing", "Failing App", isSystemApp = false),
            DiscoveredPackage("com.example.app2", "App 2", isSystemApp = true)
        )

        val statsMap = mapOf(
            "com.example.app1" to PackageStorageStats(cacheBytes = 10_000_000L, appBytes = 5_000_000L, dataBytes = 2_000_000L),
            "com.example.app2" to PackageStorageStats(cacheBytes = 25_000_000L, appBytes = 15_000_000L, dataBytes = 10_000_000L)
        )

        val repo = FakeStorageStatsRepository(
            statsMap = statsMap,
            failingPackages = setOf("com.example.failing")
        )

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(packages),
            storageStatsRepository = repo,
            ioDispatcher = testDispatcher
        )

        val result = scanner.scan()

        assertEquals(3, result.attemptedApps)
        assertEquals(2, result.successfulApps)
        assertEquals(35_000_000L, result.totalReportedCacheBytes)
        assertEquals(3, result.apps.size)
        assertTrue(result.durationMillis >= 0)

        val app1 = result.apps.find { it.packageName == "com.example.app1" }
        assertNotNull(app1)
        assertTrue(app1!!.measurementAvailable)
        assertEquals(10_000_000L, app1.cacheBytes)
        assertEquals(17_000_000L, app1.totalBytes)

        val failingApp = result.apps.find { it.packageName == "com.example.failing" }
        assertNotNull(failingApp)
        assertFalse(failingApp!!.measurementAvailable)
        assertEquals("Failing App", failingApp.appName)
        assertEquals("Access denied for com.example.failing", failingApp.errorMessage)
    }

    @Test
    fun `scanFlow emits Discovering, Scanning progress, and Complete states`() = runTest {
        val packages = listOf(
            DiscoveredPackage("com.example.app1", "App 1", isSystemApp = false),
            DiscoveredPackage("com.example.app2", "App 2", isSystemApp = true)
        )

        val statsMap = mapOf(
            "com.example.app1" to PackageStorageStats(cacheBytes = 15_000_000L, appBytes = 5_000_000L, dataBytes = 2_000_000L),
            "com.example.app2" to PackageStorageStats(cacheBytes = 20_000_000L, appBytes = 10_000_000L, dataBytes = 3_000_000L)
        )

        val repo = FakeStorageStatsRepository(statsMap = statsMap)
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(packages),
            storageStatsRepository = repo,
            ioDispatcher = testDispatcher
        )

        val states = scanner.scanFlow().toList()

        assertEquals(4, states.size) // Discovering, Scanning (1/2), Scanning (2/2), Complete
        assertTrue(states[0] is ScanState.Discovering)

        val scan1 = states[1] as ScanState.Scanning
        assertEquals(1, scan1.scannedCount)
        assertEquals(2, scan1.totalCount)
        assertEquals(0.5f, scan1.progressFraction, 0.001f)

        val scan2 = states[2] as ScanState.Scanning
        assertEquals(2, scan2.scannedCount)
        assertEquals(2, scan2.totalCount)
        assertEquals(1.0f, scan2.progressFraction, 0.001f)
        assertEquals(35_000_000L, scan2.runningReportedCacheBytes)

        val complete = states[3] as ScanState.Complete
        assertEquals(2, complete.result.attemptedApps)
        assertEquals(2, complete.result.successfulApps)
        assertEquals(35_000_000L, complete.result.totalReportedCacheBytes)
        assertEquals(2, complete.result.apps.size)
    }

    @Test
    fun `scanFlow with empty package list emits Discovering then Complete`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(emptyList()),
            storageStatsRepository = FakeStorageStatsRepository(),
            ioDispatcher = testDispatcher
        )

        val states = scanner.scanFlow().toList()

        assertEquals(2, states.size)
        assertTrue(states[0] is ScanState.Discovering)
        val complete = states[1] as ScanState.Complete
        assertEquals(0, complete.result.attemptedApps)
        assertEquals(0, complete.result.successfulApps)
        assertEquals(0L, complete.result.totalReportedCacheBytes)
        assertTrue(complete.result.apps.isEmpty())
    }

    @Test
    fun `scanFlow emits Failed when package discovery throws an exception`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val expectedException = IllegalStateException("PackageManager dead")
        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(throwOnGetPackages = expectedException),
            storageStatsRepository = FakeStorageStatsRepository(),
            ioDispatcher = testDispatcher
        )

        val states = scanner.scanFlow().toList()

        assertEquals(2, states.size)
        assertTrue(states[0] is ScanState.Discovering)
        val failed = states[1] as ScanState.Failed
        assertEquals(expectedException, failed.error)
        assertEquals("PackageManager dead", failed.message)
    }

    @Test
    fun `scan throws exception when scanFlow emits Failed`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val expectedException = RuntimeException("Security error")
        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(throwOnGetPackages = expectedException),
            storageStatsRepository = FakeStorageStatsRepository(),
            ioDispatcher = testDispatcher
        )

        try {
            scanner.scan()
            fail("Expected exception to be thrown")
        } catch (e: Exception) {
            assertEquals("Security error", e.message)
        }
    }

    @Test
    fun `scan catches unexpected exception thrown by StorageStatsRepository and isolates failure`() = runTest {
        val packages = listOf(
            DiscoveredPackage("com.example.throwing", "Crashing App", isSystemApp = false),
            DiscoveredPackage("com.example.good", "Good App", isSystemApp = false)
        )

        val statsMap = mapOf(
            "com.example.good" to PackageStorageStats(cacheBytes = 5_000_000L, appBytes = 1_000_000L, dataBytes = 500_000L)
        )

        val repo = FakeStorageStatsRepository(
            statsMap = statsMap,
            throwingPackages = setOf("com.example.throwing")
        )

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(packages),
            storageStatsRepository = repo,
            ioDispatcher = testDispatcher
        )

        val result = scanner.scan()

        assertEquals(2, result.attemptedApps)
        assertEquals(1, result.successfulApps)
        assertEquals(5_000_000L, result.totalReportedCacheBytes)

        val throwingApp = result.apps.find { it.packageName == "com.example.throwing" }
        assertNotNull(throwingApp)
        assertFalse(throwingApp!!.measurementAvailable)
        assertEquals(0L, throwingApp.cacheBytes)
        assertEquals("Direct permission failure for com.example.throwing", throwingApp.errorMessage)
    }

    @Test
    fun `scanPackage returns null for uninstalled package`() = runTest {
        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(emptyList()),
            storageStatsRepository = FakeStorageStatsRepository()
        )

        val result = scanner.scanPackage("com.nonexistent.app")
        assertNull(result)
    }

    @Test
    fun `scanPackage handles query failure for existing package`() = runTest {
        val packages = listOf(
            DiscoveredPackage("com.example.app", "Test App", isSystemApp = false)
        )
        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(packages),
            storageStatsRepository = FakeStorageStatsRepository(failingPackages = setOf("com.example.app"))
        )

        val result = scanner.scanPackage("com.example.app")
        assertNotNull(result)
        assertEquals("com.example.app", result?.packageName)
        assertEquals("Test App", result?.appName)
        assertFalse(result!!.measurementAvailable)
        assertEquals(0L, result.cacheBytes)
        assertEquals("Access denied for com.example.app", result.errorMessage)
    }

    @Test
    fun `bounded concurrency limits concurrent package queries`() = runTest {
        val concurrentCount = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)

        val packages = (1..20).map {
            DiscoveredPackage("com.example.app$it", "App $it", isSystemApp = false)
        }

        val concurrencyLimit = 4
        val repo = FakeStorageStatsRepository(
            statsMap = packages.associate { it.packageName to PackageStorageStats(cacheBytes = 1000L) },
            onQuery = {
                val current = concurrentCount.incrementAndGet()
                maxConcurrent.updateAndGet { maxOf(it, current) }
                Thread.sleep(5)
                concurrentCount.decrementAndGet()
            }
        )

        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(packages),
            storageStatsRepository = repo,
            concurrencyLimit = concurrencyLimit
        )

        val result = scanner.scan()

        assertEquals(20, result.attemptedApps)
        assertEquals(20, result.successfulApps)
        assertTrue("Max concurrent ($maxConcurrent) should be <= concurrency limit ($concurrencyLimit)", maxConcurrent.get() <= concurrencyLimit)
    }

    @Test
    fun `scan handles 350+ applications efficiently with accurate aggregates`() = runTest {
        val count = 350
        val packages = (1..count).map {
            DiscoveredPackage(
                packageName = "com.example.app_$it",
                appName = "Application $it",
                isSystemApp = (it % 5 == 0)
            )
        }

        val expectedCachePerApp = 1_000_000L // 1 MB
        val statsMap = packages.associate {
            it.packageName to PackageStorageStats(
                cacheBytes = expectedCachePerApp,
                appBytes = 5_000_000L,
                dataBytes = 2_000_000L
            )
        }

        val repo = FakeStorageStatsRepository(statsMap = statsMap)
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(packages),
            storageStatsRepository = repo,
            concurrencyLimit = 6,
            ioDispatcher = testDispatcher
        )

        val result = scanner.scan()

        assertEquals(count, result.attemptedApps)
        assertEquals(count, result.successfulApps)
        assertEquals(count * expectedCachePerApp, result.totalReportedCacheBytes)
        assertEquals(count, result.apps.size)
        assertTrue(result.durationMillis >= 0)
    }
}
