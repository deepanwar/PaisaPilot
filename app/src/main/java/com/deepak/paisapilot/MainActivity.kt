package com.deepak.paisapilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.core.data.prefs.UserSettings
import com.deepak.paisapilot.navigation.AppShell
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        var settings by mutableStateOf<UserSettings?>(null)
        splashScreen.setKeepOnScreenCondition { settings == null }
        lifecycleScope.launch {
            settingsRepository.settings.collect { settings = it }
        }
        enableEdgeToEdge()
        setContent {
            val loaded = settings ?: return@setContent
            val startAtOnboarding = remember { !loaded.onboardingComplete }
            PaisaPilotTheme(darkTheme = loaded.theme.isDark(isSystemInDarkTheme())) {
                AppShell(startAtOnboarding = startAtOnboarding)
            }
        }
    }
}
