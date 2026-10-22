package com.deepak.paisapilot.feature.activity

import com.deepak.paisapilot.domain.activity.ActivityFilter
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import java.time.LocalDate

data class ActivityRowUi(
    val id: String,
    val title: String?,
    val category: Category,
    val date: LocalDate,
    val amount: Paise,
)

sealed interface ActivityUiState {
    data object Loading : ActivityUiState

    data object Error : ActivityUiState

    data class Content(
        val filter: ActivityFilter,
        val rows: List<ActivityRowUi>,
        val resultCount: Int,
        /** False only when the ledger has no entries at all, so the empty CTA beats "no matches". */
        val hasAnyData: Boolean,
        /** First days of months that have entries, newest first. */
        val availableMonths: List<LocalDate>,
        /** Shown only for an explicit scope; the mixed feed never gets a combined total. */
        val scopeTotal: Paise?,
    ) : ActivityUiState
}
