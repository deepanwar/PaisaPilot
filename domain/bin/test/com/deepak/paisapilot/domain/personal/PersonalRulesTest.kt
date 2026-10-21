package com.deepak.paisapilot.domain.personal

import com.deepak.paisapilot.domain.money.MoneyLimits
import com.deepak.paisapilot.domain.money.Paise
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersonalRulesTest {
    private fun expense(
        paise: Long = 12_550L,
        title: String? = null,
        note: String? = null,
        date: LocalDate = LocalDate.of(2026, 10, 20),
    ) = PersonalExpense("id", Paise(paise), title, Category.Other, date, note, 1L, 1L)

    private fun valid(e: PersonalExpense) =
        (validatePersonalExpense(e) as PersonalValidation.Valid).expense

    @Test
    fun amountBounds() {
        assertEquals(PersonalValidation.AmountNotPositive, validatePersonalExpense(expense(paise = 0)))
        assertEquals(PersonalValidation.AmountNotPositive, validatePersonalExpense(expense(paise = -1)))
        valid(expense(paise = 1))
        valid(expense(paise = MoneyLimits.MAX_ENTRY_PAISE))
        assertEquals(
            PersonalValidation.AmountOverLimit,
            validatePersonalExpense(expense(paise = MoneyLimits.MAX_ENTRY_PAISE + 1)),
        )
    }

    @Test
    fun titleIsTrimmedAndBlankBecomesNull() {
        assertEquals("Tea", valid(expense(title = "  Tea  ")).title)
        assertNull(valid(expense(title = "   ")).title)
        assertNull(valid(expense(title = null)).title)
    }

    @Test
    fun titleLengthLimit() {
        valid(expense(title = "a".repeat(MAX_TITLE_LENGTH)))
        assertEquals(
            PersonalValidation.TitleTooLong,
            validatePersonalExpense(expense(title = "a".repeat(MAX_TITLE_LENGTH + 1))),
        )
    }

    @Test
    fun noteLimitAndTrim() {
        valid(expense(note = "n".repeat(MAX_NOTE_LENGTH)))
        assertEquals(
            PersonalValidation.NoteTooLong,
            validatePersonalExpense(expense(note = "n".repeat(MAX_NOTE_LENGTH + 1))),
        )
        assertNull(valid(expense(note = " \n ")).note)
        valid(expense(note = " ${"n".repeat(MAX_NOTE_LENGTH)} "))
    }

    @Test
    fun dateRange() {
        valid(expense(date = LocalDate.of(2000, 1, 1)))
        valid(expense(date = LocalDate.of(2100, 12, 31)))
        assertEquals(
            PersonalValidation.DateOutOfRange,
            validatePersonalExpense(expense(date = LocalDate.of(1999, 12, 31))),
        )
        assertEquals(
            PersonalValidation.DateOutOfRange,
            validatePersonalExpense(expense(date = LocalDate.of(2101, 1, 1))),
        )
    }
}
