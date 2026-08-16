package my.id.rmalan.cache.sweep.scanner

import kotlinx.coroutines.flow.Flow
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.model.ScanState

interface CacheScanner {

    /**
     * Performs a full scan of installed applications and returns a [ScanResult].
     */
    suspend fun scan(
        includeSelf: Boolean = false,
        includeSystem: Boolean = true
    ): ScanResult

    /**
     * Executes a scan, progressively emitting [ScanState] updates as packages are discovered and measured.
     */
    fun scanFlow(
        includeSelf: Boolean = false,
        includeSystem: Boolean = true
    ): Flow<ScanState>

    /**
     * Scans a single package by its package name. Returns null if not found.
     */
    suspend fun scanPackage(
        packageName: String
    ): AppCacheInfo?
}
