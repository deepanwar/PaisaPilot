package com.deepak.paisapilot.feature.groups.shared.form

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.deepak.paisapilot.feature.personal.form.FormLoadState
import com.deepak.paisapilot.feature.personal.labelRes
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedFormScreen(
    state: SharedFormUiState,
    onBack: () -> Unit,
    onAmountChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onPayerChange: (String) -> Unit,
    onToggleMember: (String) -> Unit,
    onSplitModeChange: (SplitMode) -> Unit,
    onExactShareChange: (String, String) -> Unit,
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
                        if (state.isEdit) R.string.shared_edit_title else R.string.shared_add_title,
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
                message = stringResource(R.string.shared_unavailable),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            FormLoadState.LoadFailed -> EmptyState(
                message = stringResource(R.string.shared_load_failed),
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
                onPayerChange = onPayerChange,
                onToggleMember = onToggleMember,
                onSplitModeChange = onSplitModeChange,
                onExactShareChange = onExactShareChange,
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
    state: SharedFormUiState,
    onAmountChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onPayerChange: (String) -> Unit,
    onToggleMember: (String) -> Unit,
    onSplitModeChange: (SplitMode) -> Unit,
    onExactShareChange: (String, String) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val amountFocus = remember { FocusRequester() }
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
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

        Text(
            text = stringResource(R.string.shared_paid_by),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            state.payerChoices.forEach { member ->
                FilterChip(
                    selected = member.id == state.payerId,
                    onClick = { onPayerChange(member.id) },
                    label = { Text(memberLabel(member)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }

        Text(
            text = stringResource(R.string.shared_split_among),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics { heading() },
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val modes = listOf(
                SplitMode.Equally to R.string.shared_mode_equally,
                SplitMode.Exact to R.string.shared_mode_exact,
            )
            modes.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = state.splitMode == mode,
                    onClick = { onSplitModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(label))
                }
            }
        }
        state.rows.forEach { row ->
            ShareRow(
                row = row,
                exact = state.splitMode == SplitMode.Exact,
                onToggle = { onToggleMember(row.member.id) },
                onExactChange = { onExactShareChange(row.member.id, it) },
            )
        }
        if (state.noBeneficiaries) {
            Text(
                text = stringResource(R.string.shared_no_beneficiaries),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        SplitSummary(state)
        NetPreview(state)

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
        state.saveError?.let { error ->
            Text(
                text = stringResource(
                    when (error) {
                        SharedSaveError.Failed -> R.string.shared_save_failed
                        SharedSaveError.MemberChanged -> R.string.shared_member_changed
                    },
                ),
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
private fun ShareRow(
    row: ShareRowUi,
    exact: Boolean,
    onToggle: () -> Unit,
    onExactChange: (String) -> Unit,
) {
    val name = memberLabel(row.member)
    val stateText = if (row.included) {
        stringResource(R.string.shared_member_included, row.shareText.ifEmpty { formatInr(Paise(0)) })
    } else {
        stringResource(R.string.shared_member_excluded)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .toggleable(value = row.included, role = Role.Checkbox, onValueChange = { onToggle() })
                .semantics(mergeDescendants = true) { stateDescription = stateText },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
        ) {
            Checkbox(checked = row.included, onCheckedChange = null)
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
        }
        if (row.included) {
            if (exact) {
                val label = stringResource(R.string.shared_share_label, name)
                OutlinedTextField(
                    value = row.shareText,
                    onValueChange = onExactChange,
                    modifier = Modifier
                        .width(140.dp)
                        .semantics { contentDescription = label },
                    prefix = { Text(stringResource(R.string.currency_prefix)) },
                    isError = row.shareError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                )
            } else {
                Text(text = row.shareText, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SplitSummary(state: SharedFormUiState) {
    val total = state.totalPaise ?: return
    val remaining = state.remainingPaise ?: 0L
    val totalText = formatInr(Paise(total))
    val assignedText = formatInr(Paise(state.assignedPaise))
    val remainingText = formatInr(Paise(remaining))
    val description = stringResource(R.string.shared_summary_description, totalText, assignedText, remainingText)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        verticalArrangement = Arrangement.spacedBy(Spacing.space1),
    ) {
        Text(stringResource(R.string.shared_total, totalText), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.shared_assigned, assignedText), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.shared_remaining, remainingText),
            style = MaterialTheme.typography.bodyLarge,
            color = if (remaining == 0L) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
        if (remaining != 0L && state.splitMode == SplitMode.Exact) {
            val amount = formatInr(Paise(abs(remaining)))
            Text(
                text = if (remaining > 0L) {
                    stringResource(R.string.shared_remaining_under, amount)
                } else {
                    stringResource(R.string.shared_remaining_over, amount)
                },
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun NetPreview(state: SharedFormUiState) {
    if (state.netPreview.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
        Text(
            text = stringResource(R.string.shared_net_heading),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics { heading() },
        )
        state.netPreview.filter { it.netPaise != 0L }.forEach { line ->
            val amount = formatInr(Paise(abs(line.netPaise)))
            val name = memberLabel(line.member)
            Text(
                text = if (line.netPaise > 0L) {
                    stringResource(R.string.shared_net_owed, name, amount)
                } else {
                    stringResource(R.string.shared_net_owes, name, amount)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
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

private val previewMe = MemberChoice("m1", "Me", isOwner = true)
private val previewAlex = MemberChoice("m2", "Alex", isOwner = false)

@Preview(name = "Equally")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun SharedFormEquallyPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        SharedFormScreen(
            state = SharedFormUiState(
                recordId = "e1",
                groupId = "g1",
                isEdit = false,
                loadState = FormLoadState.Ready,
                amountText = "200",
                date = LocalDate.of(2026, 10, 24),
                payerChoices = listOf(previewMe, previewAlex),
                payerId = "m1",
                rows = listOf(
                    ShareRowUi(previewMe, true, formatInr(Paise(10_000))),
                    ShareRowUi(previewAlex, true, formatInr(Paise(10_000))),
                ),
                totalPaise = 20_000,
                assignedPaise = 20_000,
                remainingPaise = 0,
                netPreview = listOf(NetLineUi(previewMe, 10_000), NetLineUi(previewAlex, -10_000)),
                canSave = true,
            ),
            onBack = {},
            onAmountChange = {},
            onTitleChange = {},
            onCategoryChange = {},
            onDateChange = {},
            onNoteChange = {},
            onPayerChange = {},
            onToggleMember = {},
            onSplitModeChange = {},
            onExactShareChange = { _, _ -> },
            onSave = {},
            onRetryLoad = {},
            onDiscard = {},
            onKeepEditing = {},
        )
    }
}

@Preview(name = "Exact mismatch")
@Composable
private fun SharedFormExactPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        SharedFormScreen(
            state = SharedFormUiState(
                recordId = "e1",
                groupId = "g1",
                isEdit = false,
                loadState = FormLoadState.Ready,
                amountText = "100",
                date = LocalDate.of(2026, 10, 24),
                payerChoices = listOf(previewMe, previewAlex),
                payerId = "m1",
                splitMode = SplitMode.Exact,
                rows = listOf(
                    ShareRowUi(previewMe, true, "60"),
                    ShareRowUi(previewAlex, true, "30"),
                ),
                totalPaise = 10_000,
                assignedPaise = 9_000,
                remainingPaise = 1_000,
            ),
            onBack = {},
            onAmountChange = {},
            onTitleChange = {},
            onCategoryChange = {},
            onDateChange = {},
            onNoteChange = {},
            onPayerChange = {},
            onToggleMember = {},
            onSplitModeChange = {},
            onExactShareChange = { _, _ -> },
            onSave = {},
            onRetryLoad = {},
            onDiscard = {},
            onKeepEditing = {},
        )
    }
}
