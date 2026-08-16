package my.id.rmalan.cache.sweep.cleaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.id.rmalan.cache.sweep.model.CleanerBatchResult
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.CleanerError
import my.id.rmalan.cache.sweep.model.CleaningProgress
import my.id.rmalan.cache.sweep.model.CleanupMode
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.scanner.PackageRepository
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.util.PackageValidator

class ShizukuCacheCleaner(
    private val shizukuManager: ShizukuManager,
    private val packageRepository: PackageRepository? = null
) : CacheCleaner {

    override suspend fun capabilities(): CleanerCapabilities = withContext(Dispatchers.IO) {
        shizukuManager.fetchCapabilities()
    }

    override suspend fun clearPackage(packageName: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!PackageValidator.isValid(packageName) || userId < 0) return@withContext false
        val service = shizukuManager.getOrAwaitService() ?: return@withContext false
        try {
            if (!service.supportsSelectiveCacheClear()) return@withContext false
            val result = service.clearPackageCache(packageName, userId)
            result == 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun clearPackages(
        packages: List<String>,
        userId: Int,
        scannedPackageSet: Set<String>?,
        onProgress: (suspend (CleaningProgress) -> Unit)?
    ): CleanerBatchResult = withContext(Dispatchers.IO) {
        if (packages.isEmpty()) {
            return@withContext CleanerBatchResult.EMPTY
        }

        val capabilities = shizukuManager.fetchCapabilities()
        if (!capabilities.shizukuAvailable) {
            val errorMap = packages.associateWith { CleanerError.ShizukuUnavailable }
            return@withContext CleanerBatchResult(
                totalAttempted = packages.size,
                successfulPackages = emptyList(),
                failedPackages = packages,
                errors = errorMap
            )
        }

        if (!capabilities.shizukuAuthorized) {
            val errorMap = packages.associateWith { CleanerError.PermissionDenied }
            return@withContext CleanerBatchResult(
                totalAttempted = packages.size,
                successfulPackages = emptyList(),
                failedPackages = packages,
                errors = errorMap
            )
        }

        if (!capabilities.supportsSelectiveCacheClear) {
            val errorMap = packages.associateWith { CleanerError.SelectiveUnsupported }
            return@withContext CleanerBatchResult(
                totalAttempted = packages.size,
                successfulPackages = emptyList(),
                failedPackages = packages,
                errors = errorMap
            )
        }

        val service = shizukuManager.getOrAwaitService()
        if (service == null) {
            val errorMap = packages.associateWith { CleanerError.ShizukuUnavailable }
            return@withContext CleanerBatchResult(
                totalAttempted = packages.size,
                successfulPackages = emptyList(),
                failedPackages = packages,
                errors = errorMap
            )
        }

        val successful = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val errors = mutableMapOf<String, CleanerError>()
        val total = packages.size

        for ((index, pkg) in packages.withIndex()) {
            val appLabel = try {
                packageRepository?.getPackage(pkg)?.appName
            } catch (e: Exception) {
                null
            }

            onProgress?.invoke(
                CleaningProgress(
                    current = index + 1,
                    total = total,
                    currentPackageName = pkg,
                    currentAppName = appLabel
                )
            )

            if (PackageValidator.isSelfPackage(pkg)) {
                failed.add(pkg)
                errors[pkg] = CleanerError.SelfCleanProhibited(pkg)
                continue
            }

            if (!PackageValidator.isValidFormat(pkg)) {
                failed.add(pkg)
                errors[pkg] = CleanerError.PackageInvalid(pkg)
                continue
            }

            if (scannedPackageSet != null && !scannedPackageSet.contains(pkg)) {
                failed.add(pkg)
                errors[pkg] = CleanerError.PackageNotScanned(pkg)
                continue
            }

            try {
                val exitCode = service.clearPackageCache(pkg, userId)
                if (exitCode == 0) {
                    successful.add(pkg)
                } else {
                    failed.add(pkg)
                    val lastError = try { service.lastError } catch (e: Exception) { "" }
                    errors[pkg] = CleanerError.CommandFailed(exitCode, lastError)
                }
            } catch (e: Exception) {
                failed.add(pkg)
                errors[pkg] = CleanerError.Unexpected(e)
            }
        }

        CleanerBatchResult(
            totalAttempted = total,
            successfulPackages = successful,
            failedPackages = failed,
            errors = errors
        )
    }

    override suspend fun trimGlobally(desiredFreeBytes: Long): Boolean = withContext(Dispatchers.IO) {
        if (desiredFreeBytes < 0L) return@withContext false
        val service = shizukuManager.getOrAwaitService() ?: return@withContext false
        try {
            if (!service.supportsGlobalTrim()) return@withContext false
            val result = service.trimCaches(desiredFreeBytes)
            result == 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun executePlan(
        plan: CleanupPlan,
        userId: Int,
        scannedPackageSet: Set<String>?,
        onProgress: (suspend (CleaningProgress) -> Unit)?
    ): CleanerBatchResult = withContext(Dispatchers.IO) {
        val validation = plan.validate(scannedPackageSet)
        if (validation.isFailure) {
            val ex = validation.exceptionOrNull() ?: IllegalArgumentException("Invalid plan")
            val targetPkgs = plan.selectedPackages.ifEmpty { listOf("cleanup_plan") }
            return@withContext CleanerBatchResult(
                totalAttempted = targetPkgs.size,
                successfulPackages = emptyList(),
                failedPackages = targetPkgs,
                errors = targetPkgs.associateWith { CleanerError.Unexpected(ex) }
            )
        }

        when (plan.mode) {
            CleanupMode.SELECTIVE -> {
                clearPackages(
                    packages = plan.selectedPackages,
                    userId = userId,
                    scannedPackageSet = scannedPackageSet,
                    onProgress = onProgress
                )
            }
            CleanupMode.GLOBAL_TRIM -> {
                val targetBytes = plan.desiredFreeBytes ?: 0L
                onProgress?.invoke(
                    CleaningProgress(
                        current = 1,
                        total = 1,
                        currentPackageName = null,
                        currentAppName = "Global Cache Trim"
                    )
                )

                val capabilities = shizukuManager.fetchCapabilities()
                if (!capabilities.supportsGlobalTrim) {
                    return@withContext CleanerBatchResult(
                        totalAttempted = 1,
                        successfulPackages = emptyList(),
                        failedPackages = listOf("global_trim"),
                        errors = mapOf("global_trim" to CleanerError.GlobalTrimUnsupported)
                    )
                }

                val success = trimGlobally(targetBytes)
                if (success) {
                    CleanerBatchResult(
                        totalAttempted = 1,
                        successfulPackages = listOf("global_trim"),
                        failedPackages = emptyList()
                    )
                } else {
                    val service = shizukuManager.getService()
                    val lastError = try { service?.lastError ?: "" } catch (e: Exception) { "" }
                    CleanerBatchResult(
                        totalAttempted = 1,
                        successfulPackages = emptyList(),
                        failedPackages = listOf("global_trim"),
                        errors = mapOf("global_trim" to CleanerError.CommandFailed(-1, lastError))
                    )
                }
            }
        }
    }
}

