package com.deepak.paisapilot.feature.groups.settlements.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettlementFormRoute(
    onClose: () -> Unit,
    viewModel: SettlementFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.onSavedHandled()
            onClose()
        }
    }
    SettlementFormScreen(
        state = state,
        onBack = { if (state.isDirty) viewModel.onBackRequested() else onClose() },
        onFromChange = viewModel::onFromChange,
        onToChange = viewModel::onToChange,
        onAmountChange = viewModel::onAmountChange,
        onDateChange = viewModel::onDateChange,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::onSave,
        onRetryLoad = viewModel::onRetryLoad,
        onDiscard = {
            viewModel.onKeepEditing()
            onClose()
        },
        onKeepEditing = viewModel::onKeepEditing,
    )
}
