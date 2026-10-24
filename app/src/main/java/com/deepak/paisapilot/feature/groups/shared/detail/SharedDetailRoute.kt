package com.deepak.paisapilot.feature.groups.shared.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SharedDetailRoute(
    onClose: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: SharedDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted) {
        if (state.deleted) onClose()
    }
    SharedDetailScreen(
        state = state,
        onBack = onClose,
        onEdit = { state.expense?.let { onEdit(it.id) } },
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteCancelled = viewModel::onDeleteCancelled,
    )
}
