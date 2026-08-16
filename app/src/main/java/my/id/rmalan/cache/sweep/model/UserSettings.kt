package my.id.rmalan.cache.sweep.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val showSystemApps: Boolean = false,
    val showZeroCacheApps: Boolean = true,
    val sortMode: AppSort = AppSort.CACHE_DESC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
