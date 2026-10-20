package com.deepak.paisapilot.domain.money

sealed interface AmountResult {
    data class Success(val paise: Paise) : AmountResult

    data object Empty : AmountResult

    data object Invalid : AmountResult

    data object TooManyDecimals : AmountResult

    data object NotPositive : AmountResult

    data object OverLimit : AmountResult
}
