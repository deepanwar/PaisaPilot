package com.deepak.paisapilot.feature.groups.shared.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.core.data.repo.GroupActionResult
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.core.data.repo.SharedExpenseRepository
import com.deepak.paisapilot.domain.group.Group
import com.deepak.paisapilot.domain.group.Member
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.feature.groups.detail.DetailStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val KEY_GROUP_ID = "groupId"
private const val KEY_EXPENSE_ID = "expenseId"

private data class Inputs(
    val expense: SharedExpense?,
    val members: List<Member>,
    val group: Group?,
    val ownerName: String,
)

@HiltViewModel
class SharedDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SharedExpenseRepository,
    groupRepository: GroupRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val groupId: String = checkNotNull(savedStateHandle[KEY_GROUP_ID])
    private val expenseId: String = checkNotNull(savedStateHandle[KEY_EXPENSE_ID])
    private val _state = MutableStateFlow(SharedDetailUiState())
    val state: StateFlow<SharedDetailUiState> = _state.asStateFlow()

    /** Set before a delete starts so the record vanishing never shows "unavailable". */
    private var deleteStarted = false

    init {
        viewModelScope.launch {
            combine(
                repository.observe(expenseId),
                groupRepository.observeMembers(groupId),
                groupRepository.observeGroup(groupId),
                settings.settings,
            ) { expense, members, group, userSettings -> Inputs(expense, members, group, userSettings.displayName) }
                .catch { _state.update { it.copy(status = DetailStatus.Error) } }
                .collect { (expense, members, group, ownerName) ->
                    _state.update { current ->
                        when {
                            expense != null && expense.groupId == groupId -> {
                                // G7: the owner shows the live profile name, not the stored copy.
                                val byId = members
                                    .map { if (it.isLocalOwner) it.copy(name = ownerName.ifBlank { it.name }) else it }
                                    .associateBy { it.id }
                                current.copy(
                                    status = DetailStatus.Content,
                                    expense = expense,
                                    payerName = byId[expense.payerMemberId]?.name.orEmpty(),
                                    payerIsOwner = byId[expense.payerMemberId]?.isLocalOwner == true,
                                    shareLines = expense.shares.map { share ->
                                        ShareLineUi(
                                            memberId = share.memberId,
                                            name = byId[share.memberId]?.name.orEmpty(),
                                            isOwner = byId[share.memberId]?.isLocalOwner == true,
                                            paise = share.paise.value,
                                        )
                                    },
                                    readOnly = group?.isArchived == true,
                                )
                            }
                            deleteStarted -> current
                            else -> current.copy(status = DetailStatus.NotFound, expense = null)
                        }
                    }
                }
        }
    }

    fun onDeleteRequested() {
        val current = _state.value
        if (current.expense == null || current.readOnly) return
        _state.update { it.copy(showDeleteDialog = true, deleteFailed = false) }
    }

    fun onDeleteCancelled() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    fun onDeleteConfirmed() {
        val current = _state.value
        if (current.expense == null || current.isDeleting || current.deleted) return
        deleteStarted = true
        _state.update { it.copy(showDeleteDialog = false, isDeleting = true) }
        viewModelScope.launch {
            try {
                when (repository.delete(expenseId)) {
                    GroupActionResult.Done, GroupActionResult.NotFound ->
                        _state.update { it.copy(isDeleting = false, deleted = true) }
                    GroupActionResult.ReadOnly, GroupActionResult.NotAllowed -> {
                        deleteStarted = false
                        _state.update { it.copy(isDeleting = false, deleteFailed = true) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                deleteStarted = false
                _state.update { it.copy(isDeleting = false, deleteFailed = true) }
            }
        }
    }
}
