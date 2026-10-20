package com.deepak.paisapilot

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.deepak.paisapilot.core.data.prefs.SettingsKeys
import com.deepak.paisapilot.core.data.prefs.settingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(SettingsSeedRule(onboardingComplete = false))
        .around(composeRule)

    @Test
    fun onboarding_shownOnFreshInstall() {
        composeRule.onNode(
            hasText("Track spending, together or solo") and isHeading(),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Saved only on this device; uninstall or clearing data may delete it.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Start tracking").assertIsDisplayed()
        composeRule.onNode(
            hasText("Track spending, together or solo") and isHeading(),
        ).assertIsDisplayed()
        composeRule.onNode(hasText("Start tracking") and hasClickAction())
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNode(hasText("Skip") and hasClickAction())
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun onboarding_startGoesToToday_andDoesNotRecur() {
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Sam")
        composeRule.onNodeWithText("Start tracking").performClick()
        composeRule.onNode(hasText("Today") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Track spending, together or solo").assertDoesNotExist()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNode(hasText("Today") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Track spending, together or solo").assertDoesNotExist()
        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(5_000) {
            composeRule.activityRule.scenario.state == Lifecycle.State.DESTROYED
        }
    }

    @Test
    fun onboarding_skipUsesMe() {
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNodeWithText("Skip").performClick()
        composeRule.onNode(hasText("Today") and isHeading()).assertIsDisplayed()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = runBlocking {
            settingsDataStore(context).data.first()[SettingsKeys.displayName]
        }
        assertEquals("Me", name)
    }

    @Test
    fun onboarding_tooLongNameShowsError() {
        composeRule.onNode(hasSetTextAction()).performTextReplacement("a".repeat(41))
        composeRule.onNodeWithText("Start tracking").performClick()
        composeRule.onNodeWithText("Use 40 characters or fewer.").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }
}
