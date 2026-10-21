package com.deepak.paisapilot.domain.time

import java.time.LocalDate

private val earliestReportingDate: LocalDate = LocalDate.of(2000, 1, 1)
private val latestReportingDate: LocalDate = LocalDate.of(2100, 12, 31)

fun isAllowedReportingDate(date: LocalDate): Boolean =
    !date.isBefore(earliestReportingDate) && !date.isAfter(latestReportingDate)
