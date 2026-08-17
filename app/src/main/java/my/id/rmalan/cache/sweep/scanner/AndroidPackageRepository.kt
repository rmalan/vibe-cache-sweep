package my.id.rmalan.cache.sweep.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.storage.StorageManager
import androidx.collection.LruCache
import my.id.rmalan.cache.sweep.model.DiscoveredPackage

class AndroidPackageRepository(
    private val context: Context,
    private val packageManager: PackageManager = context.packageManager
) : PackageRepository {

    private val selfPackageName: String = context.packageName

    // Memory-bounded icon caches to avoid OOM when browsing large app lists
    private val iconCache = LruCache<String, Drawable>(50)
    private val thumbnailCache = LruCache<String, Bitmap>(250)

    override fun getInstalledPackages(
        includeSelf: Boolean,
        includeSystem: Boolean
    ): List<DiscoveredPackage> {
        val installedApps = try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList()
        }

        return installedApps.mapNotNull { appInfo ->
            val pkgName = appInfo.packageName ?: return@mapNotNull null

            if (!includeSelf && pkgName == selfPackageName) {
                return@mapNotNull null
            }

            val isSystem = isSystemApplication(appInfo)
            if (!includeSystem && isSystem) {
                return@mapNotNull null
            }

            val label = try {
                packageManager.getApplicationLabel(appInfo).toString().trim()
            } catch (e: Exception) {
                ""
            }

            val appName = if (label.isNotBlank()) label else pkgName
            val storageUuid = appInfo.storageUuid

            DiscoveredPackage(
                packageName = pkgName,
                appName = appName,
                isSystemApp = isSystem,
                versionName = null,
                versionCode = 0L,
                storageUuid = storageUuid
            )
        }
    }

    override fun getPackage(packageName: String): DiscoveredPackage? {
        if (packageName.isBlank()) return null
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val isSystem = isSystemApplication(appInfo)
            val label = try {
                packageManager.getApplicationLabel(appInfo).toString().trim()
            } catch (e: Exception) {
                ""
            }
            val appName = if (label.isNotBlank()) label else packageName

            val (versionName, versionCode) = try {
                val pkgInfo = packageManager.getPackageInfo(packageName, 0)
                Pair(pkgInfo.versionName, pkgInfo.longVersionCode)
            } catch (e: Exception) {
                Pair(null, 0L)
            }

            val storageUuid = appInfo.storageUuid

            DiscoveredPackage(
                packageName = packageName,
                appName = appName,
                isSystemApp = isSystem,
                versionName = versionName,
                versionCode = versionCode,
                storageUuid = storageUuid
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun loadApplicationIcon(packageName: String): Drawable? {
        if (packageName.isBlank()) return null

        iconCache.get(packageName)?.let { return it }

        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            iconCache.put(packageName, drawable)
            drawable
        } catch (e: Exception) {
            null
        }
    }

    override fun loadIconThumbnail(packageName: String, sizePx: Int): Bitmap? {
        if (packageName.isBlank() || sizePx <= 0) return null

        val cacheKey = "${packageName}_$sizePx"
        thumbnailCache.get(cacheKey)?.let { return it }

        val drawable = loadApplicationIcon(packageName) ?: return null

        return try {
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            thumbnailCache.put(cacheKey, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    override fun getCachedIconThumbnail(packageName: String, sizePx: Int): Bitmap? {
        if (packageName.isBlank() || sizePx <= 0) return null
        return thumbnailCache.get("${packageName}_$sizePx")
    }

    fun clearCache() {
        iconCache.evictAll()
        thumbnailCache.evictAll()
    }

    companion object {
        fun isSystemApplication(appInfo: ApplicationInfo): Boolean {
            val flags = appInfo.flags
            return (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        }
    }
}
