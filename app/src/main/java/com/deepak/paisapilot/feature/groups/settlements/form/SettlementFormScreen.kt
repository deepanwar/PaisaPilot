package com.deepak.paisapilot.feature.groups.settlements.form

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.deepak.paisapilot.R
import com.deepak.paisapilot.core.ui.AmountField
import com.deepak.paisapilot.core.ui.ConfirmDialog
import com.deepak.paisapilot.core.ui.DateSelectDialog
import com.deepak.paisapilot.core.ui.EmptyState
import com.deepak.paisapilot.core.ui.StickySaveBar
import com.deepak.paisapilot.domain.group.SettlementValidation
import com.deepak.paisapilot.domain.money.AmountResult
import com.deepak.paisapilot.domain.money.MoneyLimits
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.domain.personal.MAX_NOTE_LENGTH
import com.deepak.paisapilot.feature.groups.shared.form.MemberChoice
import com.deepak.paisapilot.feature.personal.form.FormLoadState
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementFormScreen(
    state: SettlementFormUiState,
    onBack: () -> Unit,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
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
                        if (state.isEdit) R.string.settlement_edit_title else R.string.settlement_add_title,
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
                message = stringResource(R.string.settlement_unavailable),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            FormLoadState.LoadFailed -> EmptyState(
                message = stringResource(R.string.settlement_load_failed),
                actionLabel = stringResource(R.string.retry),
                onAction = onRetryLoad,
            )
            FormLoadState.Ready -> FormFields(
                state = state,
                onFromChange = onFromChange,
                onToChange = onToChange,
                onAmountChange = onAmountChange,
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

@Composable
private fun memberLabel(member: MemberChoice): String =
    if (member.isOwner) stringResource(R.string.group_owner_chip, member.name) else member.name

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.FormFields(
    state: SettlementFormUiState,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var noteExpanded by rememberSaveable { mutableStateOf(state.note.isNotEmpty()) }
    val dateText = remember(state.date) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(state.date)
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.space4, vertical = Spacing.space2),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4),
    ) {
        if (state.readOnly) {
            Text(
                text = stringResource(R.string.group_archived_banner),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(R.string.settlement_no_transfer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MemberPicker(
            label = stringResource(R.string.settlement_from),
            members = state.members,
            selectedId = state.fromId,
            onSelect = onFromChange,
        )
        MemberPicker(
            label = stringResource(R.string.settlement_to),
            members = state.members,
            selectedId = state.toId,
            onSelect = onToChange,
        )
        AmountField(
            value = state.amountText,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            error = state.amountError?.let { amountErrorText(it) },
        )
        validationText(state.validation)?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
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
            androidx.compose.material3.TextButton(
                onClick = { noteExpanded = true },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.expense_note_add))
            }
        }
        state.saveError?.let { error ->
            Text(
                text = when (error) {
                    SettlementSaveError.Failed -> stringResource(R.string.settlement_save_failed)
                    SettlementSaveError.MemberChanged -> stringResource(R.string.settlement_member_changed)
                    is SettlementSaveError.BalanceChanged -> error.max
                        ?.let { stringResource(R.string.settlement_exceeds, formatInr(Paise(it))) }
                        ?: stringResource(R.string.settlement_pair_not_suggested)
                },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberPicker(
    label: String,
    members: List<MemberChoice>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.semantics { heading() },
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        members.forEach { member ->
            FilterChip(
                selected = member.id == selectedId,
                onClick = { onSelect(member.id) },
                label = { Text(memberLabel(member)) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun validationText(validation: SettlementValidation?): String? = when (validation) {
    SettlementValidation.SameMember -> stringResource(R.string.settlement_same_member)
    SettlementValidation.PairNotSuggested -> stringResource(R.string.settlement_pair_not_suggested)
    is SettlementValidation.ExceedsSuggestion ->
        stringResource(R.string.settlement_exceeds, formatInr(Paise(validation.max)))
    else -> null
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
private fun SettlementFormPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        SettlementFormScreen(
            state = SettlementFormUiState(
                recordId = "s1",
                groupId = "g1",
                isEdit = false,
                loadState = FormLoadState.Ready,
                members = listOf(MemberChoice("m1", "Me", true), MemberChoice("m2", "Alex", false)),
                fromId = "m2",
                toId = "m1",
                amountText = "60.00",
                date = LocalDate.of(2026, 10, 24),
                canSave = true,
            ),
            onBack = {}, onFromChange = {}, onToChange = {}, onAmountChange = {}, onDateChange = {},
            onNoteChange = {}, onSave = {}, onRetryLoad = {}, onDiscard = {}, onKeepEditing = {},
        )
    }
}
