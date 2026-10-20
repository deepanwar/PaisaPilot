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
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShellTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun shell_fourTabsWithLabels() {
        listOf("Today", "Activity", "Groups", "Insights").forEach { label ->
            rule.onNode(hasText(label) and hasClickAction()).assertIsDisplayed()
        }
        rule.onNode(hasText("Today") and hasClickAction()).assertIsSelected()
    }

    @Test
    fun shell_tabSwitching() {
        listOf("Activity", "Groups", "Insights", "Today").forEach { label ->
            rule.onNode(hasText(label) and hasClickAction()).performClick()
            rule.onNode(hasText(label) and isHeading()).assertIsDisplayed()
            rule.onNode(hasText(label) and hasClickAction()).assertIsSelected()
        }
    }

    @Test
    fun shell_settingsFromTodayMenu() {
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNode(hasText("Settings") and hasClickAction()).performClick()
        rule.onNode(hasText("Settings") and isHeading()).assertIsDisplayed()
        rule.runOnIdle {
            rule.activity.onBackPressedDispatcher.onBackPressed()
        }
        rule.onNode(hasText("Today") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun a11y_touchTargets() {
        listOf("Today", "Activity", "Groups", "Insights").forEach { label ->
            rule.onNode(hasText(label) and hasClickAction()).assertHeightIsAtLeast(48.dp)
        }
        rule.onNodeWithContentDescription("More options").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun a11y_headings() {
        listOf("Today", "Activity", "Groups", "Insights").forEach { label ->
            rule.onNode(hasText(label) and hasClickAction()).performClick()
            rule.onNode(hasText(label) and isHeading()).assertIsDisplayed()
        }
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNode(hasText("Settings") and hasClickAction()).performClick()
        rule.onNode(hasText("Settings") and isHeading()).assertIsDisplayed()
    }
}
