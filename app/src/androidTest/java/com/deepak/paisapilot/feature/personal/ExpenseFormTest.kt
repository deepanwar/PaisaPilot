package com.deepak.paisapilot.feature.personal

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.db.LedgerDatabase
import com.deepak.paisapilot.core.data.repo.RoomPersonalExpenseRepository
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.domain.time.monthContaining
import com.deepak.paisapilot.feature.personal.form.ExpenseFormRoute
import com.deepak.paisapilot.feature.personal.form.ExpenseFormViewModel
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseFormTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: LedgerDatabase
    private lateinit var repo: RoomPersonalExpenseRepository
    private var closed = 0

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java,
        ).build()
        repo = RoomPersonalExpenseRepository(db.personalExpenseDao(), Clock.systemDefaultZone())
    }

    @After
    fun tearDown() = db.close()

    private fun launch(expenseId: String? = null, fontScale: Float = 1f) {
        val handle = if (expenseId == null) {
            SavedStateHandle()
        } else {
            SavedStateHandle(mapOf("expenseId" to expenseId))
        }
        val vm = ExpenseFormViewModel(handle, repo, Clock.systemDefaultZone(), IdGenerator { "new-id" })
        rule.setContent {
            PaisaPilotTheme(dynamicColor = false) {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale),
                ) {
                    ExpenseFormRoute(onClose = { closed += 1 }, viewModel = vm)
                }
            }
        }
    }

    private fun amountField() = rule.onNode(hasText("Amount in rupees") and hasSetTextAction())

    private fun rows() = runBlocking {
        repo.observeMonth(monthContaining(LocalDate.now())).first()
    }

    @Test
    fun addFlowStoresOneExpenseForToday() {
        launch()
        amountField().performTextInput("125.50")
        rule.onNodeWithText("Save").assertIsEnabled().performClick()
        rule.waitUntil(5_000) { closed == 1 }
        val row = rows().single()
        assertEquals(Paise(12_550L), row.amount)
        assertEquals(Category.Other, row.category)
        assertEquals(LocalDate.now(), row.date)
    }

    @Test
    fun invalidAmountShowsInlineErrorAndDisablesSave() {
        launch()
        amountField().performTextInput("12.345")
        rule.onNodeWithText("Use at most two decimal places.").assertIsDisplayed()
        rule.onNodeWithText("Save").assertIsNotEnabled()
        amountField().performTextReplacement("0")
        rule.onNodeWithText("Amount must be more than zero.").assertIsDisplayed()
        amountField().performTextReplacement("abc")
        rule.onNodeWithText("Use digits with an optional decimal point.").assertIsDisplayed()
        assertTrue(rows().isEmpty())
    }

    @Test
    fun pastedGroupedAmountSaves123450() {
        launch()
        amountField().performTextInput("1,234.50")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5_000) { closed == 1 }
        assertEquals(Paise(123_450L), rows().single().amount)
    }

    @Test
    fun dirtyBackAsksToDiscardAndKeepEditingRetainsContent() {
        launch()
        amountField().performTextInput("77")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            rule.activity.onBackPressedDispatcher.onBackPressed()
        }
        rule.onNodeWithText("Discard changes?").assertIsDisplayed()
        rule.onNode(hasText("Cancel") and hasClickAction()).performClick()
        rule.onNodeWithText("Discard changes?").assertDoesNotExist()
        rule.onNodeWithText("77").assertIsDisplayed()
        assertEquals(0, closed)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            rule.activity.onBackPressedDispatcher.onBackPressed()
        }
        rule.onNode(hasText("Discard") and hasClickAction()).performClick()
        assertEquals(1, closed)
        assertTrue(rows().isEmpty())
    }

    @Test
    fun cleanBackClosesWithoutPrompt() {
        launch()
        rule.onNode(hasClickAction() and androidx.compose.ui.test.hasContentDescription("Back")).performClick()
        rule.onNodeWithText("Discard changes?").assertDoesNotExist()
        assertEquals(1, closed)
    }

    @Test
    fun imeDoneDoesNotSave() {
        launch()
        amountField().performTextInput("50")
        amountField().performImeAction()
        rule.waitForIdle()
        assertEquals(0, closed)
        assertTrue(rows().isEmpty())
    }

    @Test
    fun editKeepsIdAndChangesAmount() {
        runBlocking {
            repo.create(
                PersonalExpense("e1", Paise(12_550L), "Tea", Category.Food, LocalDate.now(), null, 0, 0),
            )
        }
        launch(expenseId = "e1")
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText("Edit expense")).fetchSemanticsNodes().isNotEmpty()
        }
        amountField().performTextReplacement("225.50")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5_000) { closed == 1 }
        val row = rows().single()
        assertEquals("e1", row.id)
        assertEquals(Paise(22_550L), row.amount)
    }

    @Test
    fun missingRecordShowsUnavailable() {
        launch(expenseId = "ghost")
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText("Expense unavailable")).fetchSemanticsNodes().isNotEmpty()
        }
        assertFalse(rows().isNotEmpty())
    }

    @Test
    fun touchTargetsAreAtLeast48dp() {
        launch()
        rule.onNode(hasText("Food") and hasClickAction()).assertHeightIsAtLeast(48.dp)
        rule.onNode(hasText("Add a note") and hasClickAction()).assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("Save").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun saveReachableAtLargeFont() {
        launch(fontScale = 2f)
        amountField().performTextInput("9")
        rule.onNodeWithText("Save").assertIsDisplayed().assertIsEnabled()
    }
}
