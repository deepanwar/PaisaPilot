package com.deepak.paisapilot.feature.personal.form

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.deepak.paisapilot.R
import com.deepak.paisapilot.core.ui.AmountField
import com.deepak.paisapilot.core.ui.ConfirmDialog
import com.deepak.paisapilot.core.ui.DateSelectDialog
import com.deepak.paisapilot.core.ui.EmptyState
import com.deepak.paisapilot.core.ui.StickySaveBar
import com.deepak.paisapilot.domain.money.AmountResult
import com.deepak.paisapilot.domain.money.MoneyLimits
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.MAX_NOTE_LENGTH
import com.deepak.paisapilot.domain.personal.MAX_TITLE_LENGTH
import com.deepak.paisapilot.feature.personal.labelRes
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    state: ExpenseFormUiState,
    onBack: () -> Unit,
    onAmountChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetryLoad: () -> Unit,
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.isDirty) { onBack() }
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(
                        if (state.isEdit) R.string.expense_edit_title else R.string.expense_add_title,
                    ),
                    modifier = Modifier.semantics { heading() },
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
        )
        when (state.loadState) {
            FormLoadState.Loading -> CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(Spacing.space8),
            )
            FormLoadState.NotFound -> EmptyState(
                message = stringResource(R.string.expense_unavailable),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            FormLoadState.LoadFailed -> EmptyState(
                message = stringResource(R.string.expense_load_failed),
                actionLabel = stringResource(R.string.retry),
                onAction = onRetryLoad,
            )
            FormLoadState.Ready -> FormFields(
                state = state,
                onAmountChange = onAmountChange,
                onTitleChange = onTitleChange,
                onCategoryChange = onCategoryChange,
                onDateChange = onDateChange,
                onNoteChange = onNoteChange,
                onSave = onSave,
            )
        }
    }
    if (state.showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(R.string.expense_discard_title),
            body = stringResource(R.string.expense_discard_body),
            confirmLabel = stringResource(R.string.expense_discard_confirm),
            onConfirm = onDiscard,
            onDismiss = onKeepEditing,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.FormFields(
    state: ExpenseFormUiState,
    onAmountChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val amountFocus = remember { FocusRequester() }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var noteExpanded by rememberSaveable { mutableStateOf(state.note.isNotEmpty()) }
    val dateText = remember(state.date) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(state.date)
    }
    LaunchedEffect(Unit) {
        if (!state.isEdit) amountFocus.requestFocus()
    }
    LaunchedEffect(state.amountError) {
        if (state.amountError != null) amountFocus.requestFocus()
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.space4, vertical = Spacing.space2),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4),
    ) {
        AmountField(
            value = state.amountText,
            onValueChange = onAmountChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(amountFocus),
            error = state.amountError?.let { amountErrorText(it) },
        )
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.expense_title_label)) },
            isError = state.titleTooLong,
            supportingText = {
                Text(
                    if (state.titleTooLong) {
                        stringResource(R.string.expense_title_too_long)
                    } else {
                        stringResource(R.string.expense_title_hint)
                    },
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Text(
            text = stringResource(R.string.expense_category_label),
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
        ) {
            Category.entries.forEach { category ->
                FilterChip(
                    selected = category == state.category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(stringResource(category.labelRes())) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.expense_date_label),
            style = MaterialTheme.typography.labelLarge,
        )
        val dateDescription = stringResource(R.string.expense_date_description, dateText)
        OutlinedButton(
            onClick = {
                focusManager.clearFocus()
                showDatePicker = true
            },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = dateDescription },
        ) {
            Text(dateText)
        }
        if (noteExpanded || state.note.isNotEmpty()) {
            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.expense_note_label)) },
                isError = state.noteTooLong,
                supportingText = if (state.noteTooLong) {
                    { Text(stringResource(R.string.expense_note_too_long)) }
                } else {
                    { Text("${state.note.trim().length}/$MAX_NOTE_LENGTH") }
                },
                minLines = 2,
            )
        } else {
            TextButton(
                onClick = { noteExpanded = true },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.expense_note_add))
            }
        }
        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.expense_save_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    StickySaveBar(enabled = state.canSave, onSave = onSave)
    if (showDatePicker) {
        DateSelectDialog(
            initial = state.date,
            onDateSelected = {
                showDatePicker = false
                onDateChange(it)
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun amountErrorText(error: AmountResult): String = when (error) {
    AmountResult.Empty -> stringResource(R.string.expense_amount_empty)
    AmountResult.Invalid -> stringResource(R.string.expense_amount_invalid)
    AmountResult.TooManyDecimals -> stringResource(R.string.expense_amount_decimals)
    AmountResult.NotPositive -> stringResource(R.string.expense_amount_not_positive)
    AmountResult.OverLimit -> stringResource(
        R.string.expense_amount_over_limit,
        formatInr(Paise(MoneyLimits.MAX_ENTRY_PAISE)),
    )
    is AmountResult.Success -> ""
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ExpenseFormScreenPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        ExpenseFormScreen(
            state = ExpenseFormUiState(
                recordId = "id",
                isEdit = false,
                amountText = "125.50",
                title = "Tea",
                date = LocalDate.of(2026, 10, 20),
                canSave = true,
            ),
            onBack = {},
            onAmountChange = {},
            onTitleChange = {},
            onCategoryChange = {},
            onDateChange = {},
            onNoteChange = {},
            onSave = {},
            onRetryLoad = {},
            onDiscard = {},
            onKeepEditing = {},
        )
    }
}

@Preview(name = "Error")
@Composable
private fun ExpenseFormScreenErrorPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        ExpenseFormScreen(
            state = ExpenseFormUiState(
                recordId = "id",
                isEdit = false,
                amountText = "12.345",
                amountError = AmountResult.TooManyDecimals,
                title = "a".repeat(MAX_TITLE_LENGTH + 1),
                titleTooLong = true,
                date = LocalDate.of(2026, 10, 20),
            ),
            onBack = {},
            onAmountChange = {},
            onTitleChange = {},
            onCategoryChange = {},
            onDateChange = {},
            onNoteChange = {},
            onSave = {},
            onRetryLoad = {},
            onDiscard = {},
            onKeepEditing = {},
        )
    }
}
