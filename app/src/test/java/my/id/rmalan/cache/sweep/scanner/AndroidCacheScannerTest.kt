package my.id.rmalan.cache.sweep.scanner

import android.os.UserHandle
import kotlinx.coroutines.test.runTest
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
import my.id.rmalan.cache.sweep.model.PackageStorageStats
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AndroidCacheScannerTest {

    private class FakePackageRepository(
        private val packages: List<DiscoveredPackage>
    ) : PackageRepository {
        override fun getInstalledPackages(includeSelf: Boolean, includeSystem: Boolean): List<DiscoveredPackage> = packages
        override fun getPackage(packageName: String): DiscoveredPackage? = packages.find { it.packageName == packageName }
        override fun loadApplicationIcon(packageName: String): android.graphics.drawable.Drawable? = null
        override fun loadIconThumbnail(packageName: String, sizePx: Int): android.graphics.Bitmap? = null
    }

    private class FakeStorageStatsRepository(
        private val statsMap: Map<String, PackageStorageStats> = emptyMap(),
        private val failingPackages: Set<String> = emptySet()
    ) : StorageStatsRepository {
        override fun queryStats(packageName: String, storageUuid: UUID?, userHandle: UserHandle?): PackageStorageStats {
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

        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(packages),
            storageStatsRepository = repo
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
}
