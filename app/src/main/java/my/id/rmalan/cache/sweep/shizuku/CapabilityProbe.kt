package my.id.rmalan.cache.sweep.shizuku

data class PmCapabilities(
    val supportsSelectiveCacheClear: Boolean,
    val supportsTrimCaches: Boolean
)

object CapabilityProbe {

    fun parseCapabilities(helpText: String): PmCapabilities {
        val hasTrimCaches = helpText.contains("trim-caches")
        val hasClear = helpText.contains("clear")
        val hasCacheOnly = helpText.contains("--cache-only")
        val supportsSelective = hasClear && hasCacheOnly

        return PmCapabilities(
            supportsSelectiveCacheClear = supportsSelective,
            supportsTrimCaches = hasTrimCaches
        )
    }

    fun probeRuntimeCapabilities(): PmCapabilities {
        return try {
            val process = ProcessBuilder("/system/bin/pm", "help").start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            parseCapabilities(output)
        } catch (e: Exception) {
            PmCapabilities(
                supportsSelectiveCacheClear = false,
                supportsTrimCaches = false
            )
        }
    }
}
