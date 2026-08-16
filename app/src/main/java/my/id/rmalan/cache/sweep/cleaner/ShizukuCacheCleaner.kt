package my.id.rmalan.cache.sweep.cleaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.util.PackageValidator

class ShizukuCacheCleaner(
    private val shizukuManager: ShizukuManager
) : CacheCleaner {

    override suspend fun capabilities(): CleanerCapabilities = withContext(Dispatchers.IO) {
        shizukuManager.fetchCapabilities()
    }

    override suspend fun clearPackage(packageName: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!PackageValidator.isValid(packageName)) return@withContext false
        val service = shizukuManager.getOrAwaitService() ?: return@withContext false
        val result = service.clearPackageCache(packageName, userId)
        result == 0
    }

    override suspend fun clearPackages(packages: List<String>, userId: Int): List<String> = withContext(Dispatchers.IO) {
        val failed = mutableListOf<String>()
        val service = shizukuManager.getOrAwaitService()
        if (service == null) {
            return@withContext packages
        }
        for (pkg in packages) {
            if (!PackageValidator.isValid(pkg)) {
                failed.add(pkg)
                continue
            }
            val result = service.clearPackageCache(pkg, userId)
            if (result != 0) {
                failed.add(pkg)
            }
        }
        failed
    }

    override suspend fun trimGlobally(desiredFreeBytes: Long): Boolean = withContext(Dispatchers.IO) {
        val service = shizukuManager.getOrAwaitService() ?: return@withContext false
        val result = service.trimCaches(desiredFreeBytes)
        result == 0
    }
}
