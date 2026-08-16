package my.id.rmalan.cache.sweep.model

data class CleanupResult(
    val startedAtMillis: Long,
    val physicalFreeBefore: Long,
    val physicalFreeAfter: Long,
    val cacheBefore: Long,
    val cacheAfter: Long,
    val attemptedPackages: Int,
    val successfulPackages: Int,
    val failedPackages: List<String>
) {
    val measuredFreedBytes: Long
        get() = maxOf(0L, physicalFreeAfter - physicalFreeBefore)

    val reportedCacheReduction: Long
        get() = maxOf(0L, cacheBefore - cacheAfter)
}
