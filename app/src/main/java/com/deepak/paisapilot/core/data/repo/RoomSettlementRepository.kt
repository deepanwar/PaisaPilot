package com.deepak.paisapilot.core.data.repo

import android.database.sqlite.SQLiteConstraintException
import com.deepak.paisapilot.core.data.db.TransactionRunner
import com.deepak.paisapilot.core.data.db.dao.GroupDao
import com.deepak.paisapilot.core.data.db.dao.SettlementDao
import com.deepak.paisapilot.core.data.db.dao.SharedExpenseDao
import com.deepak.paisapilot.core.data.db.mapping.toDomain
import com.deepak.paisapilot.core.data.db.mapping.toEntity
import com.deepak.paisapilot.domain.group.Settlement
import com.deepak.paisapilot.domain.group.SettlementValidation
import com.deepak.paisapilot.domain.group.validateSettlement
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSettlementRepository(
    private val dao: SettlementDao,
    private val expenseDao: SharedExpenseDao,
    private val groupDao: GroupDao,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) : SettlementRepository {

    override suspend fun create(settlement: Settlement): SettlementWriteResult = try {
        transactions.run {
            if (dao.getSettlement(settlement.id) != null) return@run SettlementWriteResult.AlreadySaved
            val blocked = checkGroupAndMembers(settlement, existing = null)
            if (blocked != null) return@run blocked
            val valid = when (val result = validate(settlement, existing = null)) {
                is SettlementValidation.Valid -> result.settlement
                else -> return@run SettlementWriteResult.Invalid(result)
            }
            val now = clock.millis()
            dao.insertSettlement(valid.copy(createdAt = now, updatedAt = now).toEntity())
            SettlementWriteResult.Saved
        }
    } catch (_: SQLiteConstraintException) {
        SettlementWriteResult.AlreadySaved
    }

    override suspend fun update(settlement: Settlement): SettlementWriteResult = transactions.run {
        val stored = dao.getSettlement(settlement.id)?.toDomain() ?: return@run SettlementWriteResult.NotFound
        // Group never changes on edit.
        if (stored.groupId != settlement.groupId) return@run SettlementWriteResult.NotFound
        val blocked = checkGroupAndMembers(settlement, existing = stored)
        if (blocked != null) return@run blocked
        val valid = when (val result = validate(settlement, existing = stored)) {
            is SettlementValidation.Valid -> result.settlement
            else -> return@run SettlementWriteResult.Invalid(result)
        }
        dao.updateSettlement(valid.copy(createdAt = stored.createdAt, updatedAt = clock.millis()).toEntity())
        SettlementWriteResult.Saved
    }

    override suspend fun delete(id: String): GroupActionResult = transactions.run {
        val stored = dao.getSettlement(id) ?: return@run GroupActionResult.NotFound
        val group = groupDao.getGroup(stored.groupId) ?: return@run GroupActionResult.NotFound
        if (group.archivedAt != null) return@run GroupActionResult.ReadOnly
        dao.deleteSettlement(id)
        GroupActionResult.Done
    }

    override fun observe(id: String): Flow<Settlement?> =
        dao.observeSettlement(id).map { it?.toDomain() }

    override fun observeGroup(groupId: String): Flow<List<Settlement>> =
        dao.observeGroupSettlements(groupId).map { list -> list.map { it.toDomain() } }

    /** Recomputes from stored rows; the record being edited is left out of the balances. */
    private suspend fun validate(settlement: Settlement, existing: Settlement?): SettlementValidation {
        val expenses = expenseDao.getGroupExpenses(settlement.groupId).map { expense ->
            expense.toDomain(expenseDao.getShares(expense.id))
        }
        val others = dao.getGroupSettlements(settlement.groupId)
            .filter { it.id != settlement.id }
            .map { it.toDomain() }
        return validateSettlement(settlement, expenses, others, existing)
    }

    /** Returns the refusal, or null when the group is writable and both members are eligible. */
    private suspend fun checkGroupAndMembers(
        settlement: Settlement,
        existing: Settlement?,
    ): SettlementWriteResult? {
        val group = groupDao.getGroup(settlement.groupId) ?: return SettlementWriteResult.NotFound
        if (group.archivedAt != null) return SettlementWriteResult.GroupArchived
        val members = groupDao.getMembers(settlement.groupId).associateBy { it.id }
        val from = members[settlement.fromMemberId] ?: return SettlementWriteResult.MemberInvalid
        val to = members[settlement.toMemberId] ?: return SettlementWriteResult.MemberInvalid
        if (!from.active && settlement.fromMemberId != existing?.fromMemberId) {
            return SettlementWriteResult.MemberInvalid
        }
        if (!to.active && settlement.toMemberId != existing?.toMemberId) {
            return SettlementWriteResult.MemberInvalid
        }
        return null
    }
}
