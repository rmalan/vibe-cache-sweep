package my.id.rmalan.cache.sweep.util

import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.ThemeMode

object SettingsFormatter {

    fun sortModeLabel(sort: AppSort): String {
        return when (sort) {
            AppSort.CACHE_DESC -> "Largest cache first"
            AppSort.TOTAL_DESC -> "Largest total footprint"
            AppSort.NAME_ASC -> "Alphabetical (A to Z)"
        }
    }

    fun sortModeDescription(sort: AppSort): String {
        return when (sort) {
            AppSort.CACHE_DESC -> "Prioritize apps taking up the most cache space"
            AppSort.TOTAL_DESC -> "Prioritize apps with largest total app, data, and cache footprint"
            AppSort.NAME_ASC -> "Display apps in alphabetical order by name"
        }
    }

    fun themeModeLabel(theme: ThemeMode): String {
        return when (theme) {
            ThemeMode.SYSTEM -> "System default"
            ThemeMode.LIGHT -> "Light theme"
            ThemeMode.DARK -> "Dark theme"
        }
    }

    fun themeModeDescription(theme: ThemeMode): String {
        return when (theme) {
            ThemeMode.SYSTEM -> "Follow system appearance settings"
            ThemeMode.LIGHT -> "Always use bright, clean light colors"
            ThemeMode.DARK -> "Always use battery-friendly dark colors"
        }
    }

    fun historyCountLabel(count: Int): String {
        return when (count) {
            0 -> "No cleanup records"
            1 -> "1 cleanup record saved locally"
            else -> "$count cleanup records saved locally"
        }
    }
}
