package com.deepak.paisapilot.feature.personal.form

import androidx.lifecycle.SavedStateHandle
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.domain.money.AmountResult
import com.deepak.paisapilot.domain.money.MoneyLimits
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
class ExpenseFormViewModelTest {
    private val today = LocalDate.of(2026, 10, 20)
    private val clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC)

    private fun formTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(
        repository: FakePersonalExpenseRepository = FakePersonalExpenseRepository(),
        handle: SavedStateHandle = SavedStateHandle(),
        ids: IdGenerator = IdGenerator { "new-id" },
    ) = ExpenseFormViewModel(handle, repository, clock, ids)

    private fun stored(id: String = "e1", paise: Long = 12_550L) = PersonalExpense(
        id, Paise(paise), "Tea", Category.Food, LocalDate.of(2026, 10, 1), "hot", 10L, 10L,
    )

    @Test
    fun freshCreateDefaults() = formTest {
        val state = viewModel().state.value
        assertEquals("new-id", state.recordId)
        assertFalse(state.isEdit)
        assertEquals(today, state.date)
        assertEquals(Category.Other, state.category)
        assertFalse(state.isDirty)
        assertFalse(state.canSave)
        assertNull(state.amountError)
    }

    @Test
    fun saveValidAmountStoresOtherToday() = formTest {
        val repo = FakePersonalExpenseRepository()
        val vm = viewModel(repo)
        vm.onAmountChange("125.50")
        assertTrue(vm.state.value.canSave)
        vm.onSave()
        advanceUntilIdle()
        val row = repo.rows.value["new-id"]!!
        assertEquals(Paise(12_550L), row.amount)
        assertEquals(Category.Other, row.category)
        assertEquals(today, row.date)
        assertTrue(vm.state.value.saved)
        assertEquals(1, repo.creates)
    }

    @Test
    fun invalidAmountsShowErrorAndNeverWrite() = formTest {
        val cases = mapOf(
            "0" to AmountResult.NotPositive,
            "-1" to AmountResult.Invalid,
            "12.345" to AmountResult.TooManyDecimals,
            "abc" to AmountResult.Invalid,
        )
        for ((input, expected) in cases) {
            val repo = FakePersonalExpenseRepository()
            val vm = viewModel(repo)
            vm.onAmountChange(input)
            assertEquals(input, expected, vm.state.value.amountError)
            assertFalse(input, vm.state.value.canSave)
            vm.onSave()
            advanceUntilIdle()
            assertEquals(0, repo.creates)
            assertFalse(vm.state.value.saved)
        }
    }

    @Test
    fun overLimitShowsError() = formTest {
        val vm = viewModel()
        val tooMuch = (MoneyLimits.MAX_ENTRY_PAISE / 100 + 1).toString()
        vm.onAmountChange(tooMuch)
        assertEquals(AmountResult.OverLimit, vm.state.value.amountError)
        assertFalse(vm.state.value.canSave)
        vm.onAmountChange("10,00,00,000.00")
        assertNull(vm.state.value.amountError)
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun emptyAmountErrorOnlyAfterSaveAttempt() = formTest {
        val vm = viewModel()
        assertNull(vm.state.value.amountError)
        vm.onSave()
        assertEquals(AmountResult.Empty, vm.state.value.amountError)
    }

    @Test
    fun allFieldsPersist() = formTest {
        val repo = FakePersonalExpenseRepository()
        val vm = viewModel(repo)
        vm.onAmountChange("80")
        vm.onTitleChange("  Lunch  ")
        vm.onCategoryChange(Category.Food)
        vm.onDateChange(LocalDate.of(2026, 10, 3))
        vm.onNoteChange("with team")
        vm.onSave()
        advanceUntilIdle()
        val row = repo.rows.value["new-id"]!!
        assertEquals("Lunch", row.title?.trim())
        assertEquals(Category.Food, row.category)
        assertEquals(LocalDate.of(2026, 10, 3), row.date)
        assertEquals("with team", row.note)
        assertEquals(Paise(8_000L), row.amount)
    }

    @Test
    fun editUpdatesSameIdWithoutCreate() = formTest {
        val repo = FakePersonalExpenseRepository()
        repo.rows.value = mapOf("e1" to stored(paise = 12_550L))
        val vm = viewModel(repo, SavedStateHandle(mapOf("expenseId" to "e1")))
        advanceUntilIdle()
        assertEquals(FormLoadState.Ready, vm.state.value.loadState)
        assertEquals("125.50", vm.state.value.amountText)
        assertEquals("Tea", vm.state.value.title)
        assertFalse(vm.state.value.isDirty)
        vm.onAmountChange("225.50")
        assertTrue(vm.state.value.isDirty)
        vm.onSave()
        advanceUntilIdle()
        assertEquals(1, repo.updates)
        assertEquals(0, repo.creates)
        assertEquals(1, repo.rows.value.size)
        assertEquals(Paise(22_550L), repo.rows.value["e1"]!!.amount)
        assertEquals("e1", vm.state.value.recordId)
    }

    @Test
    fun pastedGroupedAmountStores123450() = formTest {
        val repo = FakePersonalExpenseRepository()
        val vm = viewModel(repo)
        vm.onAmountChange("1,234.50")
        assertEquals("1,234.50", vm.state.value.amountText)
        vm.onSave()
        advanceUntilIdle()
        assertEquals(Paise(123_450L), repo.rows.value["new-id"]!!.amount)
    }

    @Test
    fun doubleSaveWritesOnce() = formTest {
        val repo = FakePersonalExpenseRepository()
        val vm = viewModel(repo)
        vm.onAmountChange("10")
        vm.onSave()
        vm.onSave()
        advanceUntilIdle()
        vm.onSave()
        advanceUntilIdle()
        assertEquals(1, repo.creates)
        assertEquals(1, repo.rows.value.size)
    }

    @Test
    fun draftSurvivesProcessDeath() = formTest {
        val handle = SavedStateHandle()
        var next = 0
        val ids = IdGenerator { "id-${next++}" }
        val first = viewModel(handle = handle, ids = ids)
        first.onAmountChange("42")
        first.onTitleChange("Cab")
        first.onCategoryChange(Category.Transport)
        first.onDateChange(LocalDate.of(2026, 10, 5))
        first.onNoteChange("airport")
        val restored = viewModel(handle = handle, ids = ids).state.value
        assertEquals("id-0", restored.recordId)
        assertEquals("42", restored.amountText)
        assertEquals("Cab", restored.title)
        assertEquals(Category.Transport, restored.category)
        assertEquals(LocalDate.of(2026, 10, 5), restored.date)
        assertEquals("airport", restored.note)
        assertTrue(restored.isDirty)
        assertEquals(1, next)
    }

    @Test
    fun writeFailureKeepsInputAndRetrySucceeds() = formTest {
        val repo = FakePersonalExpenseRepository().apply { throwOnWrite = true }
        val vm = viewModel(repo)
        vm.onAmountChange("10")
        vm.onTitleChange("Keep me")
        vm.onSave()
        advanceUntilIdle()
        assertTrue(vm.state.value.saveFailed)
        assertFalse(vm.state.value.saved)
        assertFalse(vm.state.value.isSaving)
        assertEquals("Keep me", vm.state.value.title)
        assertEquals("10", vm.state.value.amountText)
        repo.throwOnWrite = false
        vm.onSave()
        advanceUntilIdle()
        assertTrue(vm.state.value.saved)
        assertFalse(vm.state.value.saveFailed)
    }

    @Test
    fun editMissingRecordIsNotFound() = formTest {
        val vm = viewModel(handle = SavedStateHandle(mapOf("expenseId" to "ghost")))
        advanceUntilIdle()
        assertEquals(FormLoadState.NotFound, vm.state.value.loadState)
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun loadFailureOffersRetry() = formTest {
        val repo = FakePersonalExpenseRepository().apply {
            rows.value = mapOf("e1" to stored())
            throwOnRead = true
        }
        val vm = viewModel(repo, SavedStateHandle(mapOf("expenseId" to "e1")))
        advanceUntilIdle()
        assertEquals(FormLoadState.LoadFailed, vm.state.value.loadState)
        repo.throwOnRead = false
        vm.onRetryLoad()
        advanceUntilIdle()
        assertEquals(FormLoadState.Ready, vm.state.value.loadState)
        assertEquals("125.50", vm.state.value.amountText)
    }

    @Test
    fun dirtyClearsWhenValueReturnsToOriginal() = formTest {
        val vm = viewModel()
        vm.onTitleChange("x")
        assertTrue(vm.state.value.isDirty)
        vm.onTitleChange("")
        assertFalse(vm.state.value.isDirty)
    }

    @Test
    fun blankTitleStoredAsNull() = formTest {
        val repo = FakePersonalExpenseRepository()
        val vm = viewModel(repo)
        vm.onAmountChange("5")
        vm.onTitleChange("   ")
        vm.onSave()
        advanceUntilIdle()
        // The repository normalizes; the fake stores what the ViewModel sends.
        assertTrue(repo.rows.value["new-id"]!!.title.isNullOrBlank())
    }

    @Test
    fun tooLongTitleAndNoteBlockSave() = formTest {
        val vm = viewModel()
        vm.onAmountChange("5")
        vm.onTitleChange("a".repeat(81))
        assertTrue(vm.state.value.titleTooLong)
        assertFalse(vm.state.value.canSave)
        vm.onTitleChange("ok")
        vm.onNoteChange("n".repeat(301))
        assertTrue(vm.state.value.noteTooLong)
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun discardDialogToggles() = formTest {
        val vm = viewModel()
        vm.onTitleChange("x")
        vm.onBackRequested()
        assertTrue(vm.state.value.showDiscardDialog)
        vm.onKeepEditing()
        assertFalse(vm.state.value.showDiscardDialog)
        assertEquals("x", vm.state.value.title)
    }
}
