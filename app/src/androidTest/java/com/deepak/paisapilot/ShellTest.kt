package com.deepak.paisapilot

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShellTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(SettingsSeedRule(onboardingComplete = true))
        .around(composeRule)

    @Test
    fun shell_fourTabsWithLabels() {
        listOf("Today", "Activity", "Groups", "Insights").forEach { label ->
            composeRule.onNode(hasText(label) and hasClickAction()).assertIsDisplayed()
        }
        composeRule.onNode(hasText("Today") and hasClickAction()).assertIsSelected()
    }

    @Test
    fun shell_tabSwitching() {
        listOf("Activity", "Groups", "Insights", "Today").forEach { label ->
            composeRule.onNode(hasText(label) and hasClickAction()).performClick()
            composeRule.onNode(hasText(label) and isHeading()).assertIsDisplayed()
            composeRule.onNode(hasText(label) and hasClickAction()).assertIsSelected()
        }
    }

    @Test
    fun shell_settingsFromTodayMenu() {
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNode(hasText("Settings") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Settings") and isHeading()).assertIsDisplayed()
        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNode(hasText("Today") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun a11y_touchTargets() {
        listOf("Today", "Activity", "Groups", "Insights").forEach { label ->
            composeRule.onNode(hasText(label) and hasClickAction()).assertHeightIsAtLeast(48.dp)
        }
        composeRule.onNodeWithContentDescription("More options").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun a11y_headings() {
        listOf("Today", "Activity", "Groups", "Insights").forEach { label ->
            composeRule.onNode(hasText(label) and hasClickAction()).performClick()
            composeRule.onNode(hasText(label) and isHeading()).assertIsDisplayed()
        }
        composeRule.onNode(hasText("Today") and hasClickAction()).performClick()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNode(hasText("Settings") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Settings") and isHeading()).assertIsDisplayed()
    }
}
