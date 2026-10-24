package com.deepak.paisapilot.feature.groups

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.db.LedgerDatabase
import com.deepak.paisapilot.core.data.db.RoomTransactionRunner
import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.core.data.prefs.ThemeMode
import com.deepak.paisapilot.core.data.prefs.UserSettings
import com.deepak.paisapilot.core.data.repo.RoomGroupRepository
import com.deepak.paisapilot.core.data.repo.RoomSettlementRepository
import com.deepak.paisapilot.core.data.repo.RoomSharedExpenseRepository
import com.deepak.paisapilot.domain.group.Share
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.feature.groups.balances.BalancesRoute
import com.deepak.paisapilot.feature.groups.balances.BalancesViewModel
import com.deepak.paisapilot.feature.groups.settlements.form.SettlementFormRoute
import com.deepak.paisapilot.feature.groups.settlements.form.SettlementFormViewModel
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private class SettlementStubSettings : SettingsRepository {
    override val settings: Flow<UserSettings> = MutableStateFlow(UserSettings(displayName = "Me"))

    override suspend fun completeOnboarding(displayName: String) = Unit

    override suspend fun setTheme(theme: ThemeMode) = Unit
}

@RunWith(AndroidJUnit4::class)
class SettlementFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: LedgerDatabase
    private lateinit var groups: RoomGroupRepository
    private lateinit var expenses: RoomSharedExpenseRepository
    private lateinit var settlements: RoomSettlementRepository
    private val counter = AtomicInteger()

    // createGroup makes m1 = Me, m2 = Alex. Me paid 10_000 for Alex, so Alex owes Me 10_000.
    private val me = "m1"
    private val alex = "m2"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java,
        ).build()
        val clock = Clock.systemDefaultZone()
        val transactions = RoomTransactionRunner(db)
        groups = RoomGroupRepository(db.groupDao(), transactions, clock, IdGenerator { "m${counter.incrementAndGet()}" })
        expenses = RoomSharedExpenseRepository(db.sharedExpenseDao(), db.groupDao(), transactions, clock)
        settlements = RoomSettlementRepository(db.settlementDao(), db.sharedExpenseDao(), db.groupDao(), transactions, clock)
        runBlocking {
            groups.createGroup("g1", "Flat", "Me", listOf("Alex"))
            expenses.create(
                SharedExpense(
                    id = "e1",
                    groupId = "g1",
                    amount = Paise(10_000),
                    title = "Dinner",
                    category = Category.Food,
                    date = LocalDate.now(),
                    payerMemberId = me,
                    note = null,
                    shares = listOf(Share(alex, Paise(10_000))),
                    createdAt = 0,
                    updatedAt = 0,
                ),
            )
        }
    }

    @After
    fun tearDown() {
        // Text input opens the IME; a leftover keyboard would cover widgets in later test classes.
        runCatching { Espresso.closeSoftKeyboard() }
        db.close()
    }

    private fun waitForText(text: String, substring: Boolean = false) = rule.waitUntil(5_000) {
        rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }

    // AC-BAL-01
    @Test
    fun balancesShowsSuggestionAndTappingItPrefillsTheForm() {
        var recorded: Triple<String?, String?, Long?>? = null
        rule.setContent {
            PaisaPilotTheme(dynamicColor = false) {
                BalancesRoute(
                    onClose = {},
                    onRecord = { from, to, amount -> recorded = Triple(from, to, amount) },
                    viewModel = BalancesViewModel(
                        SavedStateHandle(mapOf("groupId" to "g1")),
                        groups,
                        expenses,
                        settlements,
                        SettlementStubSettings(),
                    ),
                )
            }
        }
        waitForText("Alex pays Me (you)", substring = true)
        rule.onNodeWithText("Recording doesn't send money", substring = true).assertExists()
        rule.onNodeWithText("Alex pays Me (you)", substring = true).performClick()
        assertEquals(Triple<String?, String?, Long?>(alex, me, 10_000L), recorded)
    }

    private fun setForm(settlementId: String? = null) {
        rule.setContent {
            PaisaPilotTheme(dynamicColor = false) {
                SettlementFormRoute(
                    onClose = {},
                    viewModel = SettlementFormViewModel(
                        SavedStateHandle(
                            buildMap {
                                put("groupId", "g1")
                                if (settlementId != null) put("settlementId", settlementId)
                            },
                        ),
                        settlements,
                        expenses,
                        groups,
                        SettlementStubSettings(),
                        Clock.systemDefaultZone(),
                        IdGenerator { "new-settlement" },
                    ),
                )
            }
        }
    }

    // AC-BAL-02, AC-BAL-04
    @Test
    fun formRejectsOverAmountThenRecordsSuggestedPayment() {
        setForm()
        waitForText("100.00")
        rule.onNodeWithText("This does not transfer money", substring = true).assertExists()
        rule.onNodeWithText("100.00").performTextClearance()
        rule.onNodeWithText("Amount", substring = true).performTextInput("100.01")
        waitForText("Balance changed; review amount", substring = true)
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNodeWithText("100.01").performTextClearance()
        rule.onNodeWithText("Amount", substring = true).performTextInput("40")
        rule.onNodeWithText("Save").assertIsEnabled()
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5_000) { runBlocking { settlements.observeGroup("g1").first() }.isNotEmpty() }
        val stored = runBlocking { settlements.observe("new-settlement").first() }!!
        assertEquals(4_000L, stored.amount.value)
        assertEquals(alex, stored.fromMemberId)
        assertEquals(me, stored.toMemberId)
        assertEquals(10_000L, runBlocking { expenses.observe("e1").first() }!!.amount.value)
    }

    // AC-BAL-05
    @Test
    fun editChangesAmountOnTheSameRecord() {
        runBlocking {
            settlements.create(
                com.deepak.paisapilot.domain.group.Settlement(
                    id = "s1",
                    groupId = "g1",
                    fromMemberId = alex,
                    toMemberId = me,
                    amount = Paise(2_000),
                    occurredEpochDay = LocalDate.now().toEpochDay(),
                    note = null,
                    createdAt = 0,
                    updatedAt = 0,
                ),
            )
        }
        setForm(settlementId = "s1")
        waitForText("20.00")
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNodeWithText("20.00").performTextClearance()
        rule.onNodeWithText("Amount", substring = true).performTextInput("25")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5_000) {
            runBlocking { settlements.observe("s1").first() }?.amount?.value == 2_500L
        }
        assertEquals(1, runBlocking { settlements.observeGroup("g1").first() }.size)
        assertNull(runBlocking { settlements.observe("new-settlement").first() })
    }
}
