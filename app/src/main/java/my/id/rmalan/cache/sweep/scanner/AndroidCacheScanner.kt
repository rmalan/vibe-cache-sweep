package my.id.rmalan.cache.sweep.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository

class AndroidCacheScanner(
    private val context: Context,
    private val storageStatsRepository: StorageStatsRepository
) : CacheScanner {

    private val packageManager: PackageManager = context.packageManager
    private val concurrencyLimit = 6

    override suspend fun scan(): ScanResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val semaphore = Semaphore(concurrencyLimit)

        val deferredResults = installedApps.map { appInfo ->
            async {
                semaphore.withPermit {
                    scanApplicationInfo(appInfo)
                }
            }
        }

        val results = deferredResults.map { it.await() }
        val successfulApps = results.count { it.measurementAvailable }
        val totalCache = results.filter { it.measurementAvailable }.sumOf { it.cacheBytes }
        val duration = System.currentTimeMillis() - startTime

        ScanResult(
            apps = results,
            attemptedApps = installedApps.size,
            successfulApps = successfulApps,
            totalReportedCacheBytes = totalCache,
            durationMillis = duration
        )
    }

    override suspend fun scanPackage(packageName: String): AppCacheInfo? = withContext(Dispatchers.IO) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            scanApplicationInfo(appInfo)
        } catch (e: Exception) {
            null
        }
    }

    private fun scanApplicationInfo(appInfo: ApplicationInfo): AppCacheInfo {
        val appName = try {
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            appInfo.packageName
        }

        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        return try {
            val stats = storageStatsRepository.query(
                packageName = appInfo.packageName,
                storageUuid = appInfo.storageUuid ?: StorageManager.UUID_DEFAULT,
                userHandle = Process.myUserHandle()
            )
            AppCacheInfo(
                packageName = appInfo.packageName,
                appName = appName,
                cacheBytes = stats.cacheBytes,
                appBytes = stats.appBytes,
                dataBytes = stats.dataBytes,
                isSystemApp = isSystem,
                measurementAvailable = true
            )
        } catch (e: Exception) {
            AppCacheInfo(
                packageName = appInfo.packageName,
                appName = appName,
                cacheBytes = 0L,
                appBytes = 0L,
                dataBytes = 0L,
                isSystemApp = isSystem,
                measurementAvailable = false
            )
        }
    }
}
