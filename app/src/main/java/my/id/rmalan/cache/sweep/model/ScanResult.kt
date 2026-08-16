package my.id.rmalan.cache.sweep.model

data class ScanResult(
    val apps: List<AppCacheInfo>,
    val attemptedApps: Int,
    val successfulApps: Int,
    val totalReportedCacheBytes: Long,
    val durationMillis: Long
) {
    companion object {
        val EMPTY = ScanResult(
            apps = emptyList(),
            attemptedApps = 0,
            successfulApps = 0,
            totalReportedCacheBytes = 0L,
            durationMillis = 0L
        )
    }
}
