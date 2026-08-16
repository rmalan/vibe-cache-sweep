package my.id.rmalan.cache.sweep.storage

import android.os.Environment
import android.os.StatFs
import my.id.rmalan.cache.sweep.model.DeviceStorageInfo

class DeviceStorageRepository {
    fun snapshot(): DeviceStorageInfo {
        val statFs = StatFs(Environment.getDataDirectory().absolutePath)
        return DeviceStorageInfo(
            totalBytes = statFs.totalBytes,
            availableBytes = statFs.availableBytes
        )
    }
}
