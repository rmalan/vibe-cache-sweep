package my.id.rmalan.cache.sweep.model

enum class AppSort(val label: String) {
    CACHE_DESC("Cache Size"),
    TOTAL_DESC("Total Size"),
    NAME_ASC("App Name");

    fun sort(apps: List<AppCacheInfo>): List<AppCacheInfo> {
        return when (this) {
            CACHE_DESC -> apps.sortedWith(
                compareByDescending<AppCacheInfo> { it.cacheBytes }
                    .thenByDescending { it.totalBytes }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName.ifBlank { it.packageName } }
            )
            TOTAL_DESC -> apps.sortedWith(
                compareByDescending<AppCacheInfo> { it.totalBytes }
                    .thenByDescending { it.cacheBytes }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName.ifBlank { it.packageName } }
            )
            NAME_ASC -> apps.sortedWith { a, b ->
                val nameA = a.appName.ifBlank { a.packageName }
                val nameB = b.appName.ifBlank { b.packageName }
                val cmp = String.CASE_INSENSITIVE_ORDER.compare(nameA, nameB)
                if (cmp != 0) cmp else b.cacheBytes.compareTo(a.cacheBytes)
            }
        }
    }
}
