package my.id.rmalan.cache.sweep.scanner

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import my.id.rmalan.cache.sweep.model.DiscoveredPackage

interface PackageRepository {

    /**
     * Enumerates installed packages on the device.
     *
     * @param includeSelf Whether to include CacheSweep's own package.
     * @param includeSystem Whether to include system packages.
     * @return List of discovered package metadata.
     */
    fun getInstalledPackages(
        includeSelf: Boolean = false,
        includeSystem: Boolean = true
    ): List<DiscoveredPackage>

    /**
     * Retrieves metadata for a single package.
     *
     * @param packageName The package name to inspect.
     * @return DiscoveredPackage metadata if found, null otherwise.
     */
    fun getPackage(packageName: String): DiscoveredPackage?

    /**
     * Loads the raw application icon for a package.
     *
     * @param packageName The target package name.
     * @return Application icon Drawable if found, null otherwise.
     */
    fun loadApplicationIcon(packageName: String): Drawable?

    /**
     * Loads a memory-efficient bitmap thumbnail of the application icon.
     *
     * @param packageName The target package name.
     * @param sizePx The target width and height in pixels (default 128px).
     * @return Scaled Bitmap thumbnail if found, null otherwise.
     */
    fun loadIconThumbnail(packageName: String, sizePx: Int = 128): Bitmap?

    /**
     * Retrieves an already-cached bitmap thumbnail from memory without blocking or disk I/O.
     *
     * @param packageName The target package name.
     * @param sizePx The target width and height in pixels.
     * @return Cached Bitmap if already in memory, null otherwise.
     */
    fun getCachedIconThumbnail(packageName: String, sizePx: Int = 128): Bitmap? = null
}
