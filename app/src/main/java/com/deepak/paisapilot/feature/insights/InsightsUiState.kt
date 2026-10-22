package com.deepak.paisapilot.feature.insights

import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.time.MonthRange
import java.time.LocalDate

data class CategoryRowUi(
    val category: Category,
    val amount: Paise,
    val percent: Int,
    /** Share of the month's total, 0..1, for the decorative bar only. */
    val fraction: Float,
)

data class RecentRowUi(
    val id: String,
    val title: String?,
    val category: Category,
    val date: LocalDate,
    val amount: Paise,
)

sealed interface InsightsUiState {
    data object Loading : InsightsUiState

    data class Content(
        val month: MonthRange,
        val isCurrentMonth: Boolean,
        val total: Paise,
        val count: Int,
        val categories: List<CategoryRowUi>,
        val recent: List<RecentRowUi>,
        val canGoPrevious: Boolean,
        val canGoNext: Boolean,
    ) : InsightsUiState

    data class Error(val month: MonthRange) : InsightsUiState
}
