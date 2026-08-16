package my.id.rmalan.cache.sweep.model

data class ScanResult(
    val apps: List<AppCacheInfo>,
    val attemptedApps: Int,
    val successfulApps: Int,
    val totalReportedCacheBytes: Long,
    val durationMillis: Long
)
