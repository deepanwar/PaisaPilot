package com.deepak.paisapilot.feature.groups.detail

import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.time.MonthRange

data class MemberRowUi(
    val id: String,
    val name: String,
    val isOwner: Boolean,
    val active: Boolean,
)

enum class DetailStatus { Loading, Content, NotFound, Error }

data class GroupDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val name: String = "",
    val archived: Boolean = false,
    val members: List<MemberRowUi> = emptyList(),
    val canRecordShared: Boolean = false,
    val canDelete: Boolean = false,
    val month: MonthRange? = null,
    val isCurrentMonth: Boolean = true,
    val monthTotal: Paise = Paise(0),
    val showArchiveDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showAddMember: Boolean = false,
    val newMember: String = "",
    val memberError: MemberNameResult? = null,
    val actionFailed: Boolean = false,
    val isBusy: Boolean = false,
    val closed: Boolean = false,
)
