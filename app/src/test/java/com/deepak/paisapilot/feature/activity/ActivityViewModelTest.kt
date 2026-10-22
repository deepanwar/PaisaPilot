package com.deepak.paisapilot.feature.activity

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.deepak.paisapilot.core.ActivityFilterRequests
import com.deepak.paisapilot.core.data.repo.DefaultActivityRepository
import com.deepak.paisapilot.domain.activity.ActivityFilter
import com.deepak.paisapilot.domain.activity.ActivityScope
import com.deepak.paisapilot.domain.activity.ActivityType
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.feature.personal.CategoryLabelProvider
import com.deepak.paisapilot.feature.personal.FakePersonalExpenseRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {
    private val labels = CategoryLabelProvider { it.key.replaceFirstChar(Char::uppercase) }

    private fun activityTest(block: suspend TestScope.() -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) { block() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun expense(
        id: String,
        paise: Long,
        date: LocalDate,
        category: Category = Category.Other,
        title: String? = null,
        note: String? = null,
        created: Long = 1L,
    ) = PersonalExpense(id, Paise(paise), title, category, date, note, created, created)

    private fun seeded() = FakePersonalExpenseRepository().apply {
        rows.value = listOf(
            expense("a", 10_000, LocalDate.of(2026, 10, 20), Category.Food, "Tea"),
            expense("b", 5_000, LocalDate.of(2026, 10, 3), Category.Bills, "Power"),
            expense("c", 2_000, LocalDate.of(2026, 9, 5), Category.Food, note = "chai"),
        ).associateBy { it.id }
    }

    private val requests = ActivityFilterRequests()

    private fun vm(
        repo: FakePersonalExpenseRepository = seeded(),
        handle: SavedStateHandle = SavedStateHandle(),
    ) = ActivityViewModel(handle, DefaultActivityRepository(repo), labels, requests)

    private suspend fun ReceiveTurbine<ActivityUiState>.content(): ActivityUiState.Content {
        var item = awaitItem()
        while (item !is ActivityUiState.Content) item = awaitItem()
        return item
    }

    private suspend fun ReceiveTurbine<ActivityUiState>.contentWhere(
        check: (ActivityUiState.Content) -> Boolean,
    ): ActivityUiState.Content {
        var item = content()
        while (!check(item)) item = content()
        return item
    }

    @Test
    fun defaultShowsAllNewestFirstWithNoTotal() = activityTest {
        vm().state.test {
            val s = content()
            assertEquals(listOf("a", "b", "c"), s.rows.map { it.id })
            assertEquals(3, s.resultCount)
            assertNull(s.scopeTotal)
            assertEquals(
                listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 9, 1)),
                s.availableMonths,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun personalFoodInMonthNarrowsAndCountsMatch() = activityTest {
        val viewModel = vm()
        viewModel.state.test {
            content()
            viewModel.onScopeChange(ActivityScope.Personal)
            viewModel.onCategoryChange(Category.Food)
            viewModel.onMonthChange(LocalDate.of(2026, 10, 17))
            val s = contentWhere { it.filter.month != null && it.filter.category != null }
            assertEquals(listOf("a"), s.rows.map { it.id })
            assertEquals(1, s.resultCount)
            assertEquals(Paise(10_000), s.scopeTotal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun finalQueryWins() = activityTest {
        val viewModel = vm()
        viewModel.state.test {
            content()
            "power".forEachIndexed { i, _ -> viewModel.onQueryChange("power".take(i + 1)) }
            val s = contentWhere { it.filter.query == "power" }
            assertEquals(listOf("b"), s.rows.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchMatchesNoteAndCategoryLabelCaseInsensitively() = activityTest {
        val viewModel = vm()
        viewModel.state.test {
            content()
            viewModel.onQueryChange("  CHAI ")
            assertEquals(listOf("c"), contentWhere { it.filter.query.isNotEmpty() }.rows.map { it.id })
            viewModel.onQueryChange("bills")
            assertEquals(listOf("b"), contentWhere { it.filter.query == "bills" }.rows.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noMatchThenClearFiltersRestoresList() = activityTest {
        val viewModel = vm()
        viewModel.state.test {
            content()
            viewModel.onQueryChange("impossible-term")
            val none = contentWhere { it.filter.query.isNotEmpty() }
            assertEquals(0, none.resultCount)
            assertTrue(none.hasAnyData)
            viewModel.onClearFilters()
            val all = contentWhere { it.filter.isDefault }
            assertEquals(3, all.resultCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyLedgerHasNoData() = activityTest {
        vm(FakePersonalExpenseRepository()).state.test {
            val s = content()
            assertFalse(s.hasAnyData)
            assertEquals(0, s.resultCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filtersSurviveNewViewModelOnSameHandle() = activityTest {
        val handle = SavedStateHandle()
        val first = vm(handle = handle)
        first.onQueryChange("tea")
        first.onScopeChange(ActivityScope.Personal)
        first.onTypeChange(ActivityType.Expenses)
        first.onCategoryChange(Category.Food)
        first.onMonthChange(LocalDate.of(2026, 10, 9))
        vm(handle = handle).state.test {
            val f = content().filter
            assertEquals("tea", f.query)
            assertEquals(ActivityScope.Personal, f.scope)
            assertEquals(ActivityType.Expenses, f.type)
            assertEquals(Category.Food, f.category)
            assertEquals(LocalDate.of(2026, 10, 1), f.month)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun editBackdateAndDeleteShowUpWithoutRefresh() = activityTest {
        val repo = seeded()
        vm(repo).state.test {
            assertEquals(3, content().resultCount)
            repo.update(expense("a", 10_000, LocalDate.of(2026, 8, 1), Category.Food, "Tea"))
            val moved = contentWhere { it.rows.last().id == "a" }
            assertEquals(listOf("b", "c", "a"), moved.rows.map { it.id })
            assertTrue(moved.availableMonths.contains(LocalDate.of(2026, 8, 1)))
            repo.delete("b")
            assertEquals(2, contentWhere { it.resultCount == 2 }.resultCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun readErrorThenRetryRecovers() = activityTest {
        val repo = seeded().apply { throwOnObserveAll = true }
        val viewModel = vm(repo)
        viewModel.state.test {
            var item = awaitItem()
            while (item !is ActivityUiState.Error) item = awaitItem()
            repo.throwOnObserveAll = false
            viewModel.onRetry()
            assertEquals(3, content().resultCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pendingRequestIsAppliedOnceAndPersisted() = activityTest {
        val handle = SavedStateHandle()
        val viewModel = vm(handle = handle)
        viewModel.state.test {
            content()
            requests.request(
                ActivityFilter(
                    month = LocalDate.of(2026, 10, 1),
                    scope = ActivityScope.Personal,
                    category = Category.Food,
                ),
            )
            val s = contentWhere { it.filter.category == Category.Food }
            assertEquals(listOf("a"), s.rows.map { it.id })
            assertEquals(ActivityScope.Personal, s.filter.scope)
            assertNull(requests.pending.value)
            cancelAndIgnoreRemainingEvents()
        }
        vm(handle = handle).state.test {
            assertEquals(Category.Food, content().filter.category)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun requestMadeBeforeViewModelExistsIsAppliedOnCreation() = activityTest {
        requests.request(ActivityFilter(category = Category.Bills))
        vm().state.test {
            val s = contentWhere { it.filter.category == Category.Bills }
            assertEquals(listOf("b"), s.rows.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
