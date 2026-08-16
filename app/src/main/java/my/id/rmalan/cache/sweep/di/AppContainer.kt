package my.id.rmalan.cache.sweep.di

import android.content.Context
import my.id.rmalan.cache.sweep.cleaner.CacheCleaner
import my.id.rmalan.cache.sweep.cleaner.ShizukuCacheCleaner
import my.id.rmalan.cache.sweep.permissions.AndroidUsageAccessManager
import my.id.rmalan.cache.sweep.permissions.UsageAccessManager
import my.id.rmalan.cache.sweep.scanner.AndroidCacheScanner
import my.id.rmalan.cache.sweep.scanner.AndroidPackageRepository
import my.id.rmalan.cache.sweep.scanner.CacheScanner
import my.id.rmalan.cache.sweep.scanner.PackageRepository
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.storage.AndroidStorageStatsRepository
import my.id.rmalan.cache.sweep.storage.DeviceStorageRepository
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository

class AppContainer(
    context: Context
) {
    val usageAccessManager: UsageAccessManager =
        AndroidUsageAccessManager(context)

    val packageRepository: PackageRepository =
        AndroidPackageRepository(context)

    val storageStatsRepository: StorageStatsRepository =
        AndroidStorageStatsRepository(context)

    val deviceStorageRepository =
        DeviceStorageRepository()

    val shizukuManager =
        ShizukuManager(context)

    val cacheScanner: CacheScanner =
        AndroidCacheScanner(
            packageRepository = packageRepository,
            storageStatsRepository = storageStatsRepository
        )

    val cacheCleaner: CacheCleaner =
        ShizukuCacheCleaner(
            shizukuManager = shizukuManager,
            packageRepository = packageRepository
        )

    val cleanupCoordinator: my.id.rmalan.cache.sweep.cleaner.CleanupCoordinator =
        my.id.rmalan.cache.sweep.cleaner.CleanupCoordinator(
            cleaner = cacheCleaner,
            scanner = cacheScanner,
            storage = deviceStorageRepository,
            storageStatsRepository = storageStatsRepository,
            packageRepository = packageRepository
        )

    val safetyTestManager =
        my.id.rmalan.cache.sweep.cleaner.SafetyTestManager(context)

    val userSettingsRepository: my.id.rmalan.cache.sweep.storage.UserSettingsRepository =
        my.id.rmalan.cache.sweep.storage.DataStoreUserSettingsRepository(context)
}
