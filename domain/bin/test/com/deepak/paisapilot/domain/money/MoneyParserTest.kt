package com.deepak.paisapilot.domain.money

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyParserTest {
    @Test
    fun parsesListedAmounts() {
        assertPaise("125.50", 12550)
        assertPaise("125.5", 12550)
        assertPaise("125", 12500)
        assertPaise(".5", 50)
        assertPaise("5.", 500)
        assertPaise("0.01", 1)
        assertPaise(" 125.50 ", 12550)
        assertPaise("1,234.50", 123450)
        assertPaise("1,23,450.50", 12345050)
        assertPaise("0125.50", 12550)
    }

    @Test
    fun rejectsEmptyZeroAndGarbage() {
        assertEquals(AmountResult.Empty, parseInrAmount(""))
        assertEquals(AmountResult.Empty, parseInrAmount("   "))
        assertEquals(AmountResult.NotPositive, parseInrAmount("0"))
        assertEquals(AmountResult.NotPositive, parseInrAmount("0.00"))
        assertEquals(AmountResult.Invalid, parseInrAmount("-1"))
        assertEquals(AmountResult.Invalid, parseInrAmount("abc"))
        assertEquals(AmountResult.Invalid, parseInrAmount("₹5"))
        assertEquals(AmountResult.Invalid, parseInrAmount("१२"))
        assertEquals(AmountResult.TooManyDecimals, parseInrAmount("12.345"))
        assertEquals(AmountResult.Invalid, parseInrAmount("1.2.3"))
        assertEquals(AmountResult.Invalid, parseInrAmount("1,,2"))
        assertEquals(AmountResult.Invalid, parseInrAmount(",12"))
        assertEquals(AmountResult.Invalid, parseInrAmount("12,"))
        assertEquals(AmountResult.Invalid, parseInrAmount("1.2,3"))
    }

    @Test
    fun enforcesTheEntryLimitWithoutThrowing() {
        assertPaise("100000000", MoneyLimits.MAX_ENTRY_PAISE)
        assertPaise("100000000.00", MoneyLimits.MAX_ENTRY_PAISE)
        assertPaise("10,00,00,000.00", MoneyLimits.MAX_ENTRY_PAISE)
        assertEquals(AmountResult.OverLimit, parseInrAmount("100000000.01"))
        assertEquals(AmountResult.OverLimit, parseInrAmount("99999999999999999999"))
    }

    private fun assertPaise(input: String, expected: Long) {
        assertEquals(AmountResult.Success(Paise(expected)), parseInrAmount(input))
    }
}
