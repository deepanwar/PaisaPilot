package com.deepak.paisapilot.feature.activity

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepak.paisapilot.core.ActivityFilterRequests
import com.deepak.paisapilot.core.data.db.LedgerDatabase
import com.deepak.paisapilot.core.data.repo.DefaultActivityRepository
import com.deepak.paisapilot.core.data.repo.RoomPersonalExpenseRepository
import com.deepak.paisapilot.core.undo.UndoDeleteCoordinator
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.feature.personal.ResourceCategoryLabelProvider
import com.deepak.paisapilot.feature.personal.detail.ExpenseDetailRoute
import com.deepak.paisapilot.feature.personal.detail.ExpenseDetailViewModel
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: LedgerDatabase
    private lateinit var repo: RoomPersonalExpenseRepository
    private val coordinator = UndoDeleteCoordinator()
    private val thisMonth = LocalDate.now().withDayOfMonth(1)
    private val lastMonth = thisMonth.minusMonths(1)

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

    private fun seed(
        id: String,
        paise: Long,
        date: LocalDate,
        category: Category,
        title: String?,
    ) = runBlocking {
        repo.create(PersonalExpense(id, Paise(paise), title, category, date, null, 0, 0))
    }

    private fun seedLedger() {
        seed("t", 10_000, thisMonth.plusDays(1), Category.Food, "Tea")
        seed("p", 5_000, thisMonth.plusDays(2), Category.Bills, "Power")
        seed("c", 2_000, lastMonth.plusDays(4), Category.Food, "Chai")
    }

    private fun launch(fontScale: Float = 1f) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val vm = ActivityViewModel(
            SavedStateHandle(),
            DefaultActivityRepository(repo),
            ResourceCategoryLabelProvider(context),
            ActivityFilterRequests(),
        )
        rule.setContent {
            PaisaPilotTheme(dynamicColor = false) {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale),
                ) { Host(vm) }
            }
        }
    }

    @Composable
    private fun Host(vm: ActivityViewModel) {
        var detailId by remember { mutableStateOf<String?>(null) }
        val id = detailId
        if (id == null) {
            ActivityRoute(onAdd = {}, onOpenExpense = { detailId = it }, viewModel = vm)
        } else {
            key(id) {
                ExpenseDetailRoute(
                    onClose = { detailId = null },
                    onEdit = {},
                    viewModel = ExpenseDetailViewModel(
                        SavedStateHandle(mapOf("expenseId" to id)), repo, coordinator,
                    ),
                )
            }
        }
    }

    private fun waitForText(text: String) = rule.waitUntil(5_000) {
        rule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun chip(label: String) = rule.onNode(hasText(label) and hasClickAction())

    @Test
    fun feedIsOrderedAndLabelled() {
        seedLedger()
        launch()
        waitForText("Power")
        rule.onNodeWithContentDescription(
            "Personal expense, Power, Bills, ${java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).format(thisMonth.plusDays(2))}, ₹50.00",
        ).assertIsDisplayed()
        rule.onNodeWithText("3 entries").assertIsDisplayed()
    }

    @Test
    fun personalFoodInMonthShowsOnlyMatchesWithCount() {
        seedLedger()
        launch()
        waitForText("Power")
        chip("Personal").performClick()
        chip("Food").performClick()
        chip("All months").performClick()
        rule.onNode(hasContentDescription(
            java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy").format(thisMonth),
        ) and hasClickAction()).performClick()
        rule.onNodeWithText("1 entry").assertIsDisplayed()
        rule.onNodeWithText("Tea").assertIsDisplayed()
        rule.onNodeWithText("Power").assertDoesNotExist()
        rule.onNodeWithText("Chai").assertDoesNotExist()
        rule.onNodeWithText("Personal total: ₹100.00").assertIsDisplayed()
    }

    @Test
    fun impossibleSearchShowsNoMatchAndClearFiltersRestores() {
        seedLedger()
        launch()
        waitForText("Power")
        rule.onNode(hasSetTextAction()).performTextInput("impossible-term")
        waitForText("No entries match these filters.")
        rule.onNodeWithText("0 entries").assertIsDisplayed()
        rule.onNodeWithText("Clear filters").performClick()
        waitForText("Power")
        rule.onNodeWithText("3 entries").assertIsDisplayed()
    }

    @Test
    fun filtersAndSelectionSurviveOpeningDetailAndBack() {
        seedLedger()
        launch()
        waitForText("Power")
        chip("Bills").performClick()
        rule.onNodeWithText("1 entry").assertIsDisplayed()
        rule.onNodeWithText("Power").performClick()
        waitForText("Expense")
        rule.onNodeWithContentDescription("Back").performClick()
        waitForText("Power")
        chip("Bills").assertIsSelected()
        rule.onNodeWithText("1 entry").assertIsDisplayed()
        rule.onNodeWithText("Tea").assertDoesNotExist()
    }

    @Test
    fun mixedFeedShowsNoCombinedTotal() {
        seedLedger()
        launch()
        waitForText("Power")
        rule.onAllNodes(hasText("Personal total", substring = true)).assertCountEquals(0)
        rule.onAllNodes(hasText("₹170.00")).assertCountEquals(0)
    }

    @Test
    fun deletingFromDetailRemovesRowOnReturn() {
        seedLedger()
        launch()
        waitForText("Power")
        rule.onNodeWithText("Power").performClick()
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Delete expense").performClick()
        rule.onNode(hasText("Delete") and hasClickAction()).performClick()
        waitForText("2 entries")
        rule.onNodeWithText("Power").assertDoesNotExist()
    }

    @Test
    fun emptyLedgerOffersAddCta() {
        launch()
        waitForText("Add your first expense to see it here.")
        rule.onAllNodes(hasText("Add expense") and hasClickAction()).assertCountEquals(2)
    }

    @Test
    fun touchTargetsAtLeast48dp() {
        seedLedger()
        launch()
        waitForText("Power")
        chip("Personal").assertHeightIsAtLeast(48.dp)
        chip("Expenses").assertHeightIsAtLeast(48.dp)
        chip("All months").assertHeightIsAtLeast(48.dp)
        chip("Food").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("Power").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun largeFontKeepsFeedReachable() {
        seedLedger()
        launch(fontScale = 2f)
        waitForText("Power")
        rule.onNodeWithText("Power").assertIsDisplayed()
    }
}
