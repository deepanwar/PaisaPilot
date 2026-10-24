package com.deepak.paisapilot.core.data.repo

import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.group.SharedValidation
import kotlinx.coroutines.flow.Flow

sealed interface SharedWriteResult {
    data object Saved : SharedWriteResult

    /** Same ID already stored: a repeated Save. The first stored values are kept. */
    data object AlreadySaved : SharedWriteResult

    /** Group, or on edit the expense, is missing. */
    data object NotFound : SharedWriteResult

    /** Group is archived: read-only. */
    data object GroupArchived : SharedWriteResult

    /** Payer or a beneficiary is not an eligible member of this group. */
    data object MemberInvalid : SharedWriteResult

    data class Invalid(val error: SharedValidation) : SharedWriteResult
}

interface SharedExpenseRepository {
    /**
     * Validates, then in one transaction checks the group and members and stores the expense with
     * all its shares. Stamps createdAt and updatedAt from the clock. The payer and any new
     * beneficiary must be active members of the group.
     */
    suspend fun create(expense: SharedExpense): SharedWriteResult

    /**
     * Replaces the stored expense and its shares in one transaction. Keeps the ID, group and
     * createdAt. An unchanged payer and existing beneficiaries stay valid even if now inactive.
     */
    suspend fun update(expense: SharedExpense): SharedWriteResult

    /** Shares are removed with the expense. */
    suspend fun delete(id: String): GroupActionResult

    fun observe(id: String): Flow<SharedExpense?>

    /** Newest first: date, createdAt, id (all descending). */
    fun observeGroup(groupId: String): Flow<List<SharedExpense>>
}
