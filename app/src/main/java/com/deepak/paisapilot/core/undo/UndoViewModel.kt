package com.deepak.paisapilot.core.undo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.repo.PersonalExpenseRepository
import com.deepak.paisapilot.core.data.repo.SaveResult
import com.deepak.paisapilot.domain.personal.PersonalExpense
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UndoViewModel @Inject constructor(
    private val coordinator: UndoDeleteCoordinator,
    private val repository: PersonalExpenseRepository,
) : ViewModel() {
    val pending: StateFlow<PersonalExpense?> = coordinator.pending
    private val _restoreFailed = MutableStateFlow(false)
    val restoreFailed: StateFlow<Boolean> = _restoreFailed.asStateFlow()

    fun undo() {
        val expense = coordinator.take() ?: return
        viewModelScope.launch {
            try {
                val result = repository.restore(expense)
                if (result is SaveResult.Invalid) _restoreFailed.value = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _restoreFailed.value = true
            }
        }
    }

    fun dismiss(expense: PersonalExpense) = coordinator.clear(expense)

    fun onRestoreFailedShown() {
        _restoreFailed.value = false
    }
}
