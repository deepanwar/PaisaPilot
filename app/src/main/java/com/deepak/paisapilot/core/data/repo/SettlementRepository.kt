package com.deepak.paisapilot.core.data.repo

import com.deepak.paisapilot.domain.group.Settlement
import com.deepak.paisapilot.domain.group.SettlementValidation
import kotlinx.coroutines.flow.Flow

sealed interface SettlementWriteResult {
    data object Saved : SettlementWriteResult

    /** Same ID already stored: a repeated Save. The first stored values are kept. */
    data object AlreadySaved : SettlementWriteResult

    /** Group, or on edit the settlement, is missing. */
    data object NotFound : SettlementWriteResult

    /** Group is archived: read-only. */
    data object GroupArchived : SettlementWriteResult

    /** Payer or receiver is not an eligible member of this group. */
    data object MemberInvalid : SettlementWriteResult

    data class Invalid(val error: SettlementValidation) : SettlementWriteResult
}

interface SettlementRepository {
    /**
     * In one transaction: recomputes the group's suggestions from stored expenses and settlements,
     * validates the pair and amount against them, then stores the record. Stamps createdAt and
     * updatedAt from the clock. Both members must be active members of the group.
     */
    suspend fun create(settlement: Settlement): SettlementWriteResult

    /**
     * Replaces the stored settlement in one transaction, validating against balances that exclude
     * its own old amount. Keeps the ID, group and createdAt. An unchanged member stays valid even if
     * now inactive.
     */
    suspend fun update(settlement: Settlement): SettlementWriteResult

    suspend fun delete(id: String): GroupActionResult

    fun observe(id: String): Flow<Settlement?>

    /** Newest first: date, createdAt, id (all descending). */
    fun observeGroup(groupId: String): Flow<List<Settlement>>
}
