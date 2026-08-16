package my.id.rmalan.cache.sweep.model

/**
 * Record of a completed cleanup operation stored locally in DataStore.
 * Conforms to TECH_SPEC Section 49.
 */
data class CleanupHistoryEntry(
    val timestampMillis: Long = System.currentTimeMillis(),
    val mode: CleanupMode = CleanupMode.SELECTIVE,
    val packagesAttempted: Int = 0,
    val packagesSucceeded: Int = 0,
    val measuredFreedBytes: Long = 0L,
    val reportedCacheReductionBytes: Long = 0L,
    val durationMillis: Long = 0L
) {
    val isCompleteSuccess: Boolean
        get() = packagesAttempted > 0 && packagesSucceeded == packagesAttempted

    val hasFailures: Boolean
        get() = packagesAttempted > packagesSucceeded
}
