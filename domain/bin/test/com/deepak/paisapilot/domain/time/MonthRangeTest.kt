package com.deepak.paisapilot.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MonthRangeTest {
    @Test
    fun october2026Has31Days() {
        val range = monthContaining(LocalDate.of(2026, 10, 20))
        assertEquals(LocalDate.of(2026, 10, 1), range.start)
        assertEquals(LocalDate.of(2026, 10, 31), range.endInclusive)
    }

    @Test
    fun leapFebruary2028Has29Days() {
        val range = monthContaining(LocalDate.of(2028, 2, 15))
        assertEquals(LocalDate.of(2028, 2, 1), range.start)
        assertEquals(LocalDate.of(2028, 2, 29), range.endInclusive)
    }

    @Test
    fun decemberNextIsJanuary() {
        val next = monthContaining(LocalDate.of(2026, 12, 20)).next()
        assertEquals(LocalDate.of(2027, 1, 1), next.start)
        assertEquals(LocalDate.of(2027, 1, 31), next.endInclusive)
    }

    @Test
    fun januaryPreviousIsDecember() {
        val previous = monthContaining(LocalDate.of(2027, 1, 4)).previous()
        assertEquals(LocalDate.of(2026, 12, 1), previous.start)
        assertEquals(LocalDate.of(2026, 12, 31), previous.endInclusive)
    }
}
