package com.deepak.paisapilot.core.undo

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.paisapilot.R

@Composable
fun UndoSnackbarEffect(
    snackbarHostState: SnackbarHostState,
    viewModel: UndoViewModel = hiltViewModel(),
) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val restoreFailed by viewModel.restoreFailed.collectAsStateWithLifecycle()
    val deletedText = stringResource(R.string.undo_deleted)
    val undoText = stringResource(R.string.undo_action)
    val failedText = stringResource(R.string.undo_failed)
    pending?.let { expense ->
        LaunchedEffect(expense) {
            val result = snackbarHostState.showSnackbar(
                message = deletedText,
                actionLabel = undoText,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undo()
            } else {
                viewModel.dismiss(expense)
            }
        }
    }
    if (restoreFailed) {
        LaunchedEffect(Unit) {
            viewModel.onRestoreFailedShown()
            snackbarHostState.showSnackbar(failedText)
        }
    }
}
