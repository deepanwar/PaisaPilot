package com.deepak.paisapilot.core.undo

import com.deepak.paisapilot.domain.personal.PersonalExpense
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the one just-deleted personal expense in memory so Undo can re-insert it (G3).
 * Lost when the process dies, which the spec allows.
 */
@Singleton
class UndoDeleteCoordinator @Inject constructor() {
    private val _pending = MutableStateFlow<PersonalExpense?>(null)
    val pending: StateFlow<PersonalExpense?> = _pending.asStateFlow()

    fun offer(expense: PersonalExpense) {
        _pending.value = expense
    }

    /** Clears only if [expected] is still the pending snapshot, so a late dismissal cannot drop a newer one. */
    fun clear(expected: PersonalExpense) {
        if (_pending.value == expected) _pending.value = null
    }

    fun take(): PersonalExpense? = _pending.value.also { _pending.value = null }
}
