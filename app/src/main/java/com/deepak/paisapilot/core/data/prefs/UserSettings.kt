package com.deepak.paisapilot.core.data.prefs

data class UserSettings(
    val onboardingComplete: Boolean = false,
    val displayName: String = "Me",
    val theme: ThemeMode = ThemeMode.System,
)
