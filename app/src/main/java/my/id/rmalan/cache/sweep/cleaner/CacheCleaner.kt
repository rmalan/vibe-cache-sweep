package my.id.rmalan.cache.sweep.cleaner

import my.id.rmalan.cache.sweep.model.CleanerBatchResult
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.CleaningProgress
import my.id.rmalan.cache.sweep.model.CleanupPlan

interface CacheCleaner {
    suspend fun capabilities(): CleanerCapabilities

    suspend fun clearPackage(
        packageName: String,
        userId: Int = 0
    ): Boolean

    suspend fun clearPackages(
        packages: List<String>,
        userId: Int = 0,
        scannedPackageSet: Set<String>? = null,
        onProgress: (suspend (CleaningProgress) -> Unit)? = null
    ): CleanerBatchResult

    suspend fun trimGlobally(
        desiredFreeBytes: Long
    ): Boolean

    suspend fun executePlan(
        plan: CleanupPlan,
        userId: Int = 0,
        scannedPackageSet: Set<String>? = null,
        onProgress: (suspend (CleaningProgress) -> Unit)? = null
    ): CleanerBatchResult
}

