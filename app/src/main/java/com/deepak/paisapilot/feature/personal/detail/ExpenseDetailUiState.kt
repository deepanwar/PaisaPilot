package com.deepak.paisapilot.feature.personal.detail

import com.deepak.paisapilot.domain.personal.PersonalExpense

enum class DetailStatus { Loading, Content, NotFound, Error }

data class ExpenseDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val expense: PersonalExpense? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteFailed: Boolean = false,
    val deleted: Boolean = false,
)
