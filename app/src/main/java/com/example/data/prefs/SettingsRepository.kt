package com.example.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val alertsEnabled: Boolean = true,
    val liveSpeedNotification: Boolean = false,
    val onboardingDone: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ALERTS = booleanPreferencesKey("alerts_enabled")
        val LIVE_NOTIF = booleanPreferencesKey("live_speed_notification")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            alertsEnabled = prefs[Keys.ALERTS] ?: true,
            liveSpeedNotification = prefs[Keys.LIVE_NOTIF] ?: false,
            onboardingDone = prefs[Keys.ONBOARDING] ?: false
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setAlertsEnabled(enabled: Boolean) = dataStore.edit { it[Keys.ALERTS] = enabled }
    suspend fun setLiveSpeedNotification(enabled: Boolean) = dataStore.edit { it[Keys.LIVE_NOTIF] = enabled }
    suspend fun setOnboardingDone(done: Boolean) = dataStore.edit { it[Keys.ONBOARDING] = done }

    /** Restores everything except onboarding state (which is per-device). */
    suspend fun restore(settings: AppSettings) = dataStore.edit {
        it[Keys.THEME] = settings.themeMode.name
        it[Keys.DYNAMIC_COLOR] = settings.dynamicColor
        it[Keys.ALERTS] = settings.alertsEnabled
        it[Keys.LIVE_NOTIF] = settings.liveSpeedNotification
    }
}
