package com.deepak.paisapilot.feature.groups.shared.form

import com.deepak.paisapilot.domain.money.AmountResult
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.feature.personal.form.FormLoadState
import java.time.LocalDate

enum class SplitMode { Equally, Exact }

enum class SharedSaveError { Failed, MemberChanged }

data class MemberChoice(val id: String, val name: String, val isOwner: Boolean)

/**
 * One row of "Split among". [shareText] is the formatted amount in Equally mode, and the raw
 * input in Exact mode (only for included rows).
 */
data class ShareRowUi(
    val member: MemberChoice,
    val included: Boolean,
    val shareText: String,
    val shareError: AmountResult? = null,
)

data class NetLineUi(val member: MemberChoice, val netPaise: Long)

data class SharedFormUiState(
    val recordId: String,
    val groupId: String,
    val isEdit: Boolean,
    val loadState: FormLoadState = FormLoadState.Loading,
    val readOnly: Boolean = false,
    val amountText: String = "",
    val amountError: AmountResult? = null,
    val title: String = "",
    val titleTooLong: Boolean = false,
    val category: Category = Category.Other,
    val date: LocalDate,
    val note: String = "",
    val noteTooLong: Boolean = false,
    val payerChoices: List<MemberChoice> = emptyList(),
    val payerId: String? = null,
    val splitMode: SplitMode = SplitMode.Equally,
    val rows: List<ShareRowUi> = emptyList(),
    /** Null until the amount parses. */
    val totalPaise: Long? = null,
    val assignedPaise: Long = 0L,
    val remainingPaise: Long? = null,
    val noBeneficiaries: Boolean = false,
    val netPreview: List<NetLineUi> = emptyList(),
    val canSave: Boolean = false,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: SharedSaveError? = null,
    val saved: Boolean = false,
    val showDiscardDialog: Boolean = false,
)
