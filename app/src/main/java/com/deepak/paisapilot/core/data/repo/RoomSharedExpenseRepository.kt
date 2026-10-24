package com.deepak.paisapilot.core.data.repo

import android.database.sqlite.SQLiteConstraintException
import com.deepak.paisapilot.core.data.db.TransactionRunner
import com.deepak.paisapilot.core.data.db.dao.GroupDao
import com.deepak.paisapilot.core.data.db.dao.SharedExpenseDao
import com.deepak.paisapilot.core.data.db.mapping.toDomain
import com.deepak.paisapilot.core.data.db.mapping.toEntity
import com.deepak.paisapilot.core.data.db.mapping.toShareEntities
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.group.SharedValidation
import com.deepak.paisapilot.domain.group.validateSharedExpense
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomSharedExpenseRepository(
    private val dao: SharedExpenseDao,
    private val groupDao: GroupDao,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) : SharedExpenseRepository {

    override suspend fun create(expense: SharedExpense): SharedWriteResult {
        val valid = when (val result = validateSharedExpense(expense)) {
            is SharedValidation.Valid -> result.expense
            else -> return SharedWriteResult.Invalid(result)
        }
        return try {
            transactions.run {
                if (dao.getExpense(valid.id) != null) return@run SharedWriteResult.AlreadySaved
                val blocked = checkGroupAndMembers(valid, existing = null)
                if (blocked != null) return@run blocked
                val now = clock.millis()
                val stamped = valid.copy(createdAt = now, updatedAt = now)
                dao.insertExpense(stamped.toEntity())
                dao.insertShares(stamped.toShareEntities())
                SharedWriteResult.Saved
            }
        } catch (_: SQLiteConstraintException) {
            SharedWriteResult.AlreadySaved
        }
    }

    override suspend fun update(expense: SharedExpense): SharedWriteResult {
        val valid = when (val result = validateSharedExpense(expense)) {
            is SharedValidation.Valid -> result.expense
            else -> return SharedWriteResult.Invalid(result)
        }
        return transactions.run {
            val stored = dao.getExpense(valid.id) ?: return@run SharedWriteResult.NotFound
            // Group never changes on edit.
            if (stored.groupId != valid.groupId) return@run SharedWriteResult.NotFound
            val existing = ExistingRefs(
                payerId = stored.payerMemberId,
                shareMemberIds = dao.getShares(valid.id).map { it.memberId }.toSet(),
            )
            val blocked = checkGroupAndMembers(valid, existing)
            if (blocked != null) return@run blocked
            val updated = valid.copy(createdAt = stored.createdAt, updatedAt = clock.millis())
            dao.updateExpense(updated.toEntity())
            dao.deleteShares(valid.id)
            dao.insertShares(updated.toShareEntities())
            SharedWriteResult.Saved
        }
    }

    override suspend fun delete(id: String): GroupActionResult = transactions.run {
        val stored = dao.getExpense(id) ?: return@run GroupActionResult.NotFound
        val group = groupDao.getGroup(stored.groupId) ?: return@run GroupActionResult.NotFound
        if (group.archivedAt != null) return@run GroupActionResult.ReadOnly
        dao.deleteExpense(id)
        GroupActionResult.Done
    }

    override fun observe(id: String): Flow<SharedExpense?> =
        combine(dao.observeExpense(id), dao.observeShares(id)) { expense, shares ->
            expense?.toDomain(shares)
        }

    override fun observeGroup(groupId: String): Flow<List<SharedExpense>> =
        combine(dao.observeGroupExpenses(groupId), dao.observeGroupShares(groupId)) { expenses, shares ->
            val byExpense = shares.groupBy { it.sharedExpenseId }
            expenses.map { it.toDomain(byExpense[it.id].orEmpty()) }
        }

    private class ExistingRefs(val payerId: String, val shareMemberIds: Set<String>)

    /** Returns the refusal, or null when the group is writable and every member reference is valid. */
    private suspend fun checkGroupAndMembers(
        expense: SharedExpense,
        existing: ExistingRefs?,
    ): SharedWriteResult? {
        val group = groupDao.getGroup(expense.groupId) ?: return SharedWriteResult.NotFound
        if (group.archivedAt != null) return SharedWriteResult.GroupArchived
        val members = groupDao.getMembers(expense.groupId).associateBy { it.id }
        val payer = members[expense.payerMemberId] ?: return SharedWriteResult.MemberInvalid
        if (!payer.active && expense.payerMemberId != existing?.payerId) {
            return SharedWriteResult.MemberInvalid
        }
        for (share in expense.shares) {
            val member = members[share.memberId] ?: return SharedWriteResult.MemberInvalid
            val retained = existing != null && share.memberId in existing.shareMemberIds
            if (!member.active && !retained) return SharedWriteResult.MemberInvalid
        }
        return null
    }
}
