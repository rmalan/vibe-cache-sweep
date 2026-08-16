package my.id.rmalan.cache.sweep.scanner

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.storage.AndroidStorageStatsRepository
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository

class AndroidCacheScanner(
    private val packageRepository: PackageRepository,
    private val storageStatsRepository: StorageStatsRepository
) : CacheScanner {

    constructor(
        context: Context,
        storageStatsRepository: StorageStatsRepository = AndroidStorageStatsRepository(context)
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
        val stats = storageStatsRepository.queryStats(pkg)
        return AppCacheInfo.fromPackageAndStats(pkg, stats)
    }
}
