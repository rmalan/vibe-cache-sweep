package my.id.rmalan.cache.sweep.model

data class DeviceStorageInfo(
    val totalBytes: Long,
    val availableBytes: Long
) {
    val usedBytes: Long
        get() = totalBytes - availableBytes
}
