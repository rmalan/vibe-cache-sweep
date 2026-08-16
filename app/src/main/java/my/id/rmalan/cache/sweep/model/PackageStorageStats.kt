package my.id.rmalan.cache.sweep.model

/**
 * Represents the storage usage breakdown for an application package.
 *
 * @property cacheBytes The cache storage used in bytes.
 * @property appBytes The APK/code storage used in bytes.
 * @property dataBytes The user data storage used in bytes.
 * @property measurementAvailable Whether the storage statistics query succeeded.
 * @property errorMessage Error description if query failed.
 */
data class PackageStorageStats(
    val cacheBytes: Long = 0L,
    val appBytes: Long = 0L,
    val dataBytes: Long = 0L,
    val measurementAvailable: Boolean = true,
    val errorMessage: String? = null
) {
    /**
     * Total storage occupied by the application (cache + code + data).
     */
    val totalBytes: Long
        get() = cacheBytes + appBytes + dataBytes

    companion object {
        val ZERO = PackageStorageStats(
            cacheBytes = 0L,
            appBytes = 0L,
            dataBytes = 0L,
            measurementAvailable = true,
            errorMessage = null
        )

        fun failed(errorMessage: String? = null): PackageStorageStats = PackageStorageStats(
            cacheBytes = 0L,
            appBytes = 0L,
            dataBytes = 0L,
            measurementAvailable = false,
            errorMessage = errorMessage
        )
    }
}
