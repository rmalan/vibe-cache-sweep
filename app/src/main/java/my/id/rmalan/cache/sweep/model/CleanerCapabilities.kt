package my.id.rmalan.cache.sweep.model

data class CleanerCapabilities(
    val shizukuAvailable: Boolean,
    val shizukuAuthorized: Boolean,
    val privilegedUid: Int?,
    val supportsSelectiveCacheClear: Boolean,
    val supportsGlobalTrim: Boolean
)
