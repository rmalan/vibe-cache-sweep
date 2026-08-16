package my.id.rmalan.cache.sweep.model

sealed interface ScanState {
    data object Idle : ScanState

    data object Discovering : ScanState

    data class Scanning(
        val scannedCount: Int,
        val totalCount: Int,
        val currentPackageName: String? = null,
        val currentAppName: String? = null,
        val runningReportedCacheBytes: Long = 0L,
        val latestApp: AppCacheInfo? = null
    ) : ScanState {
        val progressFraction: Float
            get() = if (totalCount > 0) (scannedCount.toFloat() / totalCount).coerceIn(0f, 1f) else 0f
    }

    data class Complete(
        val result: ScanResult
    ) : ScanState

    data class Failed(
        val error: Throwable,
        val message: String = error.localizedMessage ?: "Unknown scan error"
    ) : ScanState
}
