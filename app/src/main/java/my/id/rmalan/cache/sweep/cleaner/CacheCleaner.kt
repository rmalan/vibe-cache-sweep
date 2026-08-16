package my.id.rmalan.cache.sweep.cleaner

import my.id.rmalan.cache.sweep.model.CleanerCapabilities

interface CacheCleaner {
    suspend fun capabilities(): CleanerCapabilities
    suspend fun clearPackage(packageName: String, userId: Int = 0): Boolean
    suspend fun clearPackages(packages: List<String>, userId: Int = 0): List<String>
    suspend fun trimGlobally(desiredFreeBytes: Long): Boolean
}
