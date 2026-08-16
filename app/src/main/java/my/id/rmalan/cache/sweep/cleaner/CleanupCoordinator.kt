package my.id.rmalan.cache.sweep.cleaner

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import my.id.rmalan.cache.sweep.model.CleanerBatchResult
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.CleanerError
import my.id.rmalan.cache.sweep.model.CleaningState
import my.id.rmalan.cache.sweep.model.CleanupMode
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.model.CleanupResult
import my.id.rmalan.cache.sweep.scanner.CacheScanner
import my.id.rmalan.cache.sweep.scanner.PackageRepository
import my.id.rmalan.cache.sweep.storage.DeviceStorageRepository
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository

class CleanupCoordinator(
    private val cleaner: CacheCleaner,
    private val scanner: CacheScanner? = null,
    private val storage: DeviceStorageRepository,
    private val storageStatsRepository: StorageStatsRepository,
    private val packageRepository: PackageRepository? = null,
    private val settlingDelayMillis: Long = DEFAULT_SETTLING_DELAY_MS,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    suspend fun clean(
        plan: CleanupPlan,
        userId: Int = 0,
        scannedPackageSet: Set<String>? = null,
        onProgress: (suspend (CleaningState) -> Unit)? = null
    ): CleanupResult = withContext(dispatcher) {
        val startedAt = System.currentTimeMillis()

        // 1. Validating capabilities and plan
        onProgress?.invoke(CleaningState.Validating)

        val capabilities: CleanerCapabilities = try {
            cleaner.capabilities()
        } catch (e: Exception) {
            val error = CleanerError.Unexpected(e)
            val msg = "Failed to query cleaner capabilities: ${e.localizedMessage ?: "Unknown error"}"
            onProgress?.invoke(CleaningState.Failed(error = error, message = msg))
            return@withContext CleanupResult.failed(
                startedAtMillis = startedAt,
                message = msg,
                error = error,
                mode = plan.mode
            )
        }

        if (!capabilities.shizukuAvailable) {
            val error = CleanerError.ShizukuUnavailable
            val msg = error.description
            onProgress?.invoke(CleaningState.Failed(error = error, message = msg))
            return@withContext CleanupResult.failed(
                startedAtMillis = startedAt,
                message = msg,
                error = error,
                mode = plan.mode
            )
        }

        if (!capabilities.shizukuAuthorized) {
            val error = CleanerError.PermissionDenied
            val msg = error.description
            onProgress?.invoke(CleaningState.Failed(error = error, message = msg))
            return@withContext CleanupResult.failed(
                startedAtMillis = startedAt,
                message = msg,
                error = error,
                mode = plan.mode
            )
        }

        when (plan.mode) {
            CleanupMode.SELECTIVE -> {
                if (!capabilities.supportsSelectiveCacheClear) {
                    val error = CleanerError.SelectiveUnsupported
                    val msg = error.description
                    onProgress?.invoke(CleaningState.Failed(error = error, message = msg))
                    return@withContext CleanupResult.failed(
                        startedAtMillis = startedAt,
                        message = msg,
                        error = error,
                        mode = plan.mode
                    )
                }
            }
            CleanupMode.GLOBAL_TRIM -> {
                if (!capabilities.supportsGlobalTrim) {
                    val error = CleanerError.GlobalTrimUnsupported
                    val msg = error.description
                    onProgress?.invoke(CleaningState.Failed(error = error, message = msg))
                    return@withContext CleanupResult.failed(
                        startedAtMillis = startedAt,
                        message = msg,
                        error = error,
                        mode = plan.mode
                    )
                }
            }
        }

        val planValidation = plan.validate(scannedPackageSet)
        if (planValidation.isFailure) {
            val ex = planValidation.exceptionOrNull() ?: IllegalArgumentException("Invalid cleanup plan")
            val error = CleanerError.Unexpected(ex)
            val msg = "Plan validation failed: ${ex.message}"
            onProgress?.invoke(CleaningState.Failed(error = error, message = msg))
            return@withContext CleanupResult.failed(
                startedAtMillis = startedAt,
                message = msg,
                error = error,
                mode = plan.mode
            )
        }

        // 2. Pre-clean snapshot
        onProgress?.invoke(CleaningState.SnapshotBefore)

        val storageBefore = try {
            storage.snapshot()
        } catch (e: Exception) {
            val error = CleanerError.Unexpected(e)
            val msg = "Failed to capture pre-clean physical storage: ${e.message}"
            onProgress?.invoke(CleaningState.Failed(error = error, message = msg))
            return@withContext CleanupResult.failed(
                startedAtMillis = startedAt,
                message = msg,
                error = error,
                mode = plan.mode
            )
        }
        val physicalFreeBefore = storageBefore.availableBytes

        val measuredCacheBefore = when (plan.mode) {
            CleanupMode.SELECTIVE -> {
                val measured = measureTargetPackagesCache(plan.selectedPackages)
                if (measured > 0L) measured else plan.estimatedCacheBytes
            }
            CleanupMode.GLOBAL_TRIM -> {
                plan.estimatedCacheBytes
            }
        }

        val effectivePlan = if (plan.mode == CleanupMode.GLOBAL_TRIM && (plan.desiredFreeBytes == null || plan.desiredFreeBytes == 0L)) {
            val targetBytes = GlobalTrimCalculator.calculateDesiredFreeBytes(
                availableBytes = physicalFreeBefore,
                totalBytes = storageBefore.totalBytes,
                estimatedCacheBytes = plan.estimatedCacheBytes
            )
            plan.copy(desiredFreeBytes = targetBytes)
        } else {
            plan
        }

        // 3. Perform Cleaning
        val batchResult: CleanerBatchResult = try {
            cleaner.executePlan(
                plan = effectivePlan,
                userId = userId,
                scannedPackageSet = scannedPackageSet,
                onProgress = { progress ->
                    onProgress?.invoke(
                        CleaningState.Clearing(
                            current = progress.current,
                            total = progress.total,
                            currentPackage = progress.currentPackageName,
                            currentAppName = progress.currentAppName
                        )
                    )
                }
            )
        } catch (e: Exception) {
            val error = CleanerError.Unexpected(e)
            val msg = "Cleaner execution failed: ${e.message}"
            onProgress?.invoke(CleaningState.Failed(error = error, message = msg))
            return@withContext CleanupResult.failed(
                startedAtMillis = startedAt,
                message = msg,
                error = error,
                mode = plan.mode
            )
        }

        // 4. Settling delay
        onProgress?.invoke(CleaningState.WaitingForStats)
        if (settlingDelayMillis > 0) {
            delay(settlingDelayMillis)
        }

        // 5. Post-clean snapshot
        onProgress?.invoke(CleaningState.SnapshotAfter)

        val storageAfter = try {
            storage.snapshot()
        } catch (e: Exception) {
            storageBefore
        }
        val physicalFreeAfter = storageAfter.availableBytes

        val measuredCacheAfter = when (plan.mode) {
            CleanupMode.SELECTIVE -> {
                measureTargetPackagesCache(plan.selectedPackages)
            }
            CleanupMode.GLOBAL_TRIM -> {
                val reclaimed = maxOf(0L, physicalFreeAfter - physicalFreeBefore)
                maxOf(0L, measuredCacheBefore - reclaimed)
            }
        }

        val result = CleanupResult(
            startedAtMillis = startedAt,
            physicalFreeBefore = physicalFreeBefore,
            physicalFreeAfter = physicalFreeAfter,
            cacheBefore = measuredCacheBefore,
            cacheAfter = measuredCacheAfter,
            attemptedPackages = batchResult.totalAttempted,
            successfulPackages = batchResult.successCount,
            failedPackages = batchResult.failedPackages,
            errors = batchResult.errors,
            mode = plan.mode,
            completedAtMillis = System.currentTimeMillis()
        )

        onProgress?.invoke(CleaningState.Completed(result))
        result
    }

    private fun measureTargetPackagesCache(packages: List<String>): Long {
        if (packages.isEmpty()) return 0L
        var total = 0L
        for (pkg in packages) {
            try {
                val stats = storageStatsRepository.queryStats(pkg)
                if (stats.measurementAvailable) {
                    total += stats.cacheBytes
                }
            } catch (e: Exception) {
                // Ignore individual query error, non-blocking
            }
        }
        return total
    }

    companion object {
        const val DEFAULT_SETTLING_DELAY_MS: Long = 500L
    }
}
