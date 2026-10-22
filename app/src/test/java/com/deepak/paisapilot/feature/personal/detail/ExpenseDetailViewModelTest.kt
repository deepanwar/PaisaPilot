package com.deepak.paisapilot.feature.personal.detail

import androidx.lifecycle.SavedStateHandle
import com.deepak.paisapilot.core.undo.UndoDeleteCoordinator
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.feature.personal.FakePersonalExpenseRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseDetailViewModelTest {
    private val stored = PersonalExpense(
        "e1", Paise(12_550), "Tea", Category.Food, LocalDate.of(2026, 10, 1), "hot", 10L, 20L,
    )

    private fun detailTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun vm(
        repo: FakePersonalExpenseRepository,
        coordinator: UndoDeleteCoordinator = UndoDeleteCoordinator(),
        id: String = "e1",
    ) = ExpenseDetailViewModel(SavedStateHandle(mapOf("expenseId" to id)), repo, coordinator)

    private fun repoWithRow() = FakePersonalExpenseRepository().apply {
        rows.value = mapOf("e1" to stored)
    }

    @Test
    fun loadsContent() = detailTest {
        val viewModel = vm(repoWithRow())
        advanceUntilIdle()
        assertEquals(DetailStatus.Content, viewModel.state.value.status)
        assertEquals(stored, viewModel.state.value.expense)
    }

    @Test
    fun missingRecordIsNotFound() = detailTest {
        val viewModel = vm(repoWithRow(), id = "ghost")
        advanceUntilIdle()
        assertEquals(DetailStatus.NotFound, viewModel.state.value.status)
    }

    @Test
    fun readErrorShowsError() = detailTest {
        val viewModel = vm(repoWithRow().apply { throwOnRead = true })
        advanceUntilIdle()
        assertEquals(DetailStatus.Error, viewModel.state.value.status)
    }

    @Test
    fun deleteConfirmedRemovesRecordOffersUndoAndNeverFlashesNotFound() = detailTest {
        val repo = repoWithRow()
        val coordinator = UndoDeleteCoordinator()
        val viewModel = vm(repo, coordinator)
        advanceUntilIdle()
        viewModel.onDeleteRequested()
        assertTrue(viewModel.state.value.showDeleteDialog)
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state.deleted)
        assertEquals(DetailStatus.Content, state.status)
        assertTrue(repo.rows.value.isEmpty())
        assertEquals(stored, coordinator.pending.value)
    }

    @Test
    fun cancelKeepsRecord() = detailTest {
        val repo = repoWithRow()
        val coordinator = UndoDeleteCoordinator()
        val viewModel = vm(repo, coordinator)
        advanceUntilIdle()
        viewModel.onDeleteRequested()
        viewModel.onDeleteCancelled()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.showDeleteDialog)
        assertFalse(viewModel.state.value.deleted)
        assertEquals(1, repo.rows.value.size)
        assertNull(coordinator.pending.value)
    }

    @Test
    fun deleteFailureKeepsRecordAndReports() = detailTest {
        val repo = repoWithRow().apply { throwOnDelete = true }
        val coordinator = UndoDeleteCoordinator()
        val viewModel = vm(repo, coordinator)
        advanceUntilIdle()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state.deleteFailed)
        assertFalse(state.deleted)
        assertFalse(state.isDeleting)
        assertEquals(1, repo.rows.value.size)
        assertNull(coordinator.pending.value)
        repo.throwOnDelete = false
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.deleted)
    }

    @Test
    fun doubleConfirmDeletesOnce() = detailTest {
        val coordinator = UndoDeleteCoordinator()
        val viewModel = vm(repoWithRow(), coordinator)
        advanceUntilIdle()
        viewModel.onDeleteConfirmed()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()
        assertEquals(stored, coordinator.pending.value)
    }
}
