package com.deepak.paisapilot.feature.groups.shared

import androidx.lifecycle.SavedStateHandle
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.repo.SharedWriteResult
import com.deepak.paisapilot.domain.group.Group
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.Share
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.feature.groups.FakeGroupRepository
import com.deepak.paisapilot.feature.groups.FakeSharedExpenseRepository
import com.deepak.paisapilot.feature.groups.shared.form.SharedFormViewModel
import com.deepak.paisapilot.feature.groups.shared.form.SharedSaveError
import com.deepak.paisapilot.feature.groups.shared.form.SplitMode
import com.deepak.paisapilot.feature.onboarding.FakeSettingsRepository
import com.deepak.paisapilot.feature.personal.form.FormLoadState
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedFormViewModelTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC)

    private class Env(
        val groups: FakeGroupRepository,
        val shared: FakeSharedExpenseRepository,
    )

    private fun formTest(block: suspend TestScope.(Env) -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                val groups = FakeGroupRepository().apply {
                    this.groups.value = listOf(Group("g", "Flat", 0L, null))
                    members.value = listOf(
                        Member("a", "g", "stored-name", isLocalOwner = true, active = true),
                        Member("b", "g", "Alex", isLocalOwner = false, active = true),
                    )
                }
                block(Env(groups, FakeSharedExpenseRepository()))
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun vm(env: Env, expenseId: String? = null, handle: SavedStateHandle? = null) =
        SharedFormViewModel(
            handle ?: SavedStateHandle(
                buildMap {
                    put("groupId", "g")
                    if (expenseId != null) put("expenseId", expenseId)
                },
            ),
            env.shared,
            env.groups,
            FakeSettingsRepository(),
            clock,
            IdGenerator { "new-id" },
        )

    private fun stored(
        amount: Long = 20_000,
        shares: List<Share> = listOf(Share("a", Paise(10_000)), Share("b", Paise(10_000))),
    ) = SharedExpense(
        id = "e1",
        groupId = "g",
        amount = Paise(amount),
        title = "Dinner",
        category = Category.Food,
        date = LocalDate.of(2026, 10, 18),
        payerMemberId = "a",
        note = null,
        shares = shares,
        createdAt = 10L,
        updatedAt = 10L,
    )

    @Test
    fun createStartsWithAllActiveSelectedAndOwnerPaying() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals(FormLoadState.Ready, state.loadState)
        assertEquals("a", state.payerId)
        assertEquals(listOf(true, true), state.rows.map { it.included })
        // G7: owner shows the live profile name.
        assertEquals("Me", state.rows.first { it.member.id == "a" }.member.name)
        assertFalse(state.canSave)
        assertFalse(state.isDirty)
    }

    // AC-SHR-01
    @Test
    fun equalSplitShowsSharesAndNetPreview() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("200")
        val state = vm.state.value
        assertEquals(listOf("₹100.00", "₹100.00"), state.rows.map { it.shareText })
        assertTrue(state.canSave)
        assertEquals(0L, state.remainingPaise)
        assertEquals(10_000L, state.netPreview.first { it.member.id == "a" }.netPaise)
        assertEquals(-10_000L, state.netPreview.first { it.member.id == "b" }.netPaise)
    }

    @Test
    fun saveCreatesExpenseAndMarksSaved() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("200")
        vm.onTitleChange("Dinner")
        vm.onSave()
        advanceUntilIdle()
        val written = env.shared.lastWritten!!
        assertEquals("new-id", written.id)
        assertEquals("g", written.groupId)
        assertEquals(listOf(10_000L, 10_000L), written.shares.map { it.paise.value })
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun doubleTapSavesOnce() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("200")
        vm.onSave()
        vm.onSave()
        advanceUntilIdle()
        assertEquals(1, env.shared.creates)
    }

    // AC-SHR-03
    @Test
    fun payerNeedNotBeBeneficiary() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("10")
        vm.onToggleMember("a")
        assertTrue(vm.state.value.canSave)
        vm.onSave()
        advanceUntilIdle()
        assertEquals(listOf("b"), env.shared.lastWritten!!.shares.map { it.memberId })
    }

    @Test
    fun noBeneficiaryBlocksSave() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("10")
        vm.onToggleMember("a")
        vm.onToggleMember("b")
        assertTrue(vm.state.value.noBeneficiaries)
        assertFalse(vm.state.value.canSave)
    }

    // AC-SHR-04 / AC-SHR-05
    @Test
    fun exactSplitBlocksUntilSharesSumToTotal() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("100")
        vm.onSplitModeChange(SplitMode.Exact)
        assertEquals(listOf("50.00", "50.00"), vm.state.value.rows.map { it.shareText })
        vm.onExactShareChange("a", "60")
        vm.onExactShareChange("b", "30")
        var state = vm.state.value
        assertEquals(1_000L, state.remainingPaise)
        assertFalse(state.canSave)
        vm.onExactShareChange("b", "40")
        state = vm.state.value
        assertEquals(0L, state.remainingPaise)
        assertTrue(state.canSave)
        vm.onSave()
        advanceUntilIdle()
        assertEquals(listOf(6_000L, 4_000L), env.shared.lastWritten!!.shares.map { it.paise.value })
    }

    // AC-SHR-06
    @Test
    fun changingTotalInExactKeepsSharesAndShowsRemaining() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("100")
        vm.onSplitModeChange(SplitMode.Exact)
        vm.onExactShareChange("a", "60")
        vm.onExactShareChange("b", "40")
        vm.onAmountChange("110")
        val state = vm.state.value
        assertEquals(listOf("60", "40"), state.rows.map { it.shareText })
        assertEquals(1_000L, state.remainingPaise)
        assertFalse(state.canSave)
    }

    @Test
    fun exactInvalidShareTextFlagsRowAndBlocksSave() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("100")
        vm.onSplitModeChange(SplitMode.Exact)
        vm.onExactShareChange("a", "abc")
        assertTrue(vm.state.value.rows.first { it.member.id == "a" }.shareError != null)
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun saveFailureKeepsEntriesAndAllowsRetry() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("200")
        vm.onTitleChange("Dinner")
        env.shared.throwOnWrite = true
        vm.onSave()
        advanceUntilIdle()
        var state = vm.state.value
        assertEquals(SharedSaveError.Failed, state.saveError)
        assertEquals("200", state.amountText)
        assertEquals("Dinner", state.title)
        assertFalse(state.saved)
        assertTrue(state.canSave)
        env.shared.throwOnWrite = false
        vm.onSave()
        advanceUntilIdle()
        state = vm.state.value
        assertTrue(state.saved)
    }

    @Test
    fun staleMemberResultShowsMemberChangedError() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("200")
        env.shared.forcedResult = SharedWriteResult.MemberInvalid
        vm.onSave()
        advanceUntilIdle()
        assertEquals(SharedSaveError.MemberChanged, vm.state.value.saveError)
        assertFalse(vm.state.value.saved)
    }

    @Test
    fun payerDeactivatedWhileEditingBlocksSaveWithError() = formTest { env ->
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("200")
        vm.onPayerChange("b")
        env.groups.setMemberActive("b", false)
        advanceUntilIdle()
        val state = vm.state.value
        assertFalse(state.canSave)
        assertEquals(SharedSaveError.MemberChanged, state.saveError)
    }

    @Test
    fun archivedGroupIsReadOnly() = formTest { env ->
        env.groups.archive("g")
        val vm = vm(env)
        advanceUntilIdle()
        vm.onAmountChange("200")
        assertTrue(vm.state.value.readOnly)
        assertFalse(vm.state.value.canSave)
    }

    // AC-SHR-07
    @Test
    fun editLoadsStoredValuesAndUpdatesSameId() = formTest { env ->
        env.shared.expenses.value = listOf(stored())
        val vm = vm(env, expenseId = "e1")
        advanceUntilIdle()
        var state = vm.state.value
        assertTrue(state.isEdit)
        assertEquals("e1", state.recordId)
        assertEquals("200.00", state.amountText)
        assertEquals(SplitMode.Equally, state.splitMode)
        assertFalse(state.isDirty)
        vm.onAmountChange("300")
        vm.onSave()
        advanceUntilIdle()
        assertEquals(1, env.shared.updates)
        assertEquals(0, env.shared.creates)
        assertEquals("e1", env.shared.lastWritten!!.id)
        assertEquals(listOf(15_000L, 15_000L), env.shared.lastWritten!!.shares.map { it.paise.value })
    }

    @Test
    fun editOfUnevenSharesOpensInExactMode() = formTest { env ->
        env.shared.expenses.value = listOf(
            stored(shares = listOf(Share("a", Paise(5_000)), Share("b", Paise(15_000)))),
        )
        val vm = vm(env, expenseId = "e1")
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals(SplitMode.Exact, state.splitMode)
        assertEquals(listOf("50.00", "150.00"), state.rows.map { it.shareText })
        assertTrue(state.canSave)
    }

    @Test
    fun editKeepsInactiveHistoricalBeneficiaryRow() = formTest { env ->
        env.shared.expenses.value = listOf(stored())
        env.groups.setMemberActive("b", false)
        val vm = vm(env, expenseId = "e1")
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals(listOf("a", "b"), state.rows.map { it.member.id })
        assertTrue(state.canSave)
    }

    @Test
    fun editOfMissingExpenseShowsNotFound() = formTest { env ->
        val vm = vm(env, expenseId = "nope")
        advanceUntilIdle()
        assertEquals(FormLoadState.NotFound, vm.state.value.loadState)
    }

    @Test
    fun draftSurvivesRecreationThroughSavedState() = formTest { env ->
        val handle = SavedStateHandle(mapOf("groupId" to "g"))
        val first = vm(env, handle = handle)
        advanceUntilIdle()
        first.onAmountChange("90")
        first.onToggleMember("b")
        val second = vm(env, handle = handle)
        advanceUntilIdle()
        val state = second.state.value
        assertEquals("90", state.amountText)
        assertEquals(listOf(true, false), state.rows.map { it.included })
        assertNull(state.saveError)
    }
}
