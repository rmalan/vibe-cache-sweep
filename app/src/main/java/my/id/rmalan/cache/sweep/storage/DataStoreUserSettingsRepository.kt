package my.id.rmalan.cache.sweep.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.ThemeMode
import my.id.rmalan.cache.sweep.model.UserSettings
import java.io.IOException

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class DataStoreUserSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : UserSettingsRepository {

    constructor(context: Context) : this(context.userSettingsDataStore)

    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
        val KEY_SHOW_ZERO_CACHE_APPS = booleanPreferencesKey("show_zero_cache_apps")
        val KEY_SORT_MODE = stringPreferencesKey("sort_mode")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override val settings: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferences(preferences)
        }

    override suspend fun getSettings(): UserSettings {
        return settings.first()
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun setShowSystemApps(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_SYSTEM_APPS] = show
        }
    }

    override suspend fun setShowZeroCacheApps(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_ZERO_CACHE_APPS] = show
        }
    }

    override suspend fun setSortMode(sort: AppSort) {
        dataStore.edit { preferences ->
            preferences[KEY_SORT_MODE] = sort.name
        }
    }

    override suspend fun setThemeMode(theme: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = theme.name
        }
    }

    private fun mapPreferences(preferences: Preferences): UserSettings {
        val onboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED] ?: false
        val showSystemApps = preferences[KEY_SHOW_SYSTEM_APPS] ?: false
        val showZeroCacheApps = preferences[KEY_SHOW_ZERO_CACHE_APPS] ?: true
        val sortModeName = preferences[KEY_SORT_MODE]
        val sortMode = sortModeName?.let { name ->
            try {
                AppSort.valueOf(name)
            } catch (e: IllegalArgumentException) {
                AppSort.CACHE_DESC
            }
        } ?: AppSort.CACHE_DESC

        val themeModeName = preferences[KEY_THEME_MODE]
        val themeMode = themeModeName?.let { name ->
            try {
                ThemeMode.valueOf(name)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        } ?: ThemeMode.SYSTEM

        return UserSettings(
            onboardingCompleted = onboardingCompleted,
            showSystemApps = showSystemApps,
            showZeroCacheApps = showZeroCacheApps,
            sortMode = sortMode,
            themeMode = themeMode
        )
    }
}
