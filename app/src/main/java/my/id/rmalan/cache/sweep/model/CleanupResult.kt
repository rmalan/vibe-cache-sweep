package my.id.rmalan.cache.sweep.model

data class CleanupResult(
    val startedAtMillis: Long,
    val physicalFreeBefore: Long,
    val physicalFreeAfter: Long,
    val cacheBefore: Long,
    val cacheAfter: Long,
    val attemptedPackages: Int,
    val successfulPackages: Int,
    val failedPackages: List<String>,
    val errors: Map<String, CleanerError> = emptyMap(),
    val mode: CleanupMode = CleanupMode.SELECTIVE,
    val completedAtMillis: Long = System.currentTimeMillis()
) {
    val measuredFreedBytes: Long
        get() = maxOf(0L, physicalFreeAfter - physicalFreeBefore)

    val reportedCacheReduction: Long
        get() = maxOf(0L, cacheBefore - cacheAfter)

    val durationMillis: Long
        get() = maxOf(0L, completedAtMillis - startedAtMillis)

    val isSignificantReclaim: Boolean
        get() = measuredFreedBytes >= DEFAULT_NOISE_THRESHOLD_BYTES

    val hasFailures: Boolean
        get() = failedPackages.isNotEmpty()

    val isCompleteSuccess: Boolean
        get() = attemptedPackages > 0 && failedPackages.isEmpty()

    val isPartialSuccess: Boolean
        get() = successfulPackages > 0 && failedPackages.isNotEmpty()

    val isCompleteFailure: Boolean
        get() = attemptedPackages > 0 && successfulPackages == 0

    companion object {
        /**
         * Noise threshold for physical storage measurement changes (16 MB).
         * Per TECH_SPEC Section 37: If measured delta is below this threshold,
         * it indicates no significant change could be measured.
         */
        const val DEFAULT_NOISE_THRESHOLD_BYTES: Long = 16L * 1024L * 1024L

        val EMPTY = CleanupResult(
            startedAtMillis = 0L,
            physicalFreeBefore = 0L,
            physicalFreeAfter = 0L,
            cacheBefore = 0L,
            cacheAfter = 0L,
            attemptedPackages = 0,
            successfulPackages = 0,
            failedPackages = emptyList(),
            errors = emptyMap(),
            mode = CleanupMode.SELECTIVE,
            completedAtMillis = 0L
        )

        fun failed(
            startedAtMillis: Long = System.currentTimeMillis(),
            message: String,
            error: CleanerError? = null,
            mode: CleanupMode = CleanupMode.SELECTIVE
        ): CleanupResult {
            val now = System.currentTimeMillis()
            return CleanupResult(
                startedAtMillis = startedAtMillis,
                physicalFreeBefore = 0L,
                physicalFreeAfter = 0L,
                cacheBefore = 0L,
                cacheAfter = 0L,
                attemptedPackages = 0,
                successfulPackages = 0,
                failedPackages = emptyList(),
                errors = if (error != null) mapOf("cleanup" to error) else emptyMap(),
                mode = mode,
                completedAtMillis = now
            )
        }
    }
}
