package com.deepak.paisapilot.core.data.repo

import com.deepak.paisapilot.domain.group.Group
import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.NewGroupResult
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.time.MonthRange
import kotlinx.coroutines.flow.Flow

data class GroupSummary(val group: Group, val memberCount: Int)

sealed interface CreateGroupResult {
    data object Created : CreateGroupResult

    /** Same ID already stored: a repeated Save. */
    data object AlreadySaved : CreateGroupResult

    data class Invalid(val error: NewGroupResult) : CreateGroupResult
}

sealed interface AddMemberResult {
    data object Added : AddMemberResult

    data class Invalid(val error: MemberNameResult) : AddMemberResult

    data object GroupNotFound : AddMemberResult

    data object GroupArchived : AddMemberResult
}

sealed interface GroupActionResult {
    data object Done : GroupActionResult

    data object NotFound : GroupActionResult

    /** Group is archived: read-only. */
    data object ReadOnly : GroupActionResult

    /** Rule refused it: owner deactivation, repeat archive, or delete with history. */
    data object NotAllowed : GroupActionResult
}

interface GroupRepository {
    /**
     * Persists the group and all members in one transaction. [ownerName] is the local owner
     * (profile name); the other names are trimmed and validated by the domain rules.
     */
    suspend fun createGroup(
        id: String,
        name: String,
        ownerName: String,
        otherNames: List<String>,
    ): CreateGroupResult

    suspend fun addMember(groupId: String, name: String): AddMemberResult

    /** Members are never hard-deleted; inactive ones keep their history. */
    suspend fun setMemberActive(memberId: String, active: Boolean): GroupActionResult

    suspend fun archive(groupId: String): GroupActionResult

    /** Only for a group with no shared expenses or settlements. */
    suspend fun delete(groupId: String): GroupActionResult

    fun observeGroups(): Flow<List<GroupSummary>>

    fun observeGroup(id: String): Flow<Group?>

    fun observeMembers(groupId: String): Flow<List<Member>>

    /** True when the group has any shared expense or settlement. */
    fun observeHasHistory(groupId: String): Flow<Boolean>

    /** Sum of shared expense amounts in the month, regardless of payer. */
    fun observeMonthTotal(groupId: String, range: MonthRange): Flow<Paise>
}
