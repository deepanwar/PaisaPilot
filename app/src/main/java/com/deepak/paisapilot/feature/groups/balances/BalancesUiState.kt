package com.deepak.paisapilot.feature.groups.balances

import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.feature.groups.detail.DetailStatus

data class BalanceRowUi(
    val memberId: String,
    val name: String,
    val isOwner: Boolean,
    /** Signed: positive = is owed, negative = owes. */
    val netPaise: Long,
)

data class SuggestionUi(
    val fromId: String,
    val fromName: String,
    val fromIsOwner: Boolean,
    val toId: String,
    val toName: String,
    val toIsOwner: Boolean,
    val amount: Paise,
)

data class BalancesUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val groupName: String = "",
    val rows: List<BalanceRowUi> = emptyList(),
    val suggestions: List<SuggestionUi> = emptyList(),
    val allSettled: Boolean = true,
    /** Informational only: an earlier settlement is no longer justified by the expenses. */
    val reviewBalances: Boolean = false,
    val archived: Boolean = false,
) {
    val canRecord: Boolean get() = !archived && suggestions.isNotEmpty()
}
