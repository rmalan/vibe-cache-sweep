package my.id.rmalan.cache.sweep.storage

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.UserHandle
import android.os.storage.StorageManager
import java.util.UUID

class StorageStatsRepository(
    private val context: Context
) {
    private val storageStatsManager: StorageStatsManager? by lazy {
        context.getSystemService(StorageStatsManager::class.java)
    }

    fun query(
        packageName: String,
        storageUuid: UUID = StorageManager.UUID_DEFAULT,
        userHandle: UserHandle
    ): StorageStats {
        val manager = checkNotNull(storageStatsManager) { "StorageStatsManager unavailable" }
        return manager.queryStatsForPackage(storageUuid, packageName, userHandle)
    }
}
