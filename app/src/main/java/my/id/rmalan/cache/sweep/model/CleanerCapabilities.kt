package my.id.rmalan.cache.sweep.model

data class CleanerCapabilities(
    val shizukuAvailable: Boolean,
    val shizukuAuthorized: Boolean,
    val privilegedUid: Int?,
    val supportsSelectiveCacheClear: Boolean,
    val supportsGlobalTrim: Boolean
) {
    val isReady: Boolean
        get() = shizukuAvailable && shizukuAuthorized && privilegedUid != null

    val canCleanSelective: Boolean
        get() = isReady && supportsSelectiveCacheClear

    val canCleanGlobal: Boolean
        get() = isReady && supportsGlobalTrim

    val canCleanAny: Boolean
        get() = canCleanSelective || canCleanGlobal

    companion object {
        val UNAVAILABLE = CleanerCapabilities(
            shizukuAvailable = false,
            shizukuAuthorized = false,
            privilegedUid = null,
            supportsSelectiveCacheClear = false,
            supportsGlobalTrim = false
        )
    }
}

