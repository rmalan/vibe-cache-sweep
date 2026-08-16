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

    fun validate(): Result<Unit> {
        return when (mode) {
            CleanupMode.SELECTIVE -> {
                if (selectedPackages.isEmpty()) {
                    Result.failure(IllegalArgumentException("Selective cleanup plan requires at least one package"))
                } else {
                    for (pkg in selectedPackages) {
                        if (PackageValidator.isSelfPackage(pkg)) {
                            return Result.failure(IllegalArgumentException("Cannot clean CacheSweep self-package: $pkg"))
                        }
                        if (!PackageValidator.isValidFormat(pkg)) {
                            return Result.failure(IllegalArgumentException("Invalid package name format: $pkg"))
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
    }
}

