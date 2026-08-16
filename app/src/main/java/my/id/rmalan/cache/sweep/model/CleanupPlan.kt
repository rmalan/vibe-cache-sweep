package my.id.rmalan.cache.sweep.model

enum class CleanupMode {
    SELECTIVE,
    GLOBAL_TRIM
}

data class CleanupPlan(
    val mode: CleanupMode,
    val selectedPackages: List<String>,
    val estimatedCacheBytes: Long
)
