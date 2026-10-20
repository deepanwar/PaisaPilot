package com.deepak.paisapilot

import androidx.test.platform.app.InstrumentationRegistry
import com.deepak.paisapilot.core.data.prefs.prepareSettings
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class SettingsSeedRule(
    private val onboardingComplete: Boolean,
) : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            runBlocking {
                prepareSettings(context, onboardingComplete = onboardingComplete)
            }
            base.evaluate()
        }
    }
}
