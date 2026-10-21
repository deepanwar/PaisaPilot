package com.deepak.paisapilot.domain.personal

import com.deepak.paisapilot.domain.money.MoneyLimits
import com.deepak.paisapilot.domain.time.isAllowedReportingDate

const val MAX_TITLE_LENGTH: Int = 80
const val MAX_NOTE_LENGTH: Int = 300

sealed interface PersonalValidation {
    data class Valid(val expense: PersonalExpense) : PersonalValidation

    data object AmountNotPositive : PersonalValidation

    data object AmountOverLimit : PersonalValidation

    data object TitleTooLong : PersonalValidation

    data object NoteTooLong : PersonalValidation

    data object DateOutOfRange : PersonalValidation
}

/** Trims title and note; a blank one becomes null. Returns the normalized expense when valid. */
fun validatePersonalExpense(expense: PersonalExpense): PersonalValidation {
    val amount = expense.amount.value
    if (amount <= 0L) return PersonalValidation.AmountNotPositive
    if (amount > MoneyLimits.MAX_ENTRY_PAISE) return PersonalValidation.AmountOverLimit
    val title = expense.title?.trim()?.takeIf { it.isNotEmpty() }
    if (title != null && title.length > MAX_TITLE_LENGTH) return PersonalValidation.TitleTooLong
    val note = expense.note?.trim()?.takeIf { it.isNotEmpty() }
    if (note != null && note.length > MAX_NOTE_LENGTH) return PersonalValidation.NoteTooLong
    if (!isAllowedReportingDate(expense.date)) return PersonalValidation.DateOutOfRange
    return PersonalValidation.Valid(expense.copy(title = title, note = note))
}
