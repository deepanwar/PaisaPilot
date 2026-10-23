package com.deepak.paisapilot.feature.groups.detail

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.deepak.paisapilot.R
import com.deepak.paisapilot.core.ui.ConfirmDialog
import com.deepak.paisapilot.core.ui.EmptyState
import com.deepak.paisapilot.core.ui.MoneyText
import com.deepak.paisapilot.core.ui.MonthSwitcher
import com.deepak.paisapilot.domain.group.MAX_MEMBERS
import com.deepak.paisapilot.domain.group.MAX_MEMBER_NAME_LENGTH
import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.domain.time.monthContaining
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.PillShape
import com.deepak.paisapilot.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    state: GroupDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onArchiveRequested: () -> Unit,
    onArchiveConfirmed: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDialogsDismissed: () -> Unit,
    onAddMemberRequested: () -> Unit,
    onAddMemberDismissed: () -> Unit,
    onNewMemberChange: (String) -> Unit,
    onAddMemberConfirmed: () -> Unit,
    onSetMemberActive: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(state.name, modifier = Modifier.semantics { heading() }) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            actions = {
                if (state.status == DetailStatus.Content) {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.group_menu))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (!state.archived) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.group_archive)) },
                                onClick = {
                                    menuOpen = false
                                    onArchiveRequested()
                                },
                            )
                        }
                        if (state.canDelete) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.group_delete)) },
                                onClick = {
                                    menuOpen = false
                                    onDeleteRequested()
                                },
                            )
                        }
                    }
                }
            },
        )
        when (state.status) {
            DetailStatus.Loading -> CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(Spacing.space8),
            )
            DetailStatus.NotFound -> EmptyState(
                message = stringResource(R.string.group_detail_unavailable),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            DetailStatus.Error -> EmptyState(
                message = stringResource(R.string.group_detail_error),
                actionLabel = stringResource(R.string.retry),
                onAction = onRetry,
            )
            DetailStatus.Content -> DetailContent(
                state = state,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onAddMemberRequested = onAddMemberRequested,
                onSetMemberActive = onSetMemberActive,
            )
        }
    }
    if (state.showArchiveDialog) {
        ConfirmDialog(
            title = stringResource(R.string.group_archive_title),
            body = stringResource(R.string.group_archive_body),
            confirmLabel = stringResource(R.string.group_archive),
            onConfirm = onArchiveConfirmed,
            onDismiss = onDialogsDismissed,
        )
    }
    if (state.showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.group_delete_title),
            body = stringResource(R.string.group_delete_body),
            confirmLabel = stringResource(R.string.group_delete),
            onConfirm = onDeleteConfirmed,
            onDismiss = onDialogsDismissed,
        )
    }
    if (state.showAddMember) {
        AddMemberDialog(
            value = state.newMember,
            error = state.memberError,
            enabled = !state.isBusy,
            onValueChange = onNewMemberChange,
            onConfirm = onAddMemberConfirmed,
            onDismiss = onAddMemberDismissed,
        )
    }
}

@Composable
private fun DetailContent(
    state: GroupDetailUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddMemberRequested: () -> Unit,
    onSetMemberActive: (String, Boolean) -> Unit,
) {
    val month = state.month
    val monthLabel = remember(month) {
        month?.let { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(it.start) }.orEmpty()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.space4, vertical = Spacing.space2),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4),
    ) {
        if (state.archived) {
            Text(
                text = stringResource(R.string.group_archived_banner),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (state.actionFailed) {
            Text(
                text = stringResource(R.string.group_action_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
            Text(
                text = stringResource(R.string.group_net_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            // Real net arrives with shared expenses (M3.3) and the balance engine (M4).
            Text(stringResource(R.string.group_net_settled), style = MaterialTheme.typography.headlineSmall)
        }
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = PillShape,
        ) {
            Text(stringResource(R.string.group_add_shared))
        }
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = PillShape,
        ) {
            Text(stringResource(R.string.group_balances))
        }
        Text(
            text = if (!state.archived && !state.canRecordShared) {
                stringResource(R.string.group_needs_members)
            } else {
                stringResource(R.string.group_actions_soon)
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(
                text = stringResource(R.string.group_spending_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            MonthSwitcher(label = monthLabel, onPrevious = onPreviousMonth, onNext = onNextMonth)
            MoneyText(paise = state.monthTotal)
            Text(
                text = stringResource(R.string.group_spending_description, formatInr(state.monthTotal), monthLabel),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(stringResource(R.string.group_no_events), style = MaterialTheme.typography.bodyMedium)
        }
        Column {
            Text(
                text = stringResource(R.string.group_members_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            state.members.forEach { member ->
                MemberRow(member, canEdit = !state.archived, onSetMemberActive = onSetMemberActive)
            }
            Text(
                text = stringResource(R.string.group_member_inactive_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = Spacing.space2),
            )
            if (!state.archived) {
                OutlinedButton(
                    onClick = onAddMemberRequested,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = PillShape,
                ) {
                    Text(stringResource(R.string.group_add_member))
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: MemberRowUi,
    canEdit: Boolean,
    onSetMemberActive: (String, Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val inactive = stringResource(R.string.group_member_inactive)
    ListItem(
        headlineContent = {
            Text(if (member.active) member.name else "${member.name} ($inactive)")
        },
        trailingContent = {
            if (canEdit && !member.isOwner) {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.group_member_menu, member.name),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (member.active) R.string.group_member_deactivate else R.string.group_member_activate,
                                ),
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onSetMemberActive(member.id, !member.active)
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun AddMemberDialog(
    value: String,
    error: MemberNameResult?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.group_add_member_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.group_member_label)) },
                    isError = error != null,
                    supportingText = error?.let { e -> { Text(memberErrorText(e)) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                )
                Text(stringResource(R.string.group_add_member_note), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.group_member_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun memberErrorText(error: MemberNameResult): String = when (error) {
    MemberNameResult.Blank -> stringResource(R.string.group_member_blank)
    MemberNameResult.TooLong -> stringResource(R.string.group_member_too_long, MAX_MEMBER_NAME_LENGTH)
    MemberNameResult.Duplicate -> stringResource(R.string.group_member_duplicate)
    MemberNameResult.GroupFull -> stringResource(R.string.group_member_full, MAX_MEMBERS)
    is MemberNameResult.Valid -> ""
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GroupDetailPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        GroupDetailScreen(
            state = GroupDetailUiState(
                status = DetailStatus.Content,
                name = "Flat",
                members = listOf(
                    MemberRowUi("1", "Me", isOwner = true, active = true),
                    MemberRowUi("2", "Alex", isOwner = false, active = true),
                    MemberRowUi("3", "Bea", isOwner = false, active = false),
                ),
                canRecordShared = true,
                canDelete = true,
                month = monthContaining(LocalDate.of(2026, 10, 1)),
                monthTotal = Paise(125_000),
            ),
            onBack = {}, onRetry = {}, onPreviousMonth = {}, onNextMonth = {},
            onArchiveRequested = {}, onArchiveConfirmed = {}, onDeleteRequested = {},
            onDeleteConfirmed = {}, onDialogsDismissed = {}, onAddMemberRequested = {},
            onAddMemberDismissed = {}, onNewMemberChange = {}, onAddMemberConfirmed = {},
            onSetMemberActive = { _, _ -> },
        )
    }
}
