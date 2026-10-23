package com.deepak.paisapilot.feature.groups

import com.deepak.paisapilot.core.data.repo.AddMemberResult
import com.deepak.paisapilot.core.data.repo.CreateGroupResult
import com.deepak.paisapilot.core.data.repo.GroupActionResult
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.core.data.repo.GroupSummary
import com.deepak.paisapilot.domain.group.Group
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.domain.group.NewGroupResult
import com.deepak.paisapilot.domain.group.canDeactivate
import com.deepak.paisapilot.domain.group.validateMemberName
import com.deepak.paisapilot.domain.group.validateNewGroup
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.time.MonthRange
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeGroupRepository : GroupRepository {
    val groups = MutableStateFlow<List<Group>>(emptyList())
    val members = MutableStateFlow<List<Member>>(emptyList())
    val hasHistory = MutableStateFlow(false)
    var creates = 0
    var throwOnWrite = false
    private var nextId = 0

    override suspend fun createGroup(
        id: String,
        name: String,
        ownerName: String,
        otherNames: List<String>,
    ): CreateGroupResult {
        creates += 1
        if (throwOnWrite) throw IOException("write failed")
        val valid = when (val r = validateNewGroup(name, ownerName, otherNames)) {
            is NewGroupResult.Valid -> r
            else -> return CreateGroupResult.Invalid(r)
        }
        if (groups.value.any { it.id == id }) return CreateGroupResult.AlreadySaved
        groups.value = groups.value + Group(id, valid.groupName, 0L, null)
        members.value = members.value + valid.memberNames.mapIndexed { i, n ->
            Member("m${nextId++}", id, n, isLocalOwner = i == 0, active = true)
        }
        return CreateGroupResult.Created
    }

    override suspend fun addMember(groupId: String, name: String): AddMemberResult {
        if (throwOnWrite) throw IOException("write failed")
        val group = groups.value.firstOrNull { it.id == groupId } ?: return AddMemberResult.GroupNotFound
        if (group.isArchived) return AddMemberResult.GroupArchived
        return when (val r = validateMemberName(name, members.value.filter { it.groupId == groupId })) {
            is MemberNameResult.Valid -> {
                members.value = members.value + Member("m${nextId++}", groupId, r.name, false, true)
                AddMemberResult.Added
            }
            else -> AddMemberResult.Invalid(r)
        }
    }

    override suspend fun setMemberActive(memberId: String, active: Boolean): GroupActionResult {
        val member = members.value.firstOrNull { it.id == memberId } ?: return GroupActionResult.NotFound
        val group = groups.value.first { it.id == member.groupId }
        if (group.isArchived) return GroupActionResult.ReadOnly
        if (!active && !canDeactivate(member)) return GroupActionResult.NotAllowed
        members.value = members.value.map { if (it.id == memberId) it.copy(active = active) else it }
        return GroupActionResult.Done
    }

    override suspend fun archive(groupId: String): GroupActionResult {
        if (throwOnWrite) throw IOException("write failed")
        val group = groups.value.firstOrNull { it.id == groupId } ?: return GroupActionResult.NotFound
        if (group.isArchived) return GroupActionResult.NotAllowed
        groups.value = groups.value.map { if (it.id == groupId) it.copy(archivedAt = 1L) else it }
        return GroupActionResult.Done
    }

    override suspend fun delete(groupId: String): GroupActionResult {
        if (hasHistory.value) return GroupActionResult.NotAllowed
        groups.value = groups.value.filterNot { it.id == groupId }
        members.value = members.value.filterNot { it.groupId == groupId }
        return GroupActionResult.Done
    }

    override fun observeGroups(): Flow<List<GroupSummary>> = groups.map { list ->
        list.map { g -> GroupSummary(g, members.value.count { it.groupId == g.id }) }
    }

    override fun observeGroup(id: String): Flow<Group?> = groups.map { l -> l.firstOrNull { it.id == id } }

    override fun observeMembers(groupId: String): Flow<List<Member>> =
        members.map { l -> l.filter { it.groupId == groupId } }

    override fun observeHasHistory(groupId: String): Flow<Boolean> = hasHistory

    override fun observeMonthTotal(groupId: String, range: MonthRange): Flow<Paise> =
        MutableStateFlow(Paise(0))
}
