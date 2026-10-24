package com.deepak.paisapilot.feature.groups.detail

import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.time.MonthRange
import java.time.LocalDate

data class MemberRowUi(
    val id: String,
    val name: String,
    val isOwner: Boolean,
    val active: Boolean,
)

data class GroupEventUi(
    val id: String,
    val title: String?,
    val category: Category,
    val amount: Paise,
    val date: LocalDate,
    val payerName: String,
    val payerIsOwner: Boolean,
)

data class SettlementEventUi(
    val id: String,
    val fromName: String,
    val fromIsOwner: Boolean,
    val toName: String,
    val toIsOwner: Boolean,
    val amount: Paise,
    val date: LocalDate,
)

enum class DetailStatus { Loading, Content, NotFound, Error }

data class GroupDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val name: String = "",
    val archived: Boolean = false,
    val members: List<MemberRowUi> = emptyList(),
    val canRecordShared: Boolean = false,
    /** Newest first. */
    val events: List<GroupEventUi> = emptyList(),
    /** Recorded settlements, newest first. Not group spending. */
    val settlements: List<SettlementEventUi> = emptyList(),
    /** Local owner's net across all shared expenses and settlements: positive = owed to them. */
    val ownerNetPaise: Long = 0L,
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
