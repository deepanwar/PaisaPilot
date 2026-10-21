package com.deepak.paisapilot.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun localDateFromUtcPickerMillis(utcMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcMillis).atOffset(ZoneOffset.UTC).toLocalDate()
