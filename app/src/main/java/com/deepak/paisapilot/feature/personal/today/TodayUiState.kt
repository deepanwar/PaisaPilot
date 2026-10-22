package com.deepak.paisapilot.feature.personal.today

import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.time.MonthRange
import java.time.LocalDate

data class TodayRowUi(
    val id: String,
    val title: String?,
    val category: Category,
    val date: LocalDate,
    val amount: Paise,
)

sealed interface TodayUiState {
    data object Loading : TodayUiState

    data class Content(
        val month: MonthRange,
        val total: Paise,
        val rows: List<TodayRowUi>,
        val hasMore: Boolean,
        val isCurrentMonth: Boolean,
        val canGoPrevious: Boolean,
        val canGoNext: Boolean,
    ) : TodayUiState

    data class Error(val month: MonthRange) : TodayUiState
}
