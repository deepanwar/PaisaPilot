package com.deepak.paisapilot.domain.money

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class InrFormatterTest {
    private lateinit var previous: Locale

    @Before
    fun useGermanDefaultLocale() {
        previous = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(previous)
    }

    @Test
    fun formatsWithIndianGroupingAndTwoDecimals() {
        assertEquals("₹125.50", formatInr(Paise(12550)))
        assertEquals("₹125.00", formatInr(Paise(12500)))
        assertEquals("₹1,234.50", formatInr(Paise(123450)))
        assertEquals("₹1,00,000.00", formatInr(Paise(10000000)))
        assertEquals("₹0.01", formatInr(Paise(1)))
        assertEquals("₹10,00,00,000.00", formatInr(Paise(MoneyLimits.MAX_ENTRY_PAISE)))
    }
}
