package com.deepak.paisapilot.domain.group

import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import java.time.LocalDate

data class SharedExpense(
    val id: String,
    val groupId: String,
    val amount: Paise,
    val title: String?,
    val category: Category,
    val date: LocalDate,
    val payerMemberId: String,
    val note: String?,
    val shares: List<Share>,
    val createdAt: Long,
    val updatedAt: Long,
)
