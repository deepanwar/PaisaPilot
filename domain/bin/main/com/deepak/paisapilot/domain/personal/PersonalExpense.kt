package com.deepak.paisapilot.domain.personal

import com.deepak.paisapilot.domain.money.Paise
import java.time.LocalDate

data class PersonalExpense(
    val id: String,
    val amount: Paise,
    val title: String?,
    val category: Category,
    val date: LocalDate,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
