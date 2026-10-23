package com.deepak.paisapilot.feature.groups

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.espresso.Espresso
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.db.LedgerDatabase
import com.deepak.paisapilot.core.data.db.RoomTransactionRunner
import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.core.data.prefs.ThemeMode
import com.deepak.paisapilot.core.data.prefs.UserSettings
import com.deepak.paisapilot.core.data.repo.RoomGroupRepository
import com.deepak.paisapilot.feature.groups.form.GroupFormRoute
import com.deepak.paisapilot.feature.groups.list.GroupListRoute
import com.deepak.paisapilot.feature.groups.list.GroupListViewModel
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private class StubSettings : SettingsRepository {
    override val settings: Flow<UserSettings> = MutableStateFlow(UserSettings(displayName = "Me"))

    override suspend fun completeOnboarding(displayName: String) = Unit

    override suspend fun setTheme(theme: ThemeMode) = Unit
}

@RunWith(AndroidJUnit4::class)
class GroupsFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: LedgerDatabase
    private lateinit var repo: RoomGroupRepository
    private val counter = AtomicInteger()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java,
        ).build()
        repo = RoomGroupRepository(
            db.groupDao(),
            RoomTransactionRunner(db),
            Clock.systemDefaultZone(),
            IdGenerator { "id-${counter.incrementAndGet()}" },
        )
    }

    @After
    fun tearDown() {
        // Text input opens the IME; a leftover keyboard would cover widgets in later test classes.
        runCatching { Espresso.closeSoftKeyboard() }
        db.close()
    }

    // AC-GRP-01
    @Test
    fun emptyListShowsDisclaimerAndNewGroupCta() {
        var newGroupClicks = 0
        rule.setContent {
            PaisaPilotTheme(dynamicColor = false) {
                GroupListRoute(
                    onNewGroup = { newGroupClicks += 1 },
                    onOpenGroup = {},
                    viewModel = GroupListViewModel(repo),
                )
            }
        }
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Groups are saved on this device only", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Groups are saved on this device only", substring = true).assertIsDisplayed()
        rule.onNodeWithText("New group").performClick()
        assertEquals(1, newGroupClicks)
    }

    // AC-GRP-02
    @Test
    fun createFlowPersistsGroupAndReportsId() {
        var created: String? = null
        rule.setContent {
            PaisaPilotTheme(dynamicColor = false) {
                GroupFormRoute(
                    onClose = {},
                    onCreated = { created = it },
                    viewModel = com.deepak.paisapilot.feature.groups.form.GroupFormViewModel(
                        SavedStateHandle(),
                        repo,
                        StubSettings(),
                        IdGenerator { "new-group" },
                    ),
                )
            }
        }
        rule.onNodeWithText("Group name").performTextInput("Flat")
        rule.onNodeWithText("Member name").performTextInput("Alex")
        rule.onNodeWithText("Add").performClick()
        rule.onNodeWithText("Create group").performClick()
        rule.waitUntil(5_000) { created != null }
        assertEquals("new-group", created)
        val members = runBlocking { repo.observeMembers("new-group").first() }
        assertEquals(listOf("Me", "Alex"), members.map { it.name })
    }

    // AC-GRP-03
    @Test
    fun duplicateMemberShowsInlineError() {
        rule.setContent {
            PaisaPilotTheme(dynamicColor = false) {
                GroupFormRoute(
                    onClose = {},
                    onCreated = {},
                    viewModel = com.deepak.paisapilot.feature.groups.form.GroupFormViewModel(
                        SavedStateHandle(),
                        repo,
                        StubSettings(),
                        IdGenerator { "new-group" },
                    ),
                )
            }
        }
        rule.onNodeWithText("Member name").performTextInput("Alex")
        rule.onNodeWithText("Add").performClick()
        rule.onNodeWithText("Member name").performTextInput(" alex ")
        rule.onNodeWithText("Add").performClick()
        rule.onNodeWithText("Someone in this group already has that name.").assertIsDisplayed()
    }
}
