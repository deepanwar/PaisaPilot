package com.deepak.paisapilot.feature.groups.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.core.data.repo.AddMemberResult
import com.deepak.paisapilot.core.data.repo.GroupActionResult
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.domain.group.canRecordSharedExpense
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
                repository.observeMonthTotal(groupId, range).map { total -> total to range }
            },
        ) { group, members, userSettings, hasHistory, totalAndRange ->
            if (group == null) {
                GroupDetailUiState(status = DetailStatus.NotFound)
            } else {
                val (total, range) = totalAndRange
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
