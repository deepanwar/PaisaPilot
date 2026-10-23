package com.deepak.paisapilot.feature.groups.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.paisapilot.core.data.repo.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupListViewModel @Inject constructor(
    repository: GroupRepository,
) : ViewModel() {
    private val reloads = MutableStateFlow(0)

    val state: StateFlow<GroupListUiState> = reloads
        .flatMapLatest {
            repository.observeGroups()
                .map { groups ->
                    val rows = groups.map {
                        GroupRowUi(it.group.id, it.group.name, it.memberCount, it.group.isArchived)
                    }
                    GroupListUiState.Content(
                        active = rows.filterNot { it.archived },
                        archived = rows.filter { it.archived },
                    ) as GroupListUiState
                }
                .catch { emit(GroupListUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupListUiState.Loading)

    fun onRetry() {
        reloads.value += 1
    }
}
