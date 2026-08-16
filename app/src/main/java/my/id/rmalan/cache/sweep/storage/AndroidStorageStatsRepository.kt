package my.id.rmalan.cache.sweep.storage

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import my.id.rmalan.cache.sweep.model.PackageStorageStats
import java.io.IOException
import java.util.UUID

/**
 * Android implementation of [StorageStatsRepository] using system [StorageStatsManager].
 */
open class AndroidStorageStatsRepository(
    private val context: Context? = null,
    private val storageStatsManagerOverride: StorageStatsManager? = null
) : StorageStatsRepository {

    private val storageStatsManager: StorageStatsManager? by lazy {
        storageStatsManagerOverride ?: context?.getSystemService(StorageStatsManager::class.java)
    }

    override fun queryStats(
        packageName: String,
        storageUuid: UUID?,
        userHandle: UserHandle?
    ): PackageStorageStats {
        val manager = storageStatsManager
            ?: return PackageStorageStats.failed("StorageStatsManager system service is unavailable")

        val resolvedUuid = storageUuid ?: StorageManager.UUID_DEFAULT
        val resolvedUser = userHandle ?: runCatching { Process.myUserHandle() }.getOrNull()

        if (resolvedUser == null) {
            return PackageStorageStats.failed("UserHandle is unavailable")
        }

        return try {
            val stats: StorageStats = manager.queryStatsForPackage(
                resolvedUuid,
                packageName,
                resolvedUser
            )
            PackageStorageStats(
                cacheBytes = stats.cacheBytes.coerceAtLeast(0L),
                appBytes = stats.appBytes.coerceAtLeast(0L),
                dataBytes = stats.dataBytes.coerceAtLeast(0L),
                measurementAvailable = true,
                errorMessage = null
            )
        } catch (e: SecurityException) {
            PackageStorageStats.failed("Permission denied: ${e.message}")
        } catch (e: IOException) {
            PackageStorageStats.failed("I/O error querying package stats: ${e.message}")
        } catch (e: IllegalArgumentException) {
            PackageStorageStats.failed("Invalid package or volume: ${e.message}")
        } catch (e: Exception) {
            PackageStorageStats.failed("Failed to query stats: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
