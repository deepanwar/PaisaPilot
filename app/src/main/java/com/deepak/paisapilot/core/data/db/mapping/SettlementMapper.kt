package com.deepak.paisapilot.core.data.db.mapping

import com.deepak.paisapilot.core.data.db.entity.SettlementEntity
import com.deepak.paisapilot.domain.group.Settlement
import com.deepak.paisapilot.domain.money.Paise

fun Settlement.toEntity(): SettlementEntity = SettlementEntity(
    id = id,
    groupId = groupId,
    fromMemberId = fromMemberId,
    toMemberId = toMemberId,
    amountPaise = amount.value,
    occurredEpochDay = occurredEpochDay,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SettlementEntity.toDomain(): Settlement = Settlement(
    id = id,
    groupId = groupId,
    fromMemberId = fromMemberId,
    toMemberId = toMemberId,
    amount = Paise(amountPaise),
    occurredEpochDay = occurredEpochDay,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
