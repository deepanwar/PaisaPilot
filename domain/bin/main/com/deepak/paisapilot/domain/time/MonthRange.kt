package com.deepak.paisapilot.domain.time

import java.time.LocalDate

data class MonthRange(
    val start: LocalDate,
    val endInclusive: LocalDate,
) {
    fun next(): MonthRange = monthContaining(start.plusMonths(1))

    fun previous(): MonthRange = monthContaining(start.minusMonths(1))
}

fun monthContaining(date: LocalDate): MonthRange {
    val start = date.withDayOfMonth(1)
    return MonthRange(start, start.plusMonths(1).minusDays(1))
}
