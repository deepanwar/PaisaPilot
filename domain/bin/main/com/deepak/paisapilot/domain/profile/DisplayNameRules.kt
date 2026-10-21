package com.deepak.paisapilot.domain.profile

const val MAX_DISPLAY_NAME_LENGTH: Int = 40
const val SKIPPED_DISPLAY_NAME: String = "Me"

sealed interface DisplayNameResult {
    data class Valid(val name: String) : DisplayNameResult

    data object Blank : DisplayNameResult

    data object TooLong : DisplayNameResult
}

enum class NameSubmit {
    Start,
    Skip,
}

fun resolveDisplayName(input: String, submit: NameSubmit): DisplayNameResult {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) {
        return if (submit == NameSubmit.Skip) {
            DisplayNameResult.Valid(SKIPPED_DISPLAY_NAME)
        } else {
            DisplayNameResult.Blank
        }
    }
    if (trimmed.length > MAX_DISPLAY_NAME_LENGTH) return DisplayNameResult.TooLong
    return DisplayNameResult.Valid(trimmed)
}
