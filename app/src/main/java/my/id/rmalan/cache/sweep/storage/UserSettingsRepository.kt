package my.id.rmalan.cache.sweep.storage

import kotlinx.coroutines.flow.Flow
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.ThemeMode
import my.id.rmalan.cache.sweep.model.UserSettings

interface UserSettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun getSettings(): UserSettings

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setShowSystemApps(show: Boolean)

    suspend fun setShowZeroCacheApps(show: Boolean)

    suspend fun setSortMode(sort: AppSort)

    suspend fun setThemeMode(theme: ThemeMode)
}
