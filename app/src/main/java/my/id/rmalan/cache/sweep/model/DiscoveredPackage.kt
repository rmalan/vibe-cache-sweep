package my.id.rmalan.cache.sweep.model

import java.util.UUID

data class DiscoveredPackage(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val versionName: String? = null,
    val versionCode: Long = 0L,
    val storageUuid: UUID? = null
)
