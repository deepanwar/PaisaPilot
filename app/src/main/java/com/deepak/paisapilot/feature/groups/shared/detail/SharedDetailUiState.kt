package com.deepak.paisapilot.feature.groups.shared.detail

import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.feature.groups.detail.DetailStatus

data class ShareLineUi(val memberId: String, val name: String, val isOwner: Boolean, val paise: Long)

data class SharedDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val expense: SharedExpense? = null,
    val payerName: String = "",
    val payerIsOwner: Boolean = false,
    val shareLines: List<ShareLineUi> = emptyList(),
    /** Archived group: view only, no Edit or Delete. */
    val readOnly: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val deleteFailed: Boolean = false,
)
