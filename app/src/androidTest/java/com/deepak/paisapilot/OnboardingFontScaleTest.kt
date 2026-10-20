package com.deepak.paisapilot

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepak.paisapilot.feature.onboarding.OnboardingScreen
import com.deepak.paisapilot.feature.onboarding.OnboardingUiState
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingFontScaleTest {
    @get:Rule
    @Suppress("DEPRECATION")
    val composeRule = createComposeRule()

    @Test
    fun onboarding_largeFontCanReachStart() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                PaisaPilotTheme {
                    OnboardingScreen(
                        state = OnboardingUiState(),
                        onNameChange = {},
                        onStart = {},
                        onSkip = {},
                        onRetry = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("Start tracking").performScrollTo().assertIsDisplayed()
    }
}
