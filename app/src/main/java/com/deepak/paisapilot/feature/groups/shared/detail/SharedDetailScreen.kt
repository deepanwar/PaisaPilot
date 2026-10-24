package com.deepak.paisapilot.feature.groups.shared.detail

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.deepak.paisapilot.R
import com.deepak.paisapilot.core.ui.ConfirmDialog
import com.deepak.paisapilot.core.ui.EmptyState
import com.deepak.paisapilot.core.ui.MoneyText
import com.deepak.paisapilot.domain.group.Share
import com.deepak.paisapilot.domain.group.SharedExpense
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.feature.groups.detail.DetailStatus
import com.deepak.paisapilot.feature.personal.labelRes
import com.deepak.paisapilot.ui.theme.AmountTextStyle
import com.deepak.paisapilot.ui.theme.LedgerShapes
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedDetailScreen(
    state: SharedDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val canChange = state.expense != null && state.status == DetailStatus.Content && !state.readOnly
    BackHandler(enabled = state.showDeleteDialog) { onDeleteCancelled() }
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.shared_detail_title),
                    modifier = Modifier.semantics { heading() },
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            actions = {
                if (canChange) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.shared_detail_edit),
                        )
                    }
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.shared_detail_delete)) },
                            onClick = {
                                menuOpen = false
                                onDeleteRequested()
                            },
                        )
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
                message = stringResource(R.string.shared_unavailable),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            DetailStatus.Error -> EmptyState(
                message = stringResource(R.string.shared_detail_error),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            DetailStatus.Content -> state.expense?.let { DetailContent(state, it) }
        }
    }
    val expense = state.expense
    if (state.showDeleteDialog && expense != null) {
        ConfirmDialog(
            title = stringResource(R.string.shared_delete_title),
            body = stringResource(
                R.string.shared_delete_body,
                expense.title ?: stringResource(expense.category.labelRes()),
                formatInr(expense.amount),
            ),
            confirmLabel = stringResource(R.string.detail_delete_confirm),
            onConfirm = onDeleteConfirmed,
            onDismiss = onDeleteCancelled,
        )
    }
}

@Composable
private fun DetailContent(state: SharedDetailUiState, expense: SharedExpense) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4),
    ) {
        if (state.readOnly) {
            Text(
                text = stringResource(R.string.group_archived_banner),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = LedgerShapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(Spacing.space6)) {
                MoneyText(paise = expense.amount, style = AmountTextStyle)
                Text(
                    text = expense.title ?: stringResource(expense.category.labelRes()),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        Field(stringResource(R.string.detail_date), dateFormatter.format(expense.date))
        Field(stringResource(R.string.detail_category), stringResource(expense.category.labelRes()))
        Field(
            label = stringResource(R.string.shared_paid_by),
            value = ownerLabel(state.payerName, state.payerIsOwner),
        )
        Text(
            text = stringResource(R.string.shared_detail_split),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        state.shareLines.forEach { line ->
            val name = ownerLabel(line.name, line.isOwner)
            val amount = formatInr(Paise(line.paise))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                Text(text = amount, style = MaterialTheme.typography.bodyLarge)
            }
        }
        expense.note?.let { Field(stringResource(R.string.detail_note), it) }
        Text(
            text = stringResource(R.string.shared_detail_recorded_locally),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.deleteFailed) {
            Text(
                text = stringResource(R.string.shared_delete_failed),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ownerLabel(name: String, isOwner: Boolean): String =
    if (isOwner) stringResource(R.string.group_owner_chip, name) else name

@Composable
private fun Field(label: String, value: String) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun SharedDetailPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        SharedDetailScreen(
            state = SharedDetailUiState(
                status = DetailStatus.Content,
                expense = SharedExpense(
                    id = "e1",
                    groupId = "g1",
                    amount = Paise(20_000),
                    title = "Dinner",
                    category = Category.Food,
                    date = LocalDate.of(2026, 10, 24),
                    payerMemberId = "m1",
                    note = "Birthday",
                    shares = listOf(Share("m1", Paise(10_000)), Share("m2", Paise(10_000))),
                    createdAt = 0,
                    updatedAt = 0,
                ),
                payerName = "Me",
                payerIsOwner = true,
                shareLines = listOf(
                    ShareLineUi("m1", "Me", true, 10_000),
                    ShareLineUi("m2", "Alex", false, 10_000),
                ),
            ),
            onBack = {}, onEdit = {}, onDeleteRequested = {}, onDeleteConfirmed = {}, onDeleteCancelled = {},
        )
    }
}
