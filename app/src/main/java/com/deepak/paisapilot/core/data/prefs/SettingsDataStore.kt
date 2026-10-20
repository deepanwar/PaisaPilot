package com.deepak.paisapilot.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

object SettingsKeys {
    val onboardingComplete = booleanPreferencesKey("onboarding_complete")
    val displayName = stringPreferencesKey("display_name")
    val theme = stringPreferencesKey("theme")
}

private const val SETTINGS_FILE = "settings.preferences_pb"
private val lock = Any()
private var dataStore: DataStore<Preferences>? = null

fun settingsDataStore(context: Context): DataStore<Preferences> {
    dataStore?.let { return it }
    return synchronized(lock) {
        dataStore ?: PreferenceDataStoreFactory.create(
            produceFile = {
                File(context.applicationContext.filesDir, "datastore/$SETTINGS_FILE")
            },
        ).also { dataStore = it }
    }
}

suspend fun prepareSettings(
    context: Context,
    onboardingComplete: Boolean,
    displayName: String = "Me",
    theme: ThemeMode = ThemeMode.System,
) {
    settingsDataStore(context).edit { prefs ->
        prefs[SettingsKeys.onboardingComplete] = onboardingComplete
        prefs[SettingsKeys.displayName] = displayName
        prefs[SettingsKeys.theme] = theme.name
    }
}

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            onboardingComplete = prefs[SettingsKeys.onboardingComplete] ?: false,
            displayName = prefs[SettingsKeys.displayName] ?: "Me",
            theme = prefs[SettingsKeys.theme].toThemeMode(),
        )
    }

    override suspend fun completeOnboarding(displayName: String) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.onboardingComplete] = true
            prefs[SettingsKeys.displayName] = displayName
        }
    }

    override suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.theme] = theme.name
        }
    }
}

private fun String?.toThemeMode(): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.System
