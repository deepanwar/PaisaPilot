package com.deepak.paisapilot.feature.personal.form

import com.deepak.paisapilot.domain.money.AmountResult
import com.deepak.paisapilot.domain.personal.Category
import java.time.LocalDate

enum class FormLoadState { Loading, Ready, NotFound, LoadFailed }

data class ExpenseFormUiState(
    val recordId: String,
    val isEdit: Boolean,
    val loadState: FormLoadState = FormLoadState.Ready,
    val amountText: String = "",
    val amountError: AmountResult? = null,
    val title: String = "",
    val titleTooLong: Boolean = false,
    val category: Category = Category.Other,
    val date: LocalDate,
    val note: String = "",
    val noteTooLong: Boolean = false,
    val canSave: Boolean = false,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: Boolean = false,
    val showDiscardDialog: Boolean = false,
)
