package my.id.rmalan.cache.sweep.scanner

import android.app.usage.StorageStats
import android.os.UserHandle
import android.os.storage.StorageManager
import kotlinx.coroutines.test.runTest
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
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
        private val statsMap: Map<String, StorageStats> = emptyMap(),
        private val failingPackages: Set<String> = emptySet()
    ) : StorageStatsRepository() {
        override fun query(packageName: String, storageUuid: UUID?, userHandle: UserHandle): StorageStats {
            if (failingPackages.contains(packageName)) {
                throw SecurityException("Package stats access denied for $packageName")
            }
            return statsMap[packageName] ?: throw IllegalArgumentException("Unknown package: $packageName")
        }
    }

    @Test
    fun `scan handles partial package query failures gracefully`() = runTest {
        val packages = listOf(
            DiscoveredPackage("com.example.app1", "App 1", isSystemApp = false),
            DiscoveredPackage("com.example.failing", "Failing App", isSystemApp = false),
            DiscoveredPackage("com.example.app2", "App 2", isSystemApp = false)
        )

        // Since StorageStats has no public constructor on all JVMs, we test the failing path
        val failingRepo = FakeStorageStatsRepository(
            failingPackages = setOf("com.example.app1", "com.example.failing", "com.example.app2")
        )

        val scanner = AndroidCacheScanner(
            packageRepository = FakePackageRepository(packages),
            storageStatsRepository = failingRepo
        )

        val result = scanner.scan()

        assertEquals(3, result.attemptedApps)
        assertEquals(0, result.successfulApps)
        assertEquals(0L, result.totalReportedCacheBytes)
        assertEquals(3, result.apps.size)
        assertTrue(result.durationMillis >= 0)

        val failingApp = result.apps.find { it.packageName == "com.example.failing" }
        assertNotNull(failingApp)
        assertFalse(failingApp!!.measurementAvailable)
        assertEquals("Failing App", failingApp.appName)
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
    }
}
