package com.deepak.paisapilot.domain.time

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateRulesTest {
    @Test
    fun allowsTheInclusiveBounds() {
        assertTrue(isAllowedReportingDate(LocalDate.of(2000, 1, 1)))
        assertTrue(isAllowedReportingDate(LocalDate.of(2100, 12, 31)))
    }

    @Test
    fun rejectsTheDaysOutsideTheBounds() {
        assertFalse(isAllowedReportingDate(LocalDate.of(1999, 12, 31)))
        assertFalse(isAllowedReportingDate(LocalDate.of(2101, 1, 1)))
    }
}
