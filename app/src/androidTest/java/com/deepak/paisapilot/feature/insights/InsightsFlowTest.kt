package com.deepak.paisapilot.feature.insights

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepak.paisapilot.core.ActivityFilterRequests
import com.deepak.paisapilot.core.data.db.LedgerDatabase
import com.deepak.paisapilot.core.data.db.entity.ExpenseGroupEntity
import com.deepak.paisapilot.core.data.db.entity.ExpenseShareEntity
import com.deepak.paisapilot.core.data.db.entity.MemberEntity
import com.deepak.paisapilot.core.data.db.entity.SharedExpenseEntity
import com.deepak.paisapilot.core.data.repo.RoomPersonalExpenseRepository
import com.deepak.paisapilot.domain.activity.ActivityScope
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InsightsFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: LedgerDatabase
    private lateinit var repo: RoomPersonalExpenseRepository
    private val requests = ActivityFilterRequests()
    private val thisMonth = LocalDate.now().withDayOfMonth(1)
    private var opened: String? = null
    private var shownInActivity = 0

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

    private fun seed(id: String, paise: Long, date: LocalDate, category: Category, title: String?) =
        runBlocking {
            repo.create(PersonalExpense(id, Paise(paise), title, category, date, null, 0, 0))
        }

    private fun launch(fontScale: Float = 1f) {
        val vm = InsightsViewModel(SavedStateHandle(), repo, requests, Clock.systemDefaultZone())
        rule.setContent {
            PaisaPilotTheme(dynamicColor = false) {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale),
                ) {
                    InsightsRoute(
                        onOpenExpense = { opened = it },
                        onShowCategoryInActivity = { shownInActivity += 1 },
                        onAdd = {},
                        viewModel = vm,
                    )
                }
            }
        }
    }

    private fun waitForDescription(text: String) = rule.waitUntil(5_000) {
        rule.onAllNodes(hasContentDescription(text)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun seedFixture() {
        seed("f", 10_000, thisMonth.plusDays(1), Category.Food, "Tea")
        seed("b", 5_000, thisMonth.plusDays(2), Category.Bills, "Power")
    }

    @Test
    fun foodAndBillsShowTotalCategoriesAndCount() {
        seedFixture()
        launch()
        waitForDescription("Personal spending, ₹150.00, 2 expenses")
        rule.onNodeWithContentDescription("Food, ₹100.00, 67 percent of spending. Show in Activity.")
            .assertIsDisplayed()
        rule.onNodeWithContentDescription("Bills, ₹50.00, 33 percent of spending. Show in Activity.")
            .assertIsDisplayed()
    }

    @Test
    fun emptyMonthShowsZeroAndEmptyMessageWithoutCategories() {
        launch()
        waitForDescription("Personal spending, ₹0.00, 0 expenses")
        rule.onNodeWithText("No expenses this month").assertIsDisplayed()
        rule.onAllNodes(hasText("Spending by category")).assertCountEquals(0)
    }

    @Test
    fun movingExpenseToAnotherMonthUpdatesBothMonths() {
        seedFixture()
        launch()
        waitForDescription("Personal spending, ₹150.00, 2 expenses")
        val lastMonthDate = thisMonth.minusMonths(1).plusDays(4)
        runBlocking {
            repo.update(PersonalExpense("f", Paise(10_000), "Tea", Category.Food, lastMonthDate, null, 0, 0))
        }
        waitForDescription("Personal spending, ₹50.00, 1 expense")
        rule.onNodeWithContentDescription("Previous month").performClick()
        waitForDescription("Personal spending, ₹100.00, 1 expense")
    }

    @Test
    fun sharedExpenseInSameMonthDoesNotAffectPersonalTotal() {
        seedFixture()
        runBlocking {
            db.schemaDao().insert(ExpenseGroupEntity("g1", "Trip", 1L, null))
            db.schemaDao().insert(MemberEntity("m1", "g1", "Me", "me", true, true))
            db.schemaDao().insert(
                SharedExpenseEntity("s1", "g1", 30_000, "Hotel", "food", thisMonth.toEpochDay(), "m1", null, 1L, 1L),
            )
            db.schemaDao().insert(ExpenseShareEntity("s1", "m1", 30_000))
        }
        launch()
        waitForDescription("Personal spending, ₹150.00, 2 expenses")
        rule.onNodeWithText("Hotel").assertDoesNotExist()
    }

    @Test
    fun tappingCategoryRequestsActivityFilter() {
        seedFixture()
        launch()
        waitForDescription("Personal spending, ₹150.00, 2 expenses")
        rule.onNodeWithContentDescription("Food, ₹100.00, 67 percent of spending. Show in Activity.")
            .performClick()
        assertEquals(1, shownInActivity)
        val request = requests.pending.value!!
        assertEquals(Category.Food, request.category)
        assertEquals(thisMonth, request.month)
        assertEquals(ActivityScope.Personal, request.scope)
    }

    @Test
    fun recentRowOpensDetail() {
        seedFixture()
        launch()
        waitForDescription("Personal spending, ₹150.00, 2 expenses")
        rule.onNode(hasText("Tea") and hasClickAction()).performClick()
        assertEquals("f", opened)
    }

    @Test
    fun touchTargetsAtLeast48dp() {
        seedFixture()
        launch()
        waitForDescription("Personal spending, ₹150.00, 2 expenses")
        rule.onNodeWithContentDescription("Previous month").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Next month").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Food, ₹100.00, 67 percent of spending. Show in Activity.")
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun largeFontKeepsCategoriesReachable() {
        seedFixture()
        launch(fontScale = 2f)
        waitForDescription("Personal spending, ₹150.00, 2 expenses")
        rule.onNodeWithContentDescription("Food, ₹100.00, 67 percent of spending. Show in Activity.")
            .assertIsDisplayed()
    }
}
