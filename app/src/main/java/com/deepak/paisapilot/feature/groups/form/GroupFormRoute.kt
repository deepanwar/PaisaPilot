package com.deepak.paisapilot.feature.groups.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GroupFormRoute(
    onClose: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: GroupFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdGroupId) {
        state.createdGroupId?.let {
            viewModel.onCreatedHandled()
            onCreated(it)
        }
    }
    GroupFormScreen(
        state = state,
        onBack = { if (state.isDirty) viewModel.onBackRequested() else onClose() },
        onNameChange = viewModel::onNameChange,
        onNewMemberChange = viewModel::onNewMemberChange,
        onAddMember = viewModel::onAddMember,
        onRemoveMember = viewModel::onRemoveMember,
        onSave = viewModel::onSave,
        onDiscard = {
            viewModel.onKeepEditing()
            onClose()
        },
        onKeepEditing = viewModel::onKeepEditing,
    )
}
