package com.deepak.paisapilot.feature.personal.today

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
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
class TodayViewModelTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC)

    private fun todayTest(block: suspend TestScope.() -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) { block() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun expense(id: String, paise: Long, date: LocalDate, created: Long = 1L) =
        PersonalExpense(id, Paise(paise), null, Category.Other, date, null, created, created)

    private fun TestScope.vm(
        repo: FakePersonalExpenseRepository,
        handle: SavedStateHandle = SavedStateHandle(),
    ) = TodayViewModel(handle, repo, clock)

    private suspend fun app.cash.turbine.ReceiveTurbine<TodayUiState>.content(): TodayUiState.Content {
        var item = awaitItem()
        while (item !is TodayUiState.Content) item = awaitItem()
        return item
    }

    @Test
    fun currentMonthSumAndNewestFirst() = todayTest {
        val repo = FakePersonalExpenseRepository()
        repo.rows.value = listOf(
            expense("a", 10_000, LocalDate.of(2026, 10, 1)),
            expense("b", 5_000, LocalDate.of(2026, 10, 20)),
            expense("c", 700, LocalDate.of(2026, 9, 30)),
        ).associateBy { it.id }
        vm(repo).state.test {
            val state = content()
            assertEquals(Paise(15_000), state.total)
            assertEquals(listOf("b", "a"), state.rows.map { it.id })
            assertTrue(state.isCurrentMonth)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyMonthShowsZeroTotal() = todayTest {
        vm(FakePersonalExpenseRepository()).state.test {
            val state = content()
            assertEquals(Paise(0), state.total)
            assertTrue(state.rows.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun monthNavigationCrossesYearBoundaryAndPersists() = todayTest {
        val repo = FakePersonalExpenseRepository()
        repo.rows.value = mapOf("j" to expense("j", 900, LocalDate.of(2027, 1, 5)))
        val handle = SavedStateHandle()
        val viewModel = vm(repo, handle)
        viewModel.state.test {
            content()
            repeat(3) { viewModel.onNextMonth() }
            var state = content()
            while (state.month.start != LocalDate.of(2027, 1, 1)) state = content()
            assertEquals(Paise(900), state.total)
            assertFalse(state.isCurrentMonth)
            cancelAndIgnoreRemainingEvents()
        }
        vm(repo, handle).state.test {
            assertEquals(LocalDate.of(2027, 1, 1), content().month.start)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun navigationStopsAtBounds() = todayTest {
        val handle = SavedStateHandle(mapOf("monthStart" to LocalDate.of(2000, 1, 1).toEpochDay()))
        val viewModel = vm(FakePersonalExpenseRepository(), handle)
        viewModel.state.test {
            val first = content()
            assertFalse(first.canGoPrevious)
            viewModel.onPreviousMonth()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recentListIsCappedButTotalCountsEverything() = todayTest {
        val repo = FakePersonalExpenseRepository()
        repo.rows.value = (1..12).associate {
            "e$it" to expense("e$it", 100, LocalDate.of(2026, 10, it), created = it.toLong())
        }
        vm(repo).state.test {
            val state = content()
            assertEquals(RECENT_LIMIT, state.rows.size)
            assertTrue(state.hasMore)
            assertEquals(Paise(1_200), state.total)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun backdatedEditMovesBetweenMonths() = todayTest {
        val repo = FakePersonalExpenseRepository()
        repo.rows.value = mapOf("a" to expense("a", 1_000, LocalDate.of(2026, 10, 5)))
        vm(repo).state.test {
            assertEquals(Paise(1_000), content().total)
            repo.update(expense("a", 1_000, LocalDate.of(2026, 9, 5)))
            var state = content()
            while (state.total != Paise(0)) state = content()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun readErrorThenRetryRecovers() = todayTest {
        val repo = FakePersonalExpenseRepository().apply { throwOnObserveMonth = true }
        val viewModel = vm(repo)
        viewModel.state.test {
            var item = awaitItem()
            while (item !is TodayUiState.Error) item = awaitItem()
            repo.throwOnObserveMonth = false
            viewModel.onRetry()
            content()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
