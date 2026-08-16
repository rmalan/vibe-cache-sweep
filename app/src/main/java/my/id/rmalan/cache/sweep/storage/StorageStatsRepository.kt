package my.id.rmalan.cache.sweep.storage

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.UserHandle
import android.os.storage.StorageManager
import java.util.UUID

open class StorageStatsRepository(
    private val context: Context? = null,
    private val storageStatsManagerOverride: StorageStatsManager? = null
) {
    private val storageStatsManager: StorageStatsManager? by lazy {
        storageStatsManagerOverride ?: context?.getSystemService(StorageStatsManager::class.java)
    }

    open fun query(
        packageName: String,
        storageUuid: UUID? = null,
        userHandle: UserHandle
    ): StorageStats {
        val manager = checkNotNull(storageStatsManager) { "StorageStatsManager unavailable" }
        val resolvedUuid = storageUuid ?: StorageManager.UUID_DEFAULT
        return manager.queryStatsForPackage(resolvedUuid, packageName, userHandle)
    }
}
