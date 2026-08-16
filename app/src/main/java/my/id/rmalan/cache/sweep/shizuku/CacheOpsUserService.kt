package my.id.rmalan.cache.sweep.shizuku

import android.os.Process
import my.id.rmalan.cache.sweep.cleaner.PackageCommands
import my.id.rmalan.cache.sweep.util.PackageValidator

class CacheOpsUserService : ICacheOpsService.Stub() {

    @Volatile
    private var lastError: String = ""

    private val cachedCapabilities: PmCapabilities by lazy {
        CapabilityProbe.probeRuntimeCapabilities()
    }

    override fun getProtocolVersion(): Int = 1

    override fun getPrivilegedUid(): Int = Process.myUid()

    override fun supportsSelectiveCacheClear(): Boolean = cachedCapabilities.supportsSelectiveCacheClear

    override fun supportsGlobalTrim(): Boolean = cachedCapabilities.supportsTrimCaches

    override fun clearPackageCache(packageName: String, userId: Int): Int {
        return try {
            require(PackageValidator.isValid(packageName)) { "Invalid package name" }
            require(userId >= 0) { "Invalid user ID" }

            val args = PackageCommands.buildClearCacheArgs(packageName, userId)
            val result = PackageCommands.execute(args)
            if (result.exitCode != 0) {
                lastError = result.error.ifBlank { result.output }
            }
            result.exitCode
        } catch (e: Exception) {
            lastError = e.message ?: "Unknown error"
            -1
        }
    }

    override fun trimCaches(desiredFreeBytes: Long): Int {
        return try {
            require(desiredFreeBytes >= 0) { "Invalid desired free bytes" }

            val args = PackageCommands.buildTrimCachesArgs(desiredFreeBytes)
            val result = PackageCommands.execute(args)
            if (result.exitCode != 0) {
                lastError = result.error.ifBlank { result.output }
            }
            result.exitCode
        } catch (e: Exception) {
            lastError = e.message ?: "Unknown error"
            -1
        }
    }

    override fun getLastError(): String = lastError
}
