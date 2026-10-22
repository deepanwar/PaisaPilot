package com.deepak.paisapilot.feature.insights

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.deepak.paisapilot.core.ActivityFilterRequests
import com.deepak.paisapilot.domain.activity.ActivityScope
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.feature.personal.FakePersonalExpenseRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC)
    private val requests = ActivityFilterRequests()

    private fun insightsTest(block: suspend TestScope.() -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) { block() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun expense(id: String, paise: Long, date: LocalDate, category: Category, created: Long = 1L) =
        PersonalExpense(id, Paise(paise), null, category, date, null, created, created)

    private fun vm(
        repo: FakePersonalExpenseRepository,
        handle: SavedStateHandle = SavedStateHandle(),
    ) = InsightsViewModel(handle, repo, requests, clock)

    private suspend fun ReceiveTurbine<InsightsUiState>.content(): InsightsUiState.Content {
        var item = awaitItem()
        while (item !is InsightsUiState.Content) item = awaitItem()
        return item
    }

    private suspend fun ReceiveTurbine<InsightsUiState>.contentWhere(
        check: (InsightsUiState.Content) -> Boolean,
    ): InsightsUiState.Content {
        var item = content()
        while (!check(item)) item = content()
        return item
    }

    private fun foodAndBills() = FakePersonalExpenseRepository().apply {
        rows.value = listOf(
            expense("f", 10_000, LocalDate.of(2026, 10, 3), Category.Food),
            expense("b", 5_000, LocalDate.of(2026, 10, 9), Category.Bills),
            expense("s", 700, LocalDate.of(2026, 9, 30), Category.Other),
        ).associateBy { it.id }
    }

    @Test
    fun fixtureTotalCategoriesCountAndRecent() = insightsTest {
        vm(foodAndBills()).state.test {
            val s = content()
            assertEquals(Paise(15_000), s.total)
            assertEquals(2, s.count)
            assertEquals(listOf(Category.Food, Category.Bills), s.categories.map { it.category })
            assertEquals(listOf(67, 33), s.categories.map { it.percent })
            assertEquals(listOf("b", "f"), s.recent.map { it.id })
            assertTrue(s.isCurrentMonth)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyMonthHasZeroTotalAndNoCategories() = insightsTest {
        val handle = SavedStateHandle(mapOf("insightsMonthStart" to LocalDate.of(2026, 8, 1).toEpochDay()))
        vm(foodAndBills(), handle).state.test {
            val s = content()
            assertEquals(Paise(0), s.total)
            assertEquals(0, s.count)
            assertTrue(s.categories.isEmpty())
            assertTrue(s.recent.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recentIsCappedButTotalCountsEverything() = insightsTest {
        val repo = FakePersonalExpenseRepository()
        repo.rows.value = (1..8).associate {
            "e$it" to expense("e$it", 100, LocalDate.of(2026, 10, it), Category.Food)
        }
        vm(repo).state.test {
            val s = content()
            assertEquals(INSIGHTS_RECENT_LIMIT, s.recent.size)
            assertEquals(8, s.count)
            assertEquals(Paise(800), s.total)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun backdatedEditMovesAmountBetweenMonthsExactly() = insightsTest {
        val repo = foodAndBills()
        val octHandle = SavedStateHandle()
        val sepHandle = SavedStateHandle(mapOf("insightsMonthStart" to LocalDate.of(2026, 9, 1).toEpochDay()))
        val oct = vm(repo, octHandle)
        val sep = vm(repo, sepHandle)
        oct.state.test {
            assertEquals(Paise(15_000), content().total)
            sep.state.test {
                assertEquals(Paise(700), content().total)
                repo.update(expense("f", 10_000, LocalDate.of(2026, 9, 12), Category.Food))
                assertEquals(Paise(10_700), contentWhere { it.total != Paise(700) }.total)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(Paise(5_000), contentWhere { it.total == Paise(5_000) }.total)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun monthNavigationCrossesYearAndPersists() = insightsTest {
        val repo = foodAndBills()
        val handle = SavedStateHandle()
        val viewModel = vm(repo, handle)
        viewModel.state.test {
            content()
            repeat(3) { viewModel.onNextMonth() }
            assertEquals(
                LocalDate.of(2027, 1, 1),
                contentWhere { it.month.start == LocalDate.of(2027, 1, 1) }.month.start,
            )
            cancelAndIgnoreRemainingEvents()
        }
        vm(repo, handle).state.test {
            assertEquals(LocalDate.of(2027, 1, 1), content().month.start)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun navigationStopsAtBounds() = insightsTest {
        val handle = SavedStateHandle(mapOf("insightsMonthStart" to LocalDate.of(2100, 12, 1).toEpochDay()))
        val viewModel = vm(foodAndBills(), handle)
        viewModel.state.test {
            val s = content()
            assertFalse(s.canGoNext)
            viewModel.onNextMonth()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun readErrorThenRetryRecovers() = insightsTest {
        val repo = foodAndBills().apply { throwOnObserveMonth = true }
        val viewModel = vm(repo)
        viewModel.state.test {
            var item = awaitItem()
            while (item !is InsightsUiState.Error) item = awaitItem()
            repo.throwOnObserveMonth = false
            viewModel.onRetry()
            assertEquals(Paise(15_000), content().total)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun categorySelectionRequestsActivityFilterForShownMonth() = insightsTest {
        val viewModel = vm(foodAndBills())
        viewModel.onCategorySelected(Category.Food)
        val request = requests.pending.value!!
        assertEquals(Category.Food, request.category)
        assertEquals(LocalDate.of(2026, 10, 1), request.month)
        assertEquals(ActivityScope.Personal, request.scope)
    }
}
