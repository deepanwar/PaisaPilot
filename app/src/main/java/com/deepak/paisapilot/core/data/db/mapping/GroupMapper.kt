package com.deepak.paisapilot.core.data.db.mapping

import com.deepak.paisapilot.core.data.db.entity.ExpenseGroupEntity
import com.deepak.paisapilot.core.data.db.entity.MemberEntity
import com.deepak.paisapilot.domain.group.Group
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.normalizeMemberName

fun ExpenseGroupEntity.toDomain(): Group = Group(id, name, createdAt, archivedAt)

fun MemberEntity.toDomain(): Member = Member(id, groupId, name, isLocalOwner, active)

fun Member.toEntity(): MemberEntity = MemberEntity(
    id = id,
    groupId = groupId,
    name = name,
    normalizedName = normalizeMemberName(name),
    isLocalOwner = isLocalOwner,
    active = active,
)
