package com.deepak.paisapilot.core.data.prefs

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun completeOnboarding(displayName: String)

    suspend fun setTheme(theme: ThemeMode)
}
