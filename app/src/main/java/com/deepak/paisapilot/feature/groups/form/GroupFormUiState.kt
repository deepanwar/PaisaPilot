package com.deepak.paisapilot.feature.groups.form

import com.deepak.paisapilot.domain.group.MemberNameResult

data class GroupFormUiState(
    val name: String = "",
    val ownerName: String = "",
    val members: List<String> = emptyList(),
    val newMember: String = "",
    val nameError: NameError? = null,
    val memberError: MemberNameResult? = null,
    val tooFewMembers: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val isDirty: Boolean = false,
    val createdGroupId: String? = null,
) {
    /** Owner plus the unsaved members. */
    val memberCount: Int get() = 1 + members.size
}

enum class NameError { Blank, TooLong }
