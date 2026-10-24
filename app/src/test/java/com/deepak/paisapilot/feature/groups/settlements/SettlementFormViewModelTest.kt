package com.deepak.paisapilot.feature.groups.settlements

import androidx.lifecycle.SavedStateHandle
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.repo.SettlementWriteResult
import com.deepak.paisapilot.domain.group.SettlementValidation
import com.deepak.paisapilot.feature.groups.settlements.form.SettlementFormViewModel
import com.deepak.paisapilot.feature.groups.settlements.form.SettlementSaveError
import com.deepak.paisapilot.feature.onboarding.FakeSettingsRepository
import com.deepak.paisapilot.feature.personal.form.FormLoadState
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementFormViewModelTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC)

    private fun vm(env: SettlementEnv, args: Map<String, Any?> = emptyMap(), handle: SavedStateHandle? = null) =
        SettlementFormViewModel(
            handle ?: SavedStateHandle(mapOf<String, Any?>("groupId" to "g") + args),
            env.settlements,
            env.expenses,
            env.groups,
            FakeSettingsRepository(),
            clock,
            IdGenerator { "new-id" },
        )

    @Test
    fun createDefaultsToTheFirstSuggestion() = settlementTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals(FormLoadState.Ready, state.loadState)
        assertEquals("a", state.fromId)
        assertEquals("o", state.toId)
        assertEquals("60.00", state.amountText)
        assertEquals(LocalDate.of(2026, 10, 20), state.date)
        assertEquals(listOf("Me", "Alex", "Sam"), state.members.map { it.name })
        assertTrue(state.canSave)
        assertFalse(state.isDirty)
    }

    @Test
    fun routeArgumentsPrefillThePair() = settlementTest { env ->
        val vm = vm(env, mapOf("fromId" to "s", "toId" to "o", "amountPaise" to 4_000L))
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals("s", state.fromId)
        assertEquals("40.00", state.amountText)
        assertTrue(state.canSave)
    }

    // AC-BAL-04
    @Test
    fun overSuggestionAndReversedPairBlockSave() = settlementTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("60.01")
        assertEquals(SettlementValidation.ExceedsSuggestion(6_000), vm.state.value.validation)
        assertFalse(vm.state.value.canSave)
        vm.onAmountChange("10")
        vm.onFromChange("o")
        vm.onToChange("a")
        assertEquals(SettlementValidation.PairNotSuggested, vm.state.value.validation)
        assertFalse(vm.state.value.canSave)
        vm.onToChange("o")
        assertEquals(SettlementValidation.SameMember, vm.state.value.validation)
    }

    @Test
    fun saveCreatesOnceAndSignalsSaved() = settlementTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("25")
        vm.onNoteChange("Cash")
        vm.onSave()
        vm.onSave()
        advanceUntilIdle()
        assertTrue(vm.state.value.saved)
        assertEquals(1, env.settlements.creates)
        val written = env.settlements.lastWritten!!
        assertEquals("new-id", written.id)
        assertEquals(2_500L, written.amount.value)
        assertEquals("a", written.fromMemberId)
        assertEquals("o", written.toMemberId)
    }

    @Test
    fun staleBalanceFromRepositoryShowsInlineError() = settlementTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        env.settlements.forcedResult = SettlementWriteResult.Invalid(SettlementValidation.ExceedsSuggestion(1_000))
        vm.onSave()
        advanceUntilIdle()
        assertEquals(SettlementSaveError.BalanceChanged(1_000), vm.state.value.saveError)
        assertFalse(vm.state.value.saved)
    }

    @Test
    fun writeFailureKeepsEntries() = settlementTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("12")
        env.settlements.throwOnWrite = true
        vm.onSave()
        advanceUntilIdle()
        assertEquals(SettlementSaveError.Failed, vm.state.value.saveError)
        assertEquals("12", vm.state.value.amountText)
        assertFalse(vm.state.value.isSaving)
    }

    // AC-BAL-05
    @Test
    fun editLoadsStoredAndSavesSameId() = settlementTest { env ->
        env.settlements.settlements.value = listOf(settlement(amount = 2_000))
        val vm = vm(env, mapOf("settlementId" to "s1"))
        advanceUntilIdle()
        val loaded = vm.state.value
        assertTrue(loaded.isEdit)
        assertEquals("20.00", loaded.amountText)
        assertFalse(loaded.canSave)
        vm.onAmountChange("25")
        assertEquals(SettlementValidation.Valid::class, vm.state.value.validation!!::class)
        assertTrue(vm.state.value.canSave)
        vm.onSave()
        advanceUntilIdle()
        assertTrue(vm.state.value.saved)
        assertEquals(1, env.settlements.updates)
        assertEquals(0, env.settlements.creates)
        assertEquals("s1", env.settlements.lastWritten!!.id)
        assertEquals(2_500L, env.settlements.settlements.value.single().amount.value)
    }

    @Test
    fun editOfMissingSettlementIsNotFound() = settlementTest { env ->
        val vm = vm(env, mapOf("settlementId" to "nope"))
        advanceUntilIdle()
        assertEquals(FormLoadState.NotFound, vm.state.value.loadState)
    }

    @Test
    fun archivedGroupCannotSave() = settlementTest(archived = true) { env ->
        val vm = vm(env)
        advanceUntilIdle()
        assertTrue(vm.state.value.readOnly)
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun restoredDraftWinsOverDefaults() = settlementTest { env ->
        val handle = SavedStateHandle(
            mapOf(
                "groupId" to "g",
                "started" to true,
                "draftFrom" to "s",
                "draftTo" to "o",
                "draftAmount" to "12",
                "draftDay" to LocalDate.of(2026, 10, 1).toEpochDay(),
                "draftNote" to "",
            ),
        )
        val vm = vm(env, handle = handle)
        advanceUntilIdle()
        assertEquals("s", vm.state.value.fromId)
        assertEquals("12", vm.state.value.amountText)
        assertNull(vm.state.value.saveError)
    }
}
