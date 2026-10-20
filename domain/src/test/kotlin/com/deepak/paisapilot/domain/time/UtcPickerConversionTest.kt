package com.deepak.paisapilot.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

class UtcPickerConversionTest {
    @Test
    fun utcMidnightStaysOnThatDateInEveryZone() {
        val date = LocalDate.of(2026, 10, 20)
        val utcMidnight = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val zones = listOf(
            "America/Los_Angeles",
            "Asia/Kolkata",
            "Pacific/Kiritimati",
        )
        val previous = TimeZone.getDefault()
        try {
            for (zone in zones) {
                TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zone)))
                assertEquals(date, localDateFromUtcPickerMillis(utcMidnight))
            }
        } finally {
            TimeZone.setDefault(previous)
        }
    }
}
