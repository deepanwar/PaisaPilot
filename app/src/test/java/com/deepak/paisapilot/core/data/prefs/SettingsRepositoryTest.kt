package com.deepak.paisapilot.core.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsRepositoryTest {
    @Test
    fun defaultsSurviveANewDataStoreOnTheSameFile() {
        runBlocking {
            val file = File.createTempFile("settings", ".preferences_pb")
            val firstJob = SupervisorJob()
            val first = DataStoreSettingsRepository(
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(firstJob + Dispatchers.IO),
                    produceFile = { file },
                ),
            )
            val initial = first.settings.first()
            assertFalse(initial.onboardingComplete)
            assertEquals(ThemeMode.System, initial.theme)

            first.completeOnboarding("Sam")
            first.setTheme(ThemeMode.Dark)
            firstJob.cancel()
            firstJob.join()

            val restored = DataStoreSettingsRepository(
                PreferenceDataStoreFactory.create(produceFile = { file }),
            ).settings.first()
            assertTrue(restored.onboardingComplete)
            assertEquals("Sam", restored.displayName)
            assertEquals(ThemeMode.Dark, restored.theme)
            file.delete()
        }
    }
}
