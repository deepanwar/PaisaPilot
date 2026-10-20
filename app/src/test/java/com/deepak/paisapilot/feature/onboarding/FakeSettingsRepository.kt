package com.deepak.paisapilot.feature.onboarding

import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.core.data.prefs.ThemeMode
import com.deepak.paisapilot.core.data.prefs.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class FakeSettingsRepository : SettingsRepository {
    var writes: Int = 0
    var throwOnWrite: Boolean = false
    var lastName: String? = null
    private val current = MutableStateFlow(UserSettings())
    override val settings: Flow<UserSettings> = current.asStateFlow()

    override suspend fun completeOnboarding(displayName: String) {
        writes += 1
        if (throwOnWrite) throw IOException("save failed")
        lastName = displayName
        current.value = current.value.copy(
            onboardingComplete = true,
            displayName = displayName,
        )
    }

    override suspend fun setTheme(theme: ThemeMode) {
        current.value = current.value.copy(theme = theme)
    }
}
