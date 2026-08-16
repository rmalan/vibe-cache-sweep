package my.id.rmalan.cache.sweep.scanner

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.storage.AndroidStorageStatsRepository
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository

class AndroidCacheScanner(
    private val packageRepository: PackageRepository,
    private val storageStatsRepository: StorageStatsRepository,
    private val concurrencyLimit: Int = DEFAULT_CONCURRENCY_LIMIT,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CacheScanner {

    constructor(
        context: Context,
        storageStatsRepository: StorageStatsRepository = AndroidStorageStatsRepository(context),
        concurrencyLimit: Int = DEFAULT_CONCURRENCY_LIMIT,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        packageRepository = AndroidPackageRepository(context),
        storageStatsRepository = storageStatsRepository,
        concurrencyLimit = concurrencyLimit,
        ioDispatcher = ioDispatcher
    )

    companion object {
        const val DEFAULT_CONCURRENCY_LIMIT = 6
    }

    override fun scanFlow(
        includeSelf: Boolean,
        includeSystem: Boolean
    ): Flow<ScanState> = flow {
        val startTime = System.currentTimeMillis()
        emit(ScanState.Discovering)

        val installedPackages = try {
            packageRepository.getInstalledPackages(
                includeSelf = includeSelf,
                includeSystem = includeSystem
            )
        } catch (e: Throwable) {
            emit(ScanState.Failed(e))
            return@flow
        }

        val totalCount = installedPackages.size
        if (totalCount == 0) {
            val duration = System.currentTimeMillis() - startTime
            emit(
                ScanState.Complete(
                    ScanResult(
                        apps = emptyList(),
                        attemptedApps = 0,
                        successfulApps = 0,
                        totalReportedCacheBytes = 0L,
                        durationMillis = duration
                    )
                )
            )
            return@flow
        }

        val completedChannel = Channel<AppCacheInfo>(Channel.UNLIMITED)
        val effectiveConcurrency = concurrencyLimit.coerceAtLeast(1)
        val semaphore = Semaphore(effectiveConcurrency)

        coroutineScope {
            val workerJobs = installedPackages.map { pkg ->
                launch(ioDispatcher) {
                    val appInfo = semaphore.withPermit {
                        scanPackageInternal(pkg)
                    }
                    completedChannel.send(appInfo)
                }
            }

            launch(ioDispatcher) {
                workerJobs.joinAll()
                completedChannel.close()
            }

            val scannedApps = ArrayList<AppCacheInfo>(totalCount)
            var scannedCount = 0
            var successfulCount = 0
            var runningTotalCache = 0L

            for (app in completedChannel) {
                scannedApps.add(app)
                scannedCount++
                if (app.measurementAvailable) {
                    successfulCount++
                    runningTotalCache += app.cacheBytes
                }
                emit(
                    ScanState.Scanning(
                        scannedCount = scannedCount,
                        totalCount = totalCount,
                        currentPackageName = app.packageName,
                        currentAppName = app.appName,
                        runningReportedCacheBytes = runningTotalCache,
                        latestApp = app
                    )
                )
            }

            val duration = System.currentTimeMillis() - startTime
            emit(
                ScanState.Complete(
                    ScanResult(
                        apps = scannedApps,
                        attemptedApps = totalCount,
                        successfulApps = successfulCount,
                        totalReportedCacheBytes = runningTotalCache,
                        durationMillis = duration
                    )
                )
            )
        }
    }.flowOn(ioDispatcher)

    override suspend fun scan(
        includeSelf: Boolean,
        includeSystem: Boolean
    ): ScanResult {
        var finalResult: ScanResult? = null
        scanFlow(includeSelf, includeSystem).collect { state ->
            when (state) {
                is ScanState.Complete -> finalResult = state.result
                is ScanState.Failed -> throw state.error
                else -> Unit
            }
        }
        return finalResult ?: ScanResult.EMPTY
    }

    override suspend fun scanPackage(packageName: String): AppCacheInfo? = withContext(ioDispatcher) {
        val pkg = packageRepository.getPackage(packageName) ?: return@withContext null
        scanPackageInternal(pkg)
    }

    private fun scanPackageInternal(pkg: DiscoveredPackage): AppCacheInfo {
        return try {
            val stats = storageStatsRepository.queryStats(pkg)
            AppCacheInfo.fromPackageAndStats(pkg, stats)
        } catch (e: Exception) {
            AppCacheInfo(
                packageName = pkg.packageName,
                appName = pkg.appName,
                cacheBytes = 0L,
                appBytes = 0L,
                dataBytes = 0L,
                isSystemApp = pkg.isSystemApp,
                measurementAvailable = false,
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName
            )
        }
    }
}
