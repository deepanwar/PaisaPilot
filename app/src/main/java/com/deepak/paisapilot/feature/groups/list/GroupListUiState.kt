package com.deepak.paisapilot.feature.groups.list

data class GroupRowUi(
    val id: String,
    val name: String,
    val memberCount: Int,
    val archived: Boolean,
)

sealed interface GroupListUiState {
    data object Loading : GroupListUiState

    data object Error : GroupListUiState

    data class Content(
        val active: List<GroupRowUi>,
        val archived: List<GroupRowUi>,
    ) : GroupListUiState
}
