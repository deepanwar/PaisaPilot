package com.deepak.paisapilot.core.data.db.mapping

import com.deepak.paisapilot.core.data.db.entity.PersonalExpenseEntity
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import java.time.LocalDate

fun PersonalExpense.toEntity(): PersonalExpenseEntity = PersonalExpenseEntity(
    id = id,
    amountPaise = amount.value,
    title = title,
    categoryKey = category.key,
    occurredEpochDay = date.toEpochDay(),
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PersonalExpenseEntity.toDomain(): PersonalExpense = PersonalExpense(
    id = id,
    amount = Paise(amountPaise),
    title = title,
    category = Category.fromKey(categoryKey),
    date = LocalDate.ofEpochDay(occurredEpochDay),
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
