package my.id.rmalan.cache.sweep.scanner

import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.ScanResult

interface CacheScanner {
    suspend fun scan(): ScanResult
    suspend fun scanPackage(packageName: String): AppCacheInfo?
}
