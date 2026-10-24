package com.deepak.paisapilot.feature.groups.settlements.detail

import com.deepak.paisapilot.domain.group.Settlement
import com.deepak.paisapilot.feature.groups.detail.DetailStatus

data class SettlementDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val settlement: Settlement? = null,
    val fromName: String = "",
    val fromIsOwner: Boolean = false,
    val toName: String = "",
    val toIsOwner: Boolean = false,
    /** Archived group: view only, no Edit or Delete. */
    val readOnly: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val deleteFailed: Boolean = false,
)
