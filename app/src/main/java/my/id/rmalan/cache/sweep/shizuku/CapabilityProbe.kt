package my.id.rmalan.cache.sweep.shizuku

data class PmCapabilities(
    val supportsSelectiveCacheClear: Boolean,
    val supportsTrimCaches: Boolean
)

object CapabilityProbe {

    fun parseCapabilities(helpText: String, isRoot: Boolean = false): PmCapabilities {
        val hasTrimCaches = helpText.contains("trim-caches")
        val hasClear = helpText.contains("clear")
        val hasCacheOnly = helpText.contains("--cache-only")
        // On Android, pm clear --cache-only requires android.permission.INTERNAL_DELETE_CACHE_FILES (signature|privileged).
        // Shell UID 2000 is silently ignored by PackageManagerService; it is only executable by root (UID 0).
        val supportsSelective = hasClear && hasCacheOnly && isRoot

        return PmCapabilities(
            supportsSelectiveCacheClear = supportsSelective,
            supportsTrimCaches = hasTrimCaches
        )
    }

    fun probeRuntimeCapabilities(): PmCapabilities {
        return try {
            val isRoot = (android.os.Process.myUid() == 0)
            val process = ProcessBuilder("/system/bin/pm", "help").start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            parseCapabilities(output, isRoot = isRoot)
        } catch (e: Exception) {
            PmCapabilities(
                supportsSelectiveCacheClear = false,
                supportsTrimCaches = false
            )
        }
    }
}
