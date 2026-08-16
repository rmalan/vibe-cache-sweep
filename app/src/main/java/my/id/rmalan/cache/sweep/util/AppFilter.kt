package my.id.rmalan.cache.sweep.util

import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.AppSort

object AppFilter {

    /**
     * Filters and sorts a list of [AppCacheInfo] based on search query, sort order, and visibility toggles.
     *
     * @param apps The list of apps to filter and sort.
     * @param query Search query to match against appName or packageName (case-insensitive).
     * @param sort Sort ordering to apply ([AppSort.CACHE_DESC], [AppSort.TOTAL_DESC], or [AppSort.NAME_ASC]).
     * @param showSystemApps Whether to include system applications.
     * @param showZeroCacheApps Whether to include applications reporting 0 bytes of cache.
     * @return The filtered and sorted list of applications.
     */
    fun filterAndSort(
        apps: List<AppCacheInfo>,
        query: String = "",
        sort: AppSort = AppSort.CACHE_DESC,
        showSystemApps: Boolean = true,
        showZeroCacheApps: Boolean = true
    ): List<AppCacheInfo> {
        val trimmedQuery = query.trim()
        val filtered = apps.filter { app ->
            if (!showSystemApps && app.isSystemApp) {
                return@filter false
            }
            if (!showZeroCacheApps && app.cacheBytes <= 0L) {
                return@filter false
            }
            if (trimmedQuery.isNotEmpty()) {
                val matchesName = app.appName.contains(trimmedQuery, ignoreCase = true)
                val matchesPackage = app.packageName.contains(trimmedQuery, ignoreCase = true)
                if (!matchesName && !matchesPackage) {
                    return@filter false
                }
            }
            true
        }
        return sort.sort(filtered)
    }
}
