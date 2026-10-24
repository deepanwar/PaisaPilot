package com.deepak.paisapilot.core.data.db.mapping

import com.deepak.paisapilot.core.data.db.entity.ExpenseShareEntity
import com.deepak.paisapilot.core.data.db.entity.SharedExpenseEntity
import com.deepak.paisapilot.domain.common.StableIdOrder
import com.deepak.paisapilot.domain.group.Share
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import java.time.LocalDate

fun SharedExpense.toEntity(): SharedExpenseEntity = SharedExpenseEntity(
    id = id,
    groupId = groupId,
    amountPaise = amount.value,
    title = title,
    categoryKey = category.key,
    occurredEpochDay = date.toEpochDay(),
    payerMemberId = payerMemberId,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SharedExpense.toShareEntities(): List<ExpenseShareEntity> =
    shares.map { ExpenseShareEntity(id, it.memberId, it.paise.value) }

fun SharedExpenseEntity.toDomain(shares: List<ExpenseShareEntity>): SharedExpense = SharedExpense(
    id = id,
    groupId = groupId,
    amount = Paise(amountPaise),
    title = title,
    category = Category.fromKey(categoryKey),
    date = LocalDate.ofEpochDay(occurredEpochDay),
    payerMemberId = payerMemberId,
    note = note,
    shares = shares.map { Share(it.memberId, Paise(it.sharePaise)) }
        .sortedWith(compareBy(StableIdOrder) { it.memberId }),
    createdAt = createdAt,
    updatedAt = updatedAt,
)
