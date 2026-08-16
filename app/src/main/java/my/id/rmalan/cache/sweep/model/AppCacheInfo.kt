package my.id.rmalan.cache.sweep.model

data class AppCacheInfo(
    val packageName: String,
    val appName: String,
    val cacheBytes: Long,
    val appBytes: Long,
    val dataBytes: Long,
    val isSystemApp: Boolean,
    val measurementAvailable: Boolean,
    val errorMessage: String? = null
) {
    val totalBytes: Long
        get() = cacheBytes + appBytes + dataBytes

    companion object {
        fun fromPackageAndStats(
            pkg: DiscoveredPackage,
            stats: PackageStorageStats
        ): AppCacheInfo {
            return AppCacheInfo(
                packageName = pkg.packageName,
                appName = pkg.appName,
                cacheBytes = stats.cacheBytes,
                appBytes = stats.appBytes,
                dataBytes = stats.dataBytes,
                isSystemApp = pkg.isSystemApp,
                measurementAvailable = stats.measurementAvailable,
                errorMessage = stats.errorMessage
            )
        }
    }
}
