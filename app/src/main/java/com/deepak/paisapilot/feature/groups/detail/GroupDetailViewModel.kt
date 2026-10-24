package com.deepak.paisapilot.feature.groups.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.core.data.repo.AddMemberResult
import com.deepak.paisapilot.core.data.repo.GroupActionResult
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.core.data.repo.SettlementRepository
import com.deepak.paisapilot.core.data.repo.SharedExpenseRepository
import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.domain.group.Settlement
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.group.canRecordSharedExpense
import com.deepak.paisapilot.domain.group.computeNets
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.time.MonthRange
import com.deepak.paisapilot.domain.time.monthContaining
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val KEY_GROUP_ID = "groupId"
private const val KEY_MONTH_START = "groupMonthStart"
private val EARLIEST_MONTH: LocalDate = LocalDate.of(2000, 1, 1)
private val LATEST_MONTH: LocalDate = LocalDate.of(2100, 12, 1)

private data class Feed(
    val monthTotal: Paise,
    val range: MonthRange,
    val expenses: List<SharedExpense>,
    val settlements: List<Settlement>,
)

/** Dialog and busy flags kept apart from the data streams, merged in [state]. */
private data class Overlay(
    val showArchiveDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showAddMember: Boolean = false,
    val newMember: String = "",
    val memberError: MemberNameResult? = null,
    val actionFailed: Boolean = false,
    val isBusy: Boolean = false,
    val closed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: GroupRepository,
    private val sharedRepository: SharedExpenseRepository,
    private val settlementRepository: SettlementRepository,
    settings: SettingsRepository,
    clock: Clock,
) : ViewModel() {
    private val groupId: String = checkNotNull(savedStateHandle[KEY_GROUP_ID])
    private val currentMonthStart: LocalDate = monthContaining(LocalDate.now(clock)).start
    private val monthStart = MutableStateFlow(
        savedStateHandle.get<Long>(KEY_MONTH_START)?.let(LocalDate::ofEpochDay) ?: currentMonthStart,
    )
    private val overlay = MutableStateFlow(Overlay())
    private val reloads = MutableStateFlow(0)

    private val content = reloads.flatMapLatest {
        combine(
            repository.observeGroup(groupId),
            repository.observeMembers(groupId),
            settings.settings,
            repository.observeHasHistory(groupId),
            monthStart.flatMapLatest { start ->
                val range = monthContaining(start)
                combine(
                    repository.observeMonthTotal(groupId, range),
                    sharedRepository.observeGroup(groupId),
                    settlementRepository.observeGroup(groupId),
                ) { total, expenses, settlements -> Feed(total, range, expenses, settlements) }
            },
        ) { group, members, userSettings, hasHistory, monthAndEvents ->
            if (group == null) {
                GroupDetailUiState(status = DetailStatus.NotFound)
            } else {
                val (total, range, expenses, settlements) = monthAndEvents
                val byId = members.associateBy { it.id }
                val owner = members.firstOrNull { it.isLocalOwner }
                // G7: the owner shows the live profile name, not the stored copy.
                fun nameOf(id: String): String = byId[id]?.let {
                    if (it.isLocalOwner) userSettings.displayName else it.name
                }.orEmpty()
                GroupDetailUiState(
                    status = DetailStatus.Content,
                    name = group.name,
                    archived = group.isArchived,
                    members = members.map {
                        MemberRowUi(
                            id = it.id,
                            // G7: the owner shows the live profile name, not the stored copy.
                            name = if (it.isLocalOwner) userSettings.displayName else it.name,
                            isOwner = it.isLocalOwner,
                            active = it.active,
                        )
                    },
                    canRecordShared = !group.isArchived && canRecordSharedExpense(members),
                    events = expenses.map {
                        GroupEventUi(
                            id = it.id,
                            title = it.title,
                            category = it.category,
                            amount = it.amount,
                            date = it.date,
                            payerName = nameOf(it.payerMemberId),
                            payerIsOwner = byId[it.payerMemberId]?.isLocalOwner == true,
                        )
                    },
                    settlements = settlements.map {
                        SettlementEventUi(
                            id = it.id,
                            fromName = nameOf(it.fromMemberId),
                            fromIsOwner = byId[it.fromMemberId]?.isLocalOwner == true,
                            toName = nameOf(it.toMemberId),
                            toIsOwner = byId[it.toMemberId]?.isLocalOwner == true,
                            amount = it.amount,
                            date = LocalDate.ofEpochDay(it.occurredEpochDay),
                        )
                    },
                    ownerNetPaise = owner?.let { computeNets(expenses, settlements)[it.id] } ?: 0L,
                    canDelete = !hasHistory,
                    month = range,
                    isCurrentMonth = range.start == currentMonthStart,
                    monthTotal = total,
                )
            }
        }.catch { emit(GroupDetailUiState(status = DetailStatus.Error)) }
    }

    val state: StateFlow<GroupDetailUiState> = combine(content, overlay) { data, o ->
        data.copy(
            showArchiveDialog = o.showArchiveDialog,
            showDeleteDialog = o.showDeleteDialog,
            showAddMember = o.showAddMember,
            newMember = o.newMember,
            memberError = o.memberError,
            actionFailed = o.actionFailed,
            isBusy = o.isBusy,
            closed = o.closed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailUiState())

    fun onRetry() {
        reloads.value += 1
    }

    fun onPreviousMonth() = moveTo(monthStart.value.minusMonths(1))

    fun onNextMonth() = moveTo(monthStart.value.plusMonths(1))

    fun onArchiveRequested() = overlay.update { it.copy(showArchiveDialog = true, actionFailed = false) }

    fun onDeleteRequested() = overlay.update { it.copy(showDeleteDialog = true, actionFailed = false) }

    fun onDialogsDismissed() = overlay.update {
        it.copy(showArchiveDialog = false, showDeleteDialog = false)
    }

    fun onAddMemberRequested() = overlay.update {
        it.copy(showAddMember = true, newMember = "", memberError = null, actionFailed = false)
    }

    fun onAddMemberDismissed() = overlay.update {
        it.copy(showAddMember = false, newMember = "", memberError = null)
    }

    fun onNewMemberChange(value: String) = overlay.update { it.copy(newMember = value, memberError = null) }

    fun onAddMemberConfirmed() {
        val current = overlay.value
        if (current.isBusy || current.newMember.isBlank()) return
        overlay.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                when (val result = repository.addMember(groupId, current.newMember)) {
                    AddMemberResult.Added ->
                        overlay.update { it.copy(showAddMember = false, newMember = "", memberError = null, isBusy = false) }
                    is AddMemberResult.Invalid -> overlay.update { it.copy(memberError = result.error, isBusy = false) }
                    AddMemberResult.GroupNotFound, AddMemberResult.GroupArchived ->
                        overlay.update { it.copy(showAddMember = false, actionFailed = true, isBusy = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                overlay.update { it.copy(showAddMember = false, actionFailed = true, isBusy = false) }
            }
        }
    }

    fun onSetMemberActive(memberId: String, active: Boolean) = perform {
        repository.setMemberActive(memberId, active)
    }

    fun onArchiveConfirmed() = perform { repository.archive(groupId) }

    fun onDeleteConfirmed() = perform {
        repository.delete(groupId).also { if (it == GroupActionResult.Done) overlay.update { o -> o.copy(closed = true) } }
    }

    private fun perform(action: suspend () -> GroupActionResult) {
        if (overlay.value.isBusy) return
        overlay.update { it.copy(isBusy = true, showArchiveDialog = false, showDeleteDialog = false) }
        viewModelScope.launch {
            val failed = try {
                action() != GroupActionResult.Done
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                true
            }
            overlay.update { it.copy(isBusy = false, actionFailed = failed) }
        }
    }

    private fun moveTo(start: LocalDate) {
        if (start < EARLIEST_MONTH || start > LATEST_MONTH) return
        savedStateHandle[KEY_MONTH_START] = start.toEpochDay()
        monthStart.value = start
    }
}
