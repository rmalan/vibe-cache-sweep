package my.id.rmalan.cache.sweep.model

data class AppCacheInfo(
    val packageName: String,
    val appName: String,
    val cacheBytes: Long,
    val appBytes: Long,
    val dataBytes: Long,
    val isSystemApp: Boolean,
    val measurementAvailable: Boolean
) {
    val totalBytes: Long
        get() = cacheBytes + appBytes + dataBytes
}
