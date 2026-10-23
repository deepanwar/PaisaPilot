package com.deepak.paisapilot.core.data.repo

import android.database.sqlite.SQLiteConstraintException
import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.db.TransactionRunner
import com.deepak.paisapilot.core.data.db.dao.GroupDao
import com.deepak.paisapilot.core.data.db.entity.ExpenseGroupEntity
import com.deepak.paisapilot.core.data.db.mapping.toDomain
import com.deepak.paisapilot.core.data.db.mapping.toEntity
import com.deepak.paisapilot.domain.group.Group
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.domain.group.NewGroupResult
import com.deepak.paisapilot.domain.group.canDeactivate
import com.deepak.paisapilot.domain.group.canDeleteGroup
import com.deepak.paisapilot.domain.group.validateMemberName
import com.deepak.paisapilot.domain.group.validateNewGroup
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.time.MonthRange
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomGroupRepository(
    private val dao: GroupDao,
    private val transactions: TransactionRunner,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : GroupRepository {

    override suspend fun createGroup(
        id: String,
        name: String,
        ownerName: String,
        otherNames: List<String>,
    ): CreateGroupResult {
        val valid = when (val result = validateNewGroup(name, ownerName, otherNames)) {
            is NewGroupResult.Valid -> result
            else -> return CreateGroupResult.Invalid(result)
        }
        return try {
            transactions.run {
                dao.insertGroup(ExpenseGroupEntity(id, valid.groupName, clock.millis(), null))
                valid.memberNames.forEachIndexed { index, memberName ->
                    dao.insertMember(
                        Member(idGenerator.newId(), id, memberName, isLocalOwner = index == 0, active = true)
                            .toEntity(),
                    )
                }
            }
            CreateGroupResult.Created
        } catch (_: SQLiteConstraintException) {
            CreateGroupResult.AlreadySaved
        }
    }

    override suspend fun addMember(groupId: String, name: String): AddMemberResult =
        transactions.run {
            val group = dao.getGroup(groupId) ?: return@run AddMemberResult.GroupNotFound
            if (group.archivedAt != null) return@run AddMemberResult.GroupArchived
            val existing = dao.getMembers(groupId).map { it.toDomain() }
            when (val result = validateMemberName(name, existing)) {
                is MemberNameResult.Valid -> {
                    dao.insertMember(
                        Member(idGenerator.newId(), groupId, result.name, isLocalOwner = false, active = true)
                            .toEntity(),
                    )
                    AddMemberResult.Added
                }
                else -> AddMemberResult.Invalid(result)
            }
        }

    override suspend fun setMemberActive(memberId: String, active: Boolean): GroupActionResult =
        transactions.run {
            val member = dao.getMember(memberId) ?: return@run GroupActionResult.NotFound
            val group = dao.getGroup(member.groupId) ?: return@run GroupActionResult.NotFound
            if (group.archivedAt != null) return@run GroupActionResult.ReadOnly
            if (!active && !canDeactivate(member.toDomain())) return@run GroupActionResult.NotAllowed
            dao.setMemberActive(memberId, active)
            GroupActionResult.Done
        }

    override suspend fun archive(groupId: String): GroupActionResult = transactions.run {
        val group = dao.getGroup(groupId) ?: return@run GroupActionResult.NotFound
        if (group.archivedAt != null) return@run GroupActionResult.NotAllowed
        dao.setArchivedAt(groupId, clock.millis())
        GroupActionResult.Done
    }

    override suspend fun delete(groupId: String): GroupActionResult = transactions.run {
        dao.getGroup(groupId) ?: return@run GroupActionResult.NotFound
        if (!canDeleteGroup(hasHistory = dao.countHistory(groupId) > 0)) {
            return@run GroupActionResult.NotAllowed
        }
        // member.group_id has no cascade; remove members first.
        dao.deleteMembers(groupId)
        dao.deleteGroup(groupId)
        GroupActionResult.Done
    }

    override fun observeGroups(): Flow<List<GroupSummary>> =
        combine(dao.observeGroups(), dao.observeAllMembers()) { groups, members ->
            val counts = members.groupingBy { it.groupId }.eachCount()
            groups.map { GroupSummary(it.toDomain(), counts[it.id] ?: 0) }
        }

    override fun observeGroup(id: String): Flow<Group?> = dao.observeGroup(id).map { it?.toDomain() }

    override fun observeMembers(groupId: String): Flow<List<Member>> =
        dao.observeMembers(groupId).map { list -> list.map { it.toDomain() } }

    override fun observeHasHistory(groupId: String): Flow<Boolean> =
        dao.observeHistoryCount(groupId).map { it > 0 }

    override fun observeMonthTotal(groupId: String, range: MonthRange): Flow<Paise> =
        dao.observeMonthTotal(groupId, range.start.toEpochDay(), range.endInclusive.toEpochDay())
            .map(::Paise)
}
