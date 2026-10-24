package com.deepak.paisapilot.feature.groups.settlements.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettlementDetailRoute(
    onClose: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: SettlementDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted) {
        if (state.deleted) onClose()
    }
    SettlementDetailScreen(
        state = state,
        onBack = onClose,
        onEdit = { state.settlement?.let { onEdit(it.id) } },
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteCancelled = viewModel::onDeleteCancelled,
    )
}
