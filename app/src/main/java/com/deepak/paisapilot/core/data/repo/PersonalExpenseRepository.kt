package com.deepak.paisapilot.core.data.repo

import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.domain.personal.PersonalValidation
import com.deepak.paisapilot.domain.time.MonthRange
import kotlinx.coroutines.flow.Flow

sealed interface SaveResult {
    data object Created : SaveResult

    /** Same ID already stored: a repeated Save. The first stored values are kept. */
    data object AlreadySaved : SaveResult

    data class Invalid(val error: PersonalValidation) : SaveResult
}

sealed interface UpdateResult {
    data object Updated : UpdateResult

    data object NotFound : UpdateResult

    data class Invalid(val error: PersonalValidation) : UpdateResult
}

interface PersonalExpenseRepository {
    /** Stamps createdAt and updatedAt from the clock; the caller's values are ignored. */
    suspend fun create(expense: PersonalExpense): SaveResult

    /** Keeps the stored createdAt and stamps updatedAt from the clock. */
    suspend fun update(expense: PersonalExpense): UpdateResult

    /** Returns true when a row was removed. */
    suspend fun delete(id: String): Boolean

    /** Re-inserts a deleted record unchanged, timestamps included. Used by Undo. */
    suspend fun restore(expense: PersonalExpense): SaveResult

    fun observe(id: String): Flow<PersonalExpense?>

    /** Every personal expense in stable order: date, createdAt, id (all descending). */
    fun observeAll(): Flow<List<PersonalExpense>>

    fun observeMonth(range: MonthRange): Flow<List<PersonalExpense>>

    fun observeMonthTotal(range: MonthRange): Flow<Paise>
}
