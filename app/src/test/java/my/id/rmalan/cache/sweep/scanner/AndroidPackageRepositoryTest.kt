package my.id.rmalan.cache.sweep.scanner

import android.content.pm.ApplicationInfo
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidPackageRepositoryTest {

    @Test
    fun `isSystemApplication identifies system applications correctly`() {
        val systemAppInfo = ApplicationInfo().apply {
            flags = ApplicationInfo.FLAG_SYSTEM
        }
        assertTrue(AndroidPackageRepository.isSystemApplication(systemAppInfo))

        val updatedSystemAppInfo = ApplicationInfo().apply {
            flags = ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        }
        assertTrue(AndroidPackageRepository.isSystemApplication(updatedSystemAppInfo))

        val combinedSystemAppInfo = ApplicationInfo().apply {
            flags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        }
        assertTrue(AndroidPackageRepository.isSystemApplication(combinedSystemAppInfo))
    }

    @Test
    fun `isSystemApplication identifies user applications correctly`() {
        val userAppInfo = ApplicationInfo().apply {
            flags = 0
        }
        assertFalse(AndroidPackageRepository.isSystemApplication(userAppInfo))

        val nonSystemFlagsAppInfo = ApplicationInfo().apply {
            flags = ApplicationInfo.FLAG_ALLOW_BACKUP or ApplicationInfo.FLAG_INSTALLED
        }
        assertFalse(AndroidPackageRepository.isSystemApplication(nonSystemFlagsAppInfo))
    }

    private class FakePackageRepository(
        private val selfPackageName: String = "my.id.rmalan.cache.sweep",
        private val packages: List<DiscoveredPackage> = emptyList()
    ) : PackageRepository {

        override fun getInstalledPackages(
            includeSelf: Boolean,
            includeSystem: Boolean
        ): List<DiscoveredPackage> {
            return packages.filter { pkg ->
                (includeSelf || pkg.packageName != selfPackageName) &&
                        (includeSystem || !pkg.isSystemApp)
            }
        }

        override fun getPackage(packageName: String): DiscoveredPackage? {
            return packages.find { it.packageName == packageName }
        }

        override fun loadApplicationIcon(packageName: String): android.graphics.drawable.Drawable? = null
        override fun loadIconThumbnail(packageName: String, sizePx: Int): android.graphics.Bitmap? = null
    }

    @Test
    fun `excludes self package by default`() {
        val packages = listOf(
            DiscoveredPackage("my.id.rmalan.cache.sweep", "CacheSweep", isSystemApp = false),
            DiscoveredPackage("com.example.app1", "App 1", isSystemApp = false),
            DiscoveredPackage("com.example.app2", "App 2", isSystemApp = false)
        )
        val repo = FakePackageRepository(packages = packages)

        val resultWithoutSelf = repo.getInstalledPackages(includeSelf = false)
        assertEquals(2, resultWithoutSelf.size)
        assertFalse(resultWithoutSelf.any { it.packageName == "my.id.rmalan.cache.sweep" })

        val resultWithSelf = repo.getInstalledPackages(includeSelf = true)
        assertEquals(3, resultWithSelf.size)
        assertTrue(resultWithSelf.any { it.packageName == "my.id.rmalan.cache.sweep" })
    }

    @Test
    fun `filters system applications when requested`() {
        val packages = listOf(
            DiscoveredPackage("com.example.userapp", "User App", isSystemApp = false),
            DiscoveredPackage("com.android.systemui", "System UI", isSystemApp = true),
            DiscoveredPackage("com.google.android.gms", "Play Services", isSystemApp = true)
        )
        val repo = FakePackageRepository(packages = packages)

        val userOnly = repo.getInstalledPackages(includeSystem = false)
        assertEquals(1, userOnly.size)
        assertEquals("com.example.userapp", userOnly.first().packageName)

        val allApps = repo.getInstalledPackages(includeSystem = true)
        assertEquals(3, allApps.size)
    }

    @Test
    fun `getPackage returns package if present and null if absent`() {
        val packages = listOf(
            DiscoveredPackage("com.example.app", "Example", isSystemApp = false)
        )
        val repo = FakePackageRepository(packages = packages)

        val found = repo.getPackage("com.example.app")
        assertEquals("Example", found?.appName)

        val missing = repo.getPackage("com.nonexistent.app")
        assertNull(missing)
    }
}
