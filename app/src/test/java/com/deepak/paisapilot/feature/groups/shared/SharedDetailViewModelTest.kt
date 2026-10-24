package com.deepak.paisapilot.feature.groups.shared

import androidx.lifecycle.SavedStateHandle
import com.deepak.paisapilot.domain.group.Group
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.Share
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.feature.groups.FakeGroupRepository
import com.deepak.paisapilot.feature.groups.FakeSharedExpenseRepository
import com.deepak.paisapilot.feature.groups.detail.DetailStatus
import com.deepak.paisapilot.feature.groups.shared.detail.SharedDetailViewModel
import com.deepak.paisapilot.feature.onboarding.FakeSettingsRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedDetailViewModelTest {
    private fun detailTest(
        archived: Boolean = false,
        block: suspend TestScope.(FakeGroupRepository, FakeSharedExpenseRepository) -> Unit,
    ) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                val groups = FakeGroupRepository().apply {
                    this.groups.value = listOf(Group("g", "Flat", 0L, if (archived) 1L else null))
                    members.value = listOf(
                        Member("a", "g", "stored-name", isLocalOwner = true, active = true),
                        Member("b", "g", "Alex", isLocalOwner = false, active = false),
                    )
                }
                val shared = FakeSharedExpenseRepository().apply {
                    expenses.value = listOf(
                        SharedExpense(
                            id = "e1",
                            groupId = "g",
                            amount = Paise(20_000),
                            title = "Dinner",
                            category = Category.Food,
                            date = LocalDate.of(2026, 10, 18),
                            payerMemberId = "a",
                            note = null,
                            shares = listOf(Share("a", Paise(5_000)), Share("b", Paise(15_000))),
                            createdAt = 1L,
                            updatedAt = 1L,
                        ),
                    )
                }
                block(groups, shared)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun vm(groups: FakeGroupRepository, shared: FakeSharedExpenseRepository, id: String = "e1") =
        SharedDetailViewModel(
            SavedStateHandle(mapOf("groupId" to "g", "expenseId" to id)),
            shared,
            groups,
            FakeSettingsRepository(),
        )

    @Test
    fun showsPayerAndEachShareWithNamesIncludingInactive() = detailTest { groups, shared ->
        val vm = vm(groups, shared)
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals(DetailStatus.Content, state.status)
        assertEquals("Me", state.payerName)
        assertEquals(listOf("Me", "Alex"), state.shareLines.map { it.name })
        assertEquals(listOf(5_000L, 15_000L), state.shareLines.map { it.paise })
        assertFalse(state.readOnly)
    }

    @Test
    fun missingExpenseIsNotFound() = detailTest { groups, shared ->
        val vm = vm(groups, shared, id = "nope")
        advanceUntilIdle()
        assertEquals(DetailStatus.NotFound, vm.state.value.status)
    }

    // AC-SHR-08 (delete half)
    @Test
    fun confirmedDeleteRemovesExpenseWithoutShowingUnavailable() = detailTest { groups, shared ->
        val vm = vm(groups, shared)
        advanceUntilIdle()
        vm.onDeleteRequested()
        assertTrue(vm.state.value.showDeleteDialog)
        vm.onDeleteConfirmed()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state.deleted)
        assertEquals(DetailStatus.Content, state.status)
        assertTrue(shared.expenses.value.isEmpty())
    }

    @Test
    fun cancelledDeleteKeepsExpense() = detailTest { groups, shared ->
        val vm = vm(groups, shared)
        advanceUntilIdle()
        vm.onDeleteRequested()
        vm.onDeleteCancelled()
        assertFalse(vm.state.value.showDeleteDialog)
        assertEquals(1, shared.expenses.value.size)
    }

    @Test
    fun deleteFailureKeepsExpenseAndReportsError() = detailTest { groups, shared ->
        val vm = vm(groups, shared)
        advanceUntilIdle()
        shared.throwOnWrite = true
        vm.onDeleteConfirmed()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state.deleteFailed)
        assertFalse(state.deleted)
        assertEquals(1, shared.expenses.value.size)
    }

    @Test
    fun archivedGroupIsReadOnlyAndCannotRequestDelete() = detailTest(archived = true) { groups, shared ->
        val vm = vm(groups, shared)
        advanceUntilIdle()
        assertTrue(vm.state.value.readOnly)
        vm.onDeleteRequested()
        assertFalse(vm.state.value.showDeleteDialog)
    }
}
