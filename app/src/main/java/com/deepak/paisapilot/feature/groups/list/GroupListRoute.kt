package com.deepak.paisapilot.feature.groups.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GroupListRoute(
    onNewGroup: () -> Unit,
    onOpenGroup: (String) -> Unit,
    viewModel: GroupListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupListScreen(
        state = state,
        onNewGroup = onNewGroup,
        onOpenGroup = onOpenGroup,
        onRetry = viewModel::onRetry,
    )
}
