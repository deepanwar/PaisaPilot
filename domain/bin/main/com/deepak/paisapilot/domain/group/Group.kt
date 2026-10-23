package com.deepak.paisapilot.domain.group

data class Group(
    val id: String,
    val name: String,
    val createdAt: Long,
    val archivedAt: Long?,
) {
    val isArchived: Boolean get() = archivedAt != null
}

data class Member(
    val id: String,
    val groupId: String,
    val name: String,
    val isLocalOwner: Boolean,
    val active: Boolean,
)
