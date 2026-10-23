package com.deepak.paisapilot.domain.group

const val MAX_GROUP_NAME_LENGTH: Int = 60
const val MAX_MEMBER_NAME_LENGTH: Int = 40
const val MAX_MEMBERS: Int = 30

sealed interface GroupNameResult {
    data class Valid(val name: String) : GroupNameResult

    data object Blank : GroupNameResult

    data object TooLong : GroupNameResult
}

sealed interface MemberNameResult {
    data class Valid(val name: String) : MemberNameResult

    data object Blank : MemberNameResult

    data object TooLong : MemberNameResult

    data object Duplicate : MemberNameResult

    data object GroupFull : MemberNameResult
}

sealed interface NewGroupResult {
    /** [memberNames] lists the owner first, then the others in entry order, all trimmed. */
    data class Valid(val groupName: String, val memberNames: List<String>) : NewGroupResult

    data object GroupNameBlank : NewGroupResult

    data object GroupNameTooLong : NewGroupResult

    data object MemberNameBlank : NewGroupResult

    data object MemberNameTooLong : NewGroupResult

    data object DuplicateMember : NewGroupResult

    data object TooFewMembers : NewGroupResult

    data object TooManyMembers : NewGroupResult
}

/** The one definition behind `member.normalized_name`: trim, collapse spaces, lowercase. */
fun normalizeMemberName(name: String): String =
    name.trim().replace(Regex("\\s+"), " ").lowercase()

fun validateGroupName(input: String): GroupNameResult {
    val name = input.trim()
    if (name.isEmpty()) return GroupNameResult.Blank
    if (name.length > MAX_GROUP_NAME_LENGTH) return GroupNameResult.TooLong
    return GroupNameResult.Valid(name)
}

/** Names are unique across all members of a group, active or not (G6). */
fun validateMemberName(input: String, existing: List<Member>): MemberNameResult {
    val name = input.trim()
    if (name.isEmpty()) return MemberNameResult.Blank
    if (name.length > MAX_MEMBER_NAME_LENGTH) return MemberNameResult.TooLong
    if (existing.size >= MAX_MEMBERS) return MemberNameResult.GroupFull
    val normalized = normalizeMemberName(name)
    if (existing.any { normalizeMemberName(it.name) == normalized }) return MemberNameResult.Duplicate
    return MemberNameResult.Valid(name)
}

fun validateNewGroup(groupName: String, ownerName: String, otherNames: List<String>): NewGroupResult {
    val name = when (val result = validateGroupName(groupName)) {
        is GroupNameResult.Valid -> result.name
        GroupNameResult.Blank -> return NewGroupResult.GroupNameBlank
        GroupNameResult.TooLong -> return NewGroupResult.GroupNameTooLong
    }
    val members = listOf(ownerName.trim()) + otherNames.map { it.trim() }
    if (members.size < 2) return NewGroupResult.TooFewMembers
    if (members.size > MAX_MEMBERS) return NewGroupResult.TooManyMembers
    if (members.any { it.isEmpty() }) return NewGroupResult.MemberNameBlank
    if (members.any { it.length > MAX_MEMBER_NAME_LENGTH }) return NewGroupResult.MemberNameTooLong
    if (members.map(::normalizeMemberName).toSet().size != members.size) {
        return NewGroupResult.DuplicateMember
    }
    return NewGroupResult.Valid(name, members)
}

/** New shared expenses need a payer and at least one other person. */
fun canRecordSharedExpense(members: List<Member>): Boolean = members.count { it.active } >= 2

fun canDeleteGroup(hasHistory: Boolean): Boolean = !hasHistory

fun canDeactivate(member: Member): Boolean = !member.isLocalOwner
