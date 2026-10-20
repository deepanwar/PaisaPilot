package com.deepak.paisapilot.domain.money

fun parseInrAmount(input: String): AmountResult {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return AmountResult.Empty
    if (trimmed.any { !it.isAsciiDigit() && it != ',' && it != '.' }) return AmountResult.Invalid

    val dot = trimmed.indexOf('.')
    if (dot != trimmed.lastIndexOf('.')) return AmountResult.Invalid

    val rupeeText = if (dot < 0) trimmed else trimmed.substring(0, dot)
    val fractionText = if (dot < 0) "" else trimmed.substring(dot + 1)
    if (',' in fractionText) return AmountResult.Invalid
    if (fractionText.length > 2) return AmountResult.TooManyDecimals
    if (rupeeText.startsWith(',') || rupeeText.endsWith(',') || ",," in rupeeText) {
        return AmountResult.Invalid
    }

    val rupeeDigits = rupeeText.replace(",", "")
    if (rupeeDigits.any { !it.isAsciiDigit() }) return AmountResult.Invalid
    if (rupeeDigits.isEmpty() && fractionText.isEmpty()) return AmountResult.Invalid

    val rupees = if (rupeeDigits.isEmpty()) 0L else parsePositiveDigits(rupeeDigits) ?: return AmountResult.OverLimit
    val fractionDigits = fractionText.length
    val fraction = when (fractionDigits) {
        0 -> 0L
        1 -> (fractionText[0] - '0') * 10L
        else -> (fractionText[0] - '0') * 10L + (fractionText[1] - '0')
    }

    val paise = try {
        Math.addExact(Math.multiplyExact(rupees, 100L), fraction)
    } catch (_: ArithmeticException) {
        return AmountResult.OverLimit
    }
    if (paise > MoneyLimits.MAX_ENTRY_PAISE) return AmountResult.OverLimit
    if (paise == 0L) return AmountResult.NotPositive
    return AmountResult.Success(Paise(paise))
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

private fun parsePositiveDigits(digits: String): Long? {
    var value = 0L
    for (char in digits) {
        val digit = char.code - '0'.code
        if (value > (Long.MAX_VALUE - digit) / 10) return null
        value = value * 10 + digit
    }
    return value
}
