package com.deepak.paisapilot.feature.groups.balances

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.prefs.SettingsRepository
import com.deepak.paisapilot.core.data.repo.GroupRepository
import com.deepak.paisapilot.core.data.repo.SettlementRepository
import com.deepak.paisapilot.core.data.repo.SharedExpenseRepository
import com.deepak.paisapilot.domain.group.computeExpenseNets
import com.deepak.paisapilot.domain.group.computeNets
import com.deepak.paisapilot.domain.group.needsBalanceReview
import com.deepak.paisapilot.domain.group.suggestSettlements
import com.deepak.paisapilot.feature.groups.detail.DetailStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private const val KEY_GROUP_ID = "groupId"

@HiltViewModel
class BalancesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    groupRepository: GroupRepository,
    expenseRepository: SharedExpenseRepository,
    settlementRepository: SettlementRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val groupId: String = checkNotNull(savedStateHandle[KEY_GROUP_ID])

    val state: StateFlow<BalancesUiState> = combine(
        groupRepository.observeGroup(groupId),
        groupRepository.observeMembers(groupId),
        expenseRepository.observeGroup(groupId),
        settlementRepository.observeGroup(groupId),
        settings.settings,
    ) { group, members, expenses, settlements, userSettings ->
        if (group == null) return@combine BalancesUiState(status = DetailStatus.NotFound)
        // G7: the owner shows the live profile name, not the stored copy.
        val byId = members
            .map { if (it.isLocalOwner) it.copy(name = userSettings.displayName.ifBlank { it.name }) else it }
            .associateBy { it.id }
        val expenseNets = computeExpenseNets(expenses)
        val nets = computeNets(expenses, settlements)
        fun name(id: String) = byId[id]?.name.orEmpty()
        fun owner(id: String) = byId[id]?.isLocalOwner == true
        BalancesUiState(
            status = DetailStatus.Content,
            groupName = group.name,
            rows = byId.values
                .filter { it.active || (nets[it.id] ?: 0L) != 0L }
                .map { BalanceRowUi(it.id, it.name, it.isLocalOwner, nets[it.id] ?: 0L) }
                .sortedWith(compareByDescending<BalanceRowUi> { it.isOwner }.thenBy { it.name.lowercase() }),
            suggestions = suggestSettlements(nets).map {
                SuggestionUi(
                    fromId = it.fromMemberId,
                    fromName = name(it.fromMemberId),
                    fromIsOwner = owner(it.fromMemberId),
                    toId = it.toMemberId,
                    toName = name(it.toMemberId),
                    toIsOwner = owner(it.toMemberId),
                    amount = it.amount,
                )
            },
            allSettled = nets.values.all { it == 0L },
            reviewBalances = needsBalanceReview(expenseNets, nets),
            archived = group.isArchived,
        )
    }
        .catch { emit(BalancesUiState(status = DetailStatus.Error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BalancesUiState())
}
