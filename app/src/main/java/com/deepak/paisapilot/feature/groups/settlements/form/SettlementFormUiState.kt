package com.deepak.paisapilot.feature.groups.settlements.form

import com.deepak.paisapilot.domain.group.SettlementValidation
import com.deepak.paisapilot.domain.money.AmountResult
import com.deepak.paisapilot.feature.groups.shared.form.MemberChoice
import com.deepak.paisapilot.feature.personal.form.FormLoadState
import java.time.LocalDate

sealed interface SettlementSaveError {
    data object Failed : SettlementSaveError

    data object MemberChanged : SettlementSaveError

    /** Balances moved since the form opened. [max] is the current limit for the pair, if any. */
    data class BalanceChanged(val max: Long?) : SettlementSaveError
}

data class SettlementFormUiState(
    val recordId: String,
    val groupId: String,
    val isEdit: Boolean,
    val loadState: FormLoadState = FormLoadState.Loading,
    /** Archived group: view only. */
    val readOnly: Boolean = false,
    val members: List<MemberChoice> = emptyList(),
    val fromId: String? = null,
    val toId: String? = null,
    val amountText: String = "",
    val amountError: AmountResult? = null,
    val date: LocalDate = LocalDate.of(2026, 1, 1),
    val note: String = "",
    val noteTooLong: Boolean = false,
    /** Live check of the pair and amount against current balances; null until both are chosen. */
    val validation: SettlementValidation? = null,
    val isDirty: Boolean = false,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val saveError: SettlementSaveError? = null,
    val showDiscardDialog: Boolean = false,
)
