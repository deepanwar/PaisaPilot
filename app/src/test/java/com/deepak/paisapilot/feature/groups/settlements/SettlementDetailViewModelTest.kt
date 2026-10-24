package com.deepak.paisapilot.feature.groups.settlements

import androidx.lifecycle.SavedStateHandle
import com.deepak.paisapilot.feature.groups.detail.DetailStatus
import com.deepak.paisapilot.feature.groups.settlements.detail.SettlementDetailViewModel
import com.deepak.paisapilot.feature.onboarding.FakeSettingsRepository
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementDetailViewModelTest {
    private fun vm(env: SettlementEnv, id: String = "s1") = SettlementDetailViewModel(
        SavedStateHandle(mapOf("groupId" to "g", "settlementId" to id)),
        env.settlements,
        env.groups,
        FakeSettingsRepository(),
    )

    @Test
    fun showsBothNamesWithLiveOwnerName() = settlementTest { env ->
        env.settlements.settlements.value = listOf(settlement(from = "a", to = "o"))
        val vm = vm(env)
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals(DetailStatus.Content, state.status)
        assertEquals("Alex", state.fromName)
        assertEquals("Me", state.toName)
        assertTrue(state.toIsOwner)
        assertFalse(state.readOnly)
    }

    @Test
    fun missingSettlementIsNotFound() = settlementTest { env ->
        val vm = vm(env, id = "nope")
        advanceUntilIdle()
        assertEquals(DetailStatus.NotFound, vm.state.value.status)
    }

    // AC-BAL-06
    @Test
    fun confirmedDeleteRemovesSettlementWithoutShowingUnavailable() = settlementTest { env ->
        env.settlements.settlements.value = listOf(settlement())
        val vm = vm(env)
        advanceUntilIdle()
        vm.onDeleteRequested()
        assertTrue(vm.state.value.showDeleteDialog)
        vm.onDeleteConfirmed()
        advanceUntilIdle()
        assertTrue(vm.state.value.deleted)
        assertEquals(DetailStatus.Content, vm.state.value.status)
        assertTrue(env.settlements.settlements.value.isEmpty())
        assertEquals(1, env.expenses.expenses.value.size)
    }

    @Test
    fun cancelledDeleteKeepsSettlement() = settlementTest { env ->
        env.settlements.settlements.value = listOf(settlement())
        val vm = vm(env)
        advanceUntilIdle()
        vm.onDeleteRequested()
        vm.onDeleteCancelled()
        assertFalse(vm.state.value.showDeleteDialog)
        assertEquals(1, env.settlements.settlements.value.size)
    }

    @Test
    fun archivedGroupIsReadOnlyAndRefusesDelete() = settlementTest(archived = true) { env ->
        env.settlements.settlements.value = listOf(settlement())
        val vm = vm(env)
        advanceUntilIdle()
        assertTrue(vm.state.value.readOnly)
        vm.onDeleteRequested()
        assertFalse(vm.state.value.showDeleteDialog)
    }

    @Test
    fun failedDeleteShowsErrorAndKeepsRecord() = settlementTest { env ->
        env.settlements.settlements.value = listOf(settlement())
        env.settlements.throwOnWrite = true
        val vm = vm(env)
        advanceUntilIdle()
        vm.onDeleteRequested()
        vm.onDeleteConfirmed()
        advanceUntilIdle()
        assertTrue(vm.state.value.deleteFailed)
        assertFalse(vm.state.value.deleted)
        assertEquals(1, env.settlements.settlements.value.size)
    }
}
