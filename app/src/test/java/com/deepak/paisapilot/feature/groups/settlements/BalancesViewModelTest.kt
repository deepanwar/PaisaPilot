package com.deepak.paisapilot.feature.groups.settlements

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.deepak.paisapilot.feature.groups.balances.BalancesUiState
import com.deepak.paisapilot.feature.groups.balances.BalancesViewModel
import com.deepak.paisapilot.feature.groups.detail.DetailStatus
import com.deepak.paisapilot.feature.onboarding.FakeSettingsRepository
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalancesViewModelTest {
    private fun vm(env: SettlementEnv, groupId: String = "g") = BalancesViewModel(
        SavedStateHandle(mapOf("groupId" to groupId)),
        env.groups,
        env.expenses,
        env.settlements,
        FakeSettingsRepository(),
    )

    private suspend fun app.cash.turbine.ReceiveTurbine<BalancesUiState>.content(): BalancesUiState {
        var item = awaitItem()
        while (item.status != DetailStatus.Content) item = awaitItem()
        return item
    }

    // AC-BAL-01
    @Test
    fun showsSignedBalancesAndSuggestionsWithLiveOwnerName() = settlementTest { env ->
        vm(env).state.test {
            val state = content()
            assertEquals(listOf("Me", "Alex", "Sam"), state.rows.map { it.name })
            assertEquals(listOf(10_000L, -6_000L, -4_000L), state.rows.map { it.netPaise })
            assertEquals(listOf("a" to 6_000L, "s" to 4_000L), state.suggestions.map { it.fromId to it.amount.value })
            assertTrue(state.suggestions.all { it.toId == "o" })
            assertFalse(state.allSettled)
            assertFalse(state.reviewBalances)
            assertTrue(state.canRecord)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun settlementShrinksSuggestions() = settlementTest { env ->
        env.settlements.settlements.value = listOf(settlement(amount = 2_000))
        vm(env).state.test {
            val state = content()
            assertEquals(listOf(8_000L, -4_000L, -4_000L), state.rows.map { it.netPaise })
            assertEquals(4_000L, state.suggestions.first().amount.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // AC-BAL-03
    @Test
    fun settledGroupHasNoSuggestionsAndCannotRecord() = settlementTest { env ->
        env.settlements.settlements.value = listOf(
            settlement("s1", from = "a", amount = 6_000),
            settlement("s2", from = "s", amount = 4_000),
        )
        vm(env).state.test {
            val state = content()
            assertTrue(state.allSettled)
            assertTrue(state.suggestions.isEmpty())
            assertFalse(state.canRecord)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // AC-BAL-08
    @Test
    fun expenseEditLeavingSettlementExcessFlagsReviewAndKeepsSumZero() = settlementTest { env ->
        env.settlements.settlements.value = listOf(settlement(amount = 6_000))
        env.expenses.expenses.value = listOf(expense(amount = 6_000, a = 2_000, s = 4_000))
        vm(env).state.test {
            val state = content()
            assertTrue(state.reviewBalances)
            assertEquals(0L, state.rows.sumOf { it.netPaise })
            assertEquals(1, env.settlements.settlements.value.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun archivedGroupCannotRecord() = settlementTest(archived = true) { env ->
        vm(env).state.test {
            val state = content()
            assertTrue(state.archived)
            assertFalse(state.canRecord)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingGroupIsNotFound() = settlementTest { env ->
        val vm = vm(env, groupId = "nope")
        vm.state.test {
            var item = awaitItem()
            while (item.status == DetailStatus.Loading) item = awaitItem()
            assertEquals(DetailStatus.NotFound, item.status)
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()
    }
}
