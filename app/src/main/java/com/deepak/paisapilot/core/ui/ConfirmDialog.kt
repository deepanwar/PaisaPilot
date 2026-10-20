package com.deepak.paisapilot.core.ui

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.deepak.paisapilot.R
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.focusRequester(cancelFocus),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
    LaunchedEffect(cancelFocus) {
        cancelFocus.requestFocus()
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConfirmDialogPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        ConfirmDialog(
            title = "Delete expense?",
            body = "This removes the expense from this device.",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
