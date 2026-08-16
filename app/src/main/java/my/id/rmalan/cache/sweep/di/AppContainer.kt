package my.id.rmalan.cache.sweep.di

import android.content.Context
import my.id.rmalan.cache.sweep.cleaner.CacheCleaner
import my.id.rmalan.cache.sweep.cleaner.ShizukuCacheCleaner
import my.id.rmalan.cache.sweep.permissions.AndroidUsageAccessManager
import my.id.rmalan.cache.sweep.permissions.UsageAccessManager
import my.id.rmalan.cache.sweep.scanner.AndroidCacheScanner
import my.id.rmalan.cache.sweep.scanner.CacheScanner
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.storage.DeviceStorageRepository
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository

class AppContainer(
    context: Context
) {
    val usageAccessManager: UsageAccessManager =
        AndroidUsageAccessManager(context)

    val storageStatsRepository =
        StorageStatsRepository(context)

    val deviceStorageRepository =
        DeviceStorageRepository()

    val shizukuManager =
        ShizukuManager(context)

    val cacheScanner: CacheScanner =
        AndroidCacheScanner(
            context = context,
            storageStatsRepository = storageStatsRepository
        )

    val cacheCleaner: CacheCleaner =
        ShizukuCacheCleaner(
            shizukuManager = shizukuManager
        )
}
