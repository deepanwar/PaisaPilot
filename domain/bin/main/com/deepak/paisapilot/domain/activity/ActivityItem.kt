package com.deepak.paisapilot.domain.activity

import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.PersonalExpense
import java.time.LocalDate

/** One entry in the Activity feed. Shared expenses and settlements join in M3/M4. */
sealed interface ActivityItem {
    val id: String
    val date: LocalDate
    val createdAt: Long
    val amount: Paise

    data class Personal(val expense: PersonalExpense) : ActivityItem {
        override val id: String get() = expense.id
        override val date: LocalDate get() = expense.date
        override val createdAt: Long get() = expense.createdAt
        override val amount: Paise get() = expense.amount
    }
}
