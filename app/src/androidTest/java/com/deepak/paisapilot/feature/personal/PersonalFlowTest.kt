package com.deepak.paisapilot.feature.personal

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.db.LedgerDatabase
import com.deepak.paisapilot.core.data.db.entity.ExpenseGroupEntity
import com.deepak.paisapilot.core.data.db.entity.ExpenseShareEntity
import com.deepak.paisapilot.core.data.db.entity.MemberEntity
import com.deepak.paisapilot.core.data.db.entity.SharedExpenseEntity
import com.deepak.paisapilot.core.data.repo.RoomPersonalExpenseRepository
import com.deepak.paisapilot.core.undo.UndoDeleteCoordinator
import com.deepak.paisapilot.core.undo.UndoSnackbarEffect
import com.deepak.paisapilot.core.undo.UndoViewModel
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.domain.time.monthContaining
import com.deepak.paisapilot.feature.personal.detail.ExpenseDetailRoute
import com.deepak.paisapilot.feature.personal.detail.ExpenseDetailViewModel
import com.deepak.paisapilot.feature.personal.form.ExpenseFormRoute
import com.deepak.paisapilot.feature.personal.form.ExpenseFormViewModel
import com.deepak.paisapilot.feature.personal.today.TodayRoute
import com.deepak.paisapilot.feature.personal.today.TodayViewModel
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private sealed interface Screen {
    data object Today : Screen
    data object Add : Screen
    data class Detail(val id: String) : Screen
    data class Edit(val id: String) : Screen
}

@RunWith(AndroidJUnit4::class)
class PersonalFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: LedgerDatabase
    private lateinit var repo: RoomPersonalExpenseRepository
    private val coordinator = UndoDeleteCoordinator()
    private val clock = Clock.systemDefaultZone()
    private val today: LocalDate = LocalDate.now()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java,
        ).build()
        repo = RoomPersonalExpenseRepository(db.personalExpenseDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    private fun seed(id: String, paise: Long, title: String?) = runBlocking {
        repo.create(PersonalExpense(id, Paise(paise), title, Category.Food, today, null, 0, 0))
    }

    private fun launch(fontScale: Float = 1f) {
        rule.setContent {
            PaisaPilotTheme(dynamicColor = false) {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale),
                ) { Host() }
            }
        }
    }

    @Composable
    private fun Host() {
        var screen by remember { mutableStateOf<Screen>(Screen.Today) }
        val snackbar = remember { SnackbarHostState() }
        val todayVm = remember { TodayViewModel(SavedStateHandle(), repo, clock) }
        val undoVm = remember { UndoViewModel(coordinator, repo) }
        UndoSnackbarEffect(snackbar, undoVm)
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Box(Modifier.padding(padding)) {
                key(screen) {
                    when (val s = screen) {
                        Screen.Today -> TodayRoute(
                            onAdd = { screen = Screen.Add },
                            onOpenExpense = { screen = Screen.Detail(it) },
                            onViewAll = {},
                            viewModel = todayVm,
                        )
                        Screen.Add -> ExpenseFormRoute(
                            onClose = { screen = Screen.Today },
                            viewModel = ExpenseFormViewModel(
                                SavedStateHandle(), repo, clock, IdGenerator { "added-id" },
                            ),
                        )
                        is Screen.Edit -> ExpenseFormRoute(
                            onClose = { screen = Screen.Detail(s.id) },
                            viewModel = ExpenseFormViewModel(
                                SavedStateHandle(mapOf("expenseId" to s.id)), repo, clock,
                                IdGenerator { "unused" },
                            ),
                        )
                        is Screen.Detail -> ExpenseDetailRoute(
                            onClose = { screen = Screen.Today },
                            onEdit = { screen = Screen.Edit(it) },
                            viewModel = ExpenseDetailViewModel(
                                SavedStateHandle(mapOf("expenseId" to s.id)), repo, coordinator,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun heroShows(amount: String) {
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasContentDescription("Personal spending this month, $amount"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun month() = monthContaining(today)

    @Test
    fun todayShowsPersonalTotalAndRowsOnly_noGroupLeakage() {
        seed("p1", 10_000, "Tea")
        seed("p2", 2_550, "Snack")
        runBlocking {
            db.schemaDao().insert(ExpenseGroupEntity("g1", "Trip", 1L, null))
            db.schemaDao().insert(MemberEntity("m1", "g1", "Me", "me", true, true))
            db.schemaDao().insert(
                SharedExpenseEntity("s1", "g1", 99_900, "Hotel", "food", today.toEpochDay(), "m1", null, 1L, 1L),
            )
            db.schemaDao().insert(ExpenseShareEntity("s1", "m1", 99_900))
        }
        launch()
        heroShows("₹125.50")
        rule.onNodeWithText("Tea").assertIsDisplayed()
        rule.onNodeWithText("Snack").assertIsDisplayed()
        rule.onNodeWithText("Hotel").assertDoesNotExist()
    }

    @Test
    fun emptyTodayOffersFirstExpenseCta() {
        launch()
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText("Add your first expense")).fetchSemanticsNodes().isNotEmpty()
        }
        heroShows("₹0.00")
    }

    @Test
    fun addExpenseAppearsOnToday() {
        launch()
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText("Add expense") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodes(hasText("Add expense") and hasClickAction())[0].performClick()
        rule.onNode(hasText("Amount in rupees") and hasSetTextAction()).performTextInput("125.50")
        rule.onNodeWithText("Save").performClick()
        heroShows("₹125.50")
        assertEquals(1, runBlocking { repo.observeMonth(month()).first() }.size)
    }

    @Test
    fun editFromDetailChangesTotalByExactly100() {
        seed("e1", 12_550, "Tea")
        launch()
        heroShows("₹125.50")
        rule.onNodeWithText("Tea").performClick()
        rule.onNodeWithContentDescription("Edit expense").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText("Amount in rupees") and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNode(hasText("Amount in rupees") and hasSetTextAction()).performTextReplacement("225.50")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText("Expense")).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Back").performClick()
        heroShows("₹225.50")
        assertEquals(1, runBlocking { repo.observeMonth(month()).first() }.size)
    }

    @Test
    fun deleteThenUndoRestoresSameRecord() {
        seed("e1", 12_550, "Tea")
        val before = runBlocking { repo.observe("e1").first() }!!
        launch()
        heroShows("₹125.50")
        rule.onNodeWithText("Tea").performClick()
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Delete expense").performClick()
        rule.onNodeWithText("Delete this expense?").assertIsDisplayed()
        rule.onNode(hasText("Delete") and hasClickAction()).performClick()
        heroShows("₹0.00")
        rule.onNodeWithText("Tea").assertDoesNotExist()
        assertNull(runBlocking { repo.observe("e1").first() })
        rule.onNodeWithText("Expense deleted").assertIsDisplayed()
        rule.onNodeWithText("Undo").performClick()
        heroShows("₹125.50")
        assertEquals(before, runBlocking { repo.observe("e1").first() })
    }

    @Test
    fun cancelDeleteKeepsRecord() {
        seed("e1", 12_550, "Tea")
        launch()
        heroShows("₹125.50")
        rule.onNodeWithText("Tea").performClick()
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Delete expense").performClick()
        rule.onNode(hasText("Cancel") and hasClickAction()).performClick()
        rule.onNodeWithText("Delete this expense?").assertDoesNotExist()
        assertEquals(1, runBlocking { repo.observeMonth(month()).first() }.size)
    }

    @Test
    fun touchTargetsAtLeast48dp() {
        seed("e1", 12_550, "Tea")
        launch()
        heroShows("₹125.50")
        rule.onNodeWithContentDescription("Previous month").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Next month").assertHeightIsAtLeast(48.dp)
        rule.onNode(hasText("Tea")).assertHeightIsAtLeast(48.dp)
        rule.onAllNodes(hasText("Add expense") and hasClickAction())[0].assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun largeFontKeepsRowsAndAddReachable() {
        seed("e1", 12_550, "Tea")
        launch(fontScale = 2f)
        heroShows("₹125.50")
        rule.onNodeWithText("Tea").assertIsDisplayed()
        rule.onAllNodes(hasText("Add expense") and hasClickAction())[0].assertIsDisplayed()
    }
}
