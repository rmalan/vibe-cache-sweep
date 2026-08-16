package my.id.rmalan.cache.sweep.storage

import android.os.UserHandle
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
import my.id.rmalan.cache.sweep.model.PackageStorageStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AndroidStorageStatsRepositoryTest {

    @Test
    fun queryStats_whenManagerIsNull_returnsFailedStats() {
        val repo = AndroidStorageStatsRepository(
            context = null,
            storageStatsManagerOverride = null
        )

        val result = repo.queryStats("com.example.app")

        assertFalse(result.measurementAvailable)
        assertEquals(0L, result.cacheBytes)
        assertEquals(0L, result.totalBytes)
        assertTrue(result.errorMessage?.contains("unavailable") == true)
    }

    @Test
    fun storageStatsRepository_defaultInterfaceMethod_delegatesWithDiscoveredPackage() {
        var queriedPkg: String? = null
        var queriedUuid: UUID? = null

        val customUuid = UUID.randomUUID()

        val repo = object : StorageStatsRepository {
            override fun queryStats(
                packageName: String,
                storageUuid: UUID?,
                userHandle: UserHandle?
            ): PackageStorageStats {
                queriedPkg = packageName
                queriedUuid = storageUuid
                return PackageStorageStats(
                    cacheBytes = 12_000_000L,
                    appBytes = 24_000_000L,
                    dataBytes = 36_000_000L,
                    measurementAvailable = true
                )
            }
        }

        val pkg = DiscoveredPackage(
            packageName = "com.example.test",
            appName = "Test App",
            isSystemApp = false,
            storageUuid = customUuid
        )

        val result = repo.queryStats(pkg)

        assertEquals("com.example.test", queriedPkg)
        assertEquals(customUuid, queriedUuid)

        assertTrue(result.measurementAvailable)
        assertEquals(12_000_000L, result.cacheBytes)
        assertEquals(24_000_000L, result.appBytes)
        assertEquals(36_000_000L, result.dataBytes)
        assertEquals(72_000_000L, result.totalBytes)
        assertNull(result.errorMessage)
    }

    @Test
    fun storageStatsRepository_customImplementation_handlesExceptionsGracefully() {
        val repo = object : StorageStatsRepository {
            override fun queryStats(
                packageName: String,
                storageUuid: UUID?,
                userHandle: UserHandle?
            ): PackageStorageStats {
                return when (packageName) {
                    "com.secure.app" -> PackageStorageStats.failed("Permission denied: SecurityException")
                    "com.io.error" -> PackageStorageStats.failed("I/O error querying package stats: disk error")
                    else -> PackageStorageStats.ZERO
                }
            }
        }

        val secureResult = repo.queryStats("com.secure.app")
        assertFalse(secureResult.measurementAvailable)
        assertEquals("Permission denied: SecurityException", secureResult.errorMessage)

        val ioResult = repo.queryStats("com.io.error")
        assertFalse(ioResult.measurementAvailable)
        assertEquals("I/O error querying package stats: disk error", ioResult.errorMessage)

        val normalResult = repo.queryStats("com.normal.app")
        assertTrue(normalResult.measurementAvailable)
        assertEquals(0L, normalResult.totalBytes)
    }
}
