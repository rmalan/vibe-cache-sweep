package my.id.rmalan.cache.sweep.model

import my.id.rmalan.cache.sweep.util.PackageValidator

enum class CleanupMode {
    SELECTIVE,
    GLOBAL_TRIM
}

data class CleanupPlan(
    val mode: CleanupMode,
    val selectedPackages: List<String> = emptyList(),
    val estimatedCacheBytes: Long = 0L,
    val desiredFreeBytes: Long? = null
) {
    init {
        require(estimatedCacheBytes >= 0L) {
            "estimatedCacheBytes must be non-negative, got $estimatedCacheBytes"
        }
        if (desiredFreeBytes != null) {
            require(desiredFreeBytes >= 0L) {
                "desiredFreeBytes must be non-negative, got $desiredFreeBytes"
            }
        }
    }

    val packageCount: Int
        get() = selectedPackages.size

    val isSelective: Boolean
        get() = mode == CleanupMode.SELECTIVE

    val isGlobalTrim: Boolean
        get() = mode == CleanupMode.GLOBAL_TRIM

    fun canFallbackToGlobalTrim(capabilities: CleanerCapabilities): Boolean {
        return isSelective && !capabilities.canCleanSelective && capabilities.canCleanGlobal
    }

    fun toGlobalTrimFallback(
        deviceStorage: DeviceStorageInfo,
        userConsentConfirmed: Boolean
    ): CleanupPlan {
        require(userConsentConfirmed) {
            "Explicit user consent is strictly required to convert a selective cleanup plan to global trim fallback."
        }
        return globalTrim(
            deviceStorage = deviceStorage,
            estimatedCacheBytes = this.estimatedCacheBytes
        )
    }

    fun validate(scannedPackages: Set<String>? = null): Result<Unit> {
        return when (mode) {
            CleanupMode.SELECTIVE -> {
                if (selectedPackages.isEmpty()) {
                    Result.failure(IllegalArgumentException("Selective cleanup plan requires at least one package"))
                } else {
                    for (pkg in selectedPackages) {
                        val validation = PackageValidator.validatePackage(pkg, scannedPackages)
                        if (validation.isFailure) {
                            return Result.failure(validation.exceptionOrNull() ?: IllegalArgumentException("Invalid package: $pkg"))
                        }
                    }
                    Result.success(Unit)
                }
            }
            CleanupMode.GLOBAL_TRIM -> {
                if (desiredFreeBytes != null && desiredFreeBytes <= 0L) {
                    Result.failure(IllegalArgumentException("Global trim target bytes must be positive"))
                } else {
                    Result.success(Unit)
                }
            }
        }
    }

    companion object {
        fun selective(
            packages: List<String>,
            estimatedCacheBytes: Long = 0L
        ): CleanupPlan {
            val sanitized = packages.distinct()
            return CleanupPlan(
                mode = CleanupMode.SELECTIVE,
                selectedPackages = sanitized,
                estimatedCacheBytes = maxOf(0L, estimatedCacheBytes),
                desiredFreeBytes = null
            )
        }

        fun selectiveSingle(
            packageName: String,
            estimatedCacheBytes: Long = 0L
        ): CleanupPlan {
            return selective(
                packages = listOf(packageName),
                estimatedCacheBytes = estimatedCacheBytes
            )
        }

        fun fromApps(
            apps: List<AppCacheInfo>
        ): CleanupPlan {
            val validPkgs = apps
                .map { it.packageName }
                .filter { PackageValidator.isValid(it) }
                .distinct()
            val totalCache = apps
                .filter { validPkgs.contains(it.packageName) }
                .sumOf { it.cacheBytes }

            return selective(
                packages = validPkgs,
                estimatedCacheBytes = totalCache
            )
        }

        fun globalTrim(
            desiredFreeBytes: Long,
            estimatedCacheBytes: Long = 0L
        ): CleanupPlan {
            return CleanupPlan(
                mode = CleanupMode.GLOBAL_TRIM,
                selectedPackages = emptyList(),
                estimatedCacheBytes = maxOf(0L, estimatedCacheBytes),
                desiredFreeBytes = maxOf(0L, desiredFreeBytes)
            )
        }

        fun globalTrim(
            deviceStorage: DeviceStorageInfo,
            estimatedCacheBytes: Long = 0L
        ): CleanupPlan {
            val target = my.id.rmalan.cache.sweep.cleaner.GlobalTrimCalculator.calculateDesiredFreeBytes(
                deviceStorage = deviceStorage,
                estimatedCacheBytes = estimatedCacheBytes
            )
            return globalTrim(
                desiredFreeBytes = target,
                estimatedCacheBytes = estimatedCacheBytes
            )
        }

        fun maxGlobalTrim(
            deviceStorage: DeviceStorageInfo
        ): CleanupPlan {
            val target = my.id.rmalan.cache.sweep.cleaner.GlobalTrimCalculator.calculateMaxFreeBytes(deviceStorage)
            return globalTrim(
                desiredFreeBytes = target,
                estimatedCacheBytes = 0L
            )
        }
    }
}

