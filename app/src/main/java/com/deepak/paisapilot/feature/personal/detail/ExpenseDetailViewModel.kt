package com.deepak.paisapilot.feature.personal.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.repo.PersonalExpenseRepository
import com.deepak.paisapilot.core.undo.UndoDeleteCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val KEY_EXPENSE_ID = "expenseId"

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PersonalExpenseRepository,
    private val undoCoordinator: UndoDeleteCoordinator,
) : ViewModel() {
    private val expenseId: String = checkNotNull(savedStateHandle[KEY_EXPENSE_ID])
    private val _state = MutableStateFlow(ExpenseDetailUiState())
    val state: StateFlow<ExpenseDetailUiState> = _state.asStateFlow()

    /** Set before a delete starts so the record vanishing never shows "unavailable". */
    private var deleteStarted = false

    init {
        viewModelScope.launch {
            repository.observe(expenseId)
                .catch { _state.update { it.copy(status = DetailStatus.Error) } }
                .collect { expense ->
                    _state.update { current ->
                        when {
                            expense != null -> current.copy(status = DetailStatus.Content, expense = expense)
                            deleteStarted -> current
                            else -> current.copy(status = DetailStatus.NotFound, expense = null)
                        }
                    }
                }
        }
    }

    fun onDeleteRequested() {
        if (_state.value.expense == null) return
        _state.update { it.copy(showDeleteDialog = true, deleteFailed = false) }
    }

    fun onDeleteCancelled() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    fun onDeleteConfirmed() {
        val current = _state.value
        val snapshot = current.expense ?: return
        if (current.isDeleting || current.deleted) return
        deleteStarted = true
        _state.update { it.copy(showDeleteDialog = false, isDeleting = true) }
        viewModelScope.launch {
            try {
                if (repository.delete(expenseId)) undoCoordinator.offer(snapshot)
                _state.update { it.copy(isDeleting = false, deleted = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                deleteStarted = false
                _state.update { it.copy(isDeleting = false, deleteFailed = true) }
            }
        }
    }
}
