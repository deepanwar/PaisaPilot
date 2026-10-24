package com.deepak.paisapilot.feature.groups.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GroupDetailRoute(
    onClose: () -> Unit,
    onAddShared: () -> Unit,
    onOpenShared: (String) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) {
        if (state.closed) onClose()
    }
    GroupDetailScreen(
        state = state,
        onBack = onClose,
        onRetry = viewModel::onRetry,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onArchiveRequested = viewModel::onArchiveRequested,
        onArchiveConfirmed = viewModel::onArchiveConfirmed,
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDialogsDismissed = viewModel::onDialogsDismissed,
        onAddMemberRequested = viewModel::onAddMemberRequested,
        onAddMemberDismissed = viewModel::onAddMemberDismissed,
        onNewMemberChange = viewModel::onNewMemberChange,
        onAddMemberConfirmed = viewModel::onAddMemberConfirmed,
        onSetMemberActive = viewModel::onSetMemberActive,
        onAddShared = onAddShared,
        onOpenShared = onOpenShared,
    )
}
