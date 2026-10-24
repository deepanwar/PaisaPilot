package com.deepak.paisapilot.feature.groups.settlements.detail

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.deepak.paisapilot.domain.group.Settlement
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.feature.groups.detail.DetailStatus
import com.deepak.paisapilot.ui.theme.AmountTextStyle
import com.deepak.paisapilot.ui.theme.LedgerShapes
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementDetailScreen(
    state: SettlementDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val canChange = state.settlement != null && state.status == DetailStatus.Content && !state.readOnly
    BackHandler(enabled = state.showDeleteDialog) { onDeleteCancelled() }
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settlement_detail_title),
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
                            contentDescription = stringResource(R.string.settlement_detail_edit),
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
                            text = { Text(stringResource(R.string.settlement_detail_delete)) },
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
                message = stringResource(R.string.settlement_unavailable),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            DetailStatus.Error -> EmptyState(
                message = stringResource(R.string.settlement_load_failed),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            DetailStatus.Content -> state.settlement?.let { DetailContent(state, it) }
        }
    }
    val settlement = state.settlement
    if (state.showDeleteDialog && settlement != null) {
        ConfirmDialog(
            title = stringResource(R.string.settlement_delete_title),
            body = stringResource(
                R.string.settlement_delete_body,
                paidPhrase(state),
                formatInr(settlement.amount),
            ),
            confirmLabel = stringResource(R.string.detail_delete_confirm),
            onConfirm = onDeleteConfirmed,
            onDismiss = onDeleteCancelled,
        )
    }
}

@Composable
private fun ownerLabel(name: String, isOwner: Boolean): String =
    if (isOwner) stringResource(R.string.group_owner_chip, name) else name

@Composable
private fun paidPhrase(state: SettlementDetailUiState): String = stringResource(
    R.string.settlement_detail_paid,
    ownerLabel(state.fromName, state.fromIsOwner),
    ownerLabel(state.toName, state.toIsOwner),
)

@Composable
private fun DetailContent(state: SettlementDetailUiState, settlement: Settlement) {
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
                MoneyText(paise = settlement.amount, style = AmountTextStyle)
                Text(text = paidPhrase(state), style = MaterialTheme.typography.titleLarge)
            }
        }
        Field(
            stringResource(R.string.detail_date),
            dateFormatter.format(LocalDate.ofEpochDay(settlement.occurredEpochDay)),
        )
        settlement.note?.let { Field(stringResource(R.string.detail_note), it) }
        Text(
            text = stringResource(R.string.settlement_detail_record_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.deleteFailed) {
            Text(
                text = stringResource(R.string.settlement_delete_failed),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

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
private fun SettlementDetailPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        SettlementDetailScreen(
            state = SettlementDetailUiState(
                status = DetailStatus.Content,
                settlement = Settlement(
                    id = "s1",
                    groupId = "g1",
                    fromMemberId = "m2",
                    toMemberId = "m1",
                    amount = Paise(6_000),
                    occurredEpochDay = LocalDate.of(2026, 10, 24).toEpochDay(),
                    note = "Cash",
                    createdAt = 0,
                    updatedAt = 0,
                ),
                fromName = "Alex",
                toName = "Me",
                toIsOwner = true,
            ),
            onBack = {}, onEdit = {}, onDeleteRequested = {}, onDeleteConfirmed = {}, onDeleteCancelled = {},
        )
    }
}
