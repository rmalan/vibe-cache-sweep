package my.id.rmalan.cache.sweep.scanner

import android.content.Context
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository

class AndroidCacheScanner(
    private val packageRepository: PackageRepository,
    private val storageStatsRepository: StorageStatsRepository
) : CacheScanner {

    constructor(
        context: Context,
        storageStatsRepository: StorageStatsRepository
    ) : this(
        packageRepository = AndroidPackageRepository(context),
        storageStatsRepository = storageStatsRepository
    )

    private val concurrencyLimit = 6

    override suspend fun scan(): ScanResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val installedPackages = packageRepository.getInstalledPackages(
            includeSelf = false,
            includeSystem = true
        )
        val semaphore = Semaphore(concurrencyLimit)

        val deferredResults = installedPackages.map { pkg ->
            async {
                semaphore.withPermit {
                    scanPackageInternal(pkg)
                }
            }
        }

        val results = deferredResults.map { it.await() }
        val successfulApps = results.count { it.measurementAvailable }
        val totalCache = results.filter { it.measurementAvailable }.sumOf { it.cacheBytes }
        val duration = System.currentTimeMillis() - startTime

        ScanResult(
            apps = results,
            attemptedApps = installedPackages.size,
            successfulApps = successfulApps,
            totalReportedCacheBytes = totalCache,
            durationMillis = duration
        )
    }

    override suspend fun scanPackage(packageName: String): AppCacheInfo? = withContext(Dispatchers.IO) {
        val pkg = packageRepository.getPackage(packageName) ?: return@withContext null
        scanPackageInternal(pkg)
    }

    private fun scanPackageInternal(pkg: DiscoveredPackage): AppCacheInfo {
        return try {
            val stats = storageStatsRepository.query(
                packageName = pkg.packageName,
                storageUuid = pkg.storageUuid,
                userHandle = Process.myUserHandle()
            )
            AppCacheInfo(
                packageName = pkg.packageName,
                appName = pkg.appName,
                cacheBytes = stats.cacheBytes,
                appBytes = stats.appBytes,
                dataBytes = stats.dataBytes,
                isSystemApp = pkg.isSystemApp,
                measurementAvailable = true
            )
        } catch (e: Exception) {
            AppCacheInfo(
                packageName = pkg.packageName,
                appName = pkg.appName,
                cacheBytes = 0L,
                appBytes = 0L,
                dataBytes = 0L,
                isSystemApp = pkg.isSystemApp,
                measurementAvailable = false
            )
        }
    }
}
