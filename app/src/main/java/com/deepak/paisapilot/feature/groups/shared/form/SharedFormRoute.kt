package com.deepak.paisapilot.feature.groups.shared.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SharedFormRoute(
    onClose: () -> Unit,
    viewModel: SharedFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.onSavedHandled()
            onClose()
        }
    }
    SharedFormScreen(
        state = state,
        onBack = { if (state.isDirty) viewModel.onBackRequested() else onClose() },
        onAmountChange = viewModel::onAmountChange,
        onTitleChange = viewModel::onTitleChange,
        onCategoryChange = viewModel::onCategoryChange,
        onDateChange = viewModel::onDateChange,
        onNoteChange = viewModel::onNoteChange,
        onPayerChange = viewModel::onPayerChange,
        onToggleMember = viewModel::onToggleMember,
        onSplitModeChange = viewModel::onSplitModeChange,
        onExactShareChange = viewModel::onExactShareChange,
        onSave = viewModel::onSave,
        onRetryLoad = viewModel::onRetryLoad,
        onDiscard = {
            viewModel.onKeepEditing()
            onClose()
        },
        onKeepEditing = viewModel::onKeepEditing,
    )
}
