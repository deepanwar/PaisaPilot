package com.deepak.paisapilot.feature.personal.detail

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
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.personal.PersonalExpense
import com.deepak.paisapilot.feature.personal.labelRes
import com.deepak.paisapilot.ui.theme.AmountTextStyle
import com.deepak.paisapilot.ui.theme.LedgerShapes
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    state: ExpenseDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = state.showDeleteDialog) { onDeleteCancelled() }
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.detail_title),
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
                if (state.expense != null && state.status == DetailStatus.Content) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.detail_edit),
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
                            text = { Text(stringResource(R.string.detail_delete)) },
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
                message = stringResource(R.string.expense_unavailable),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            DetailStatus.Error -> EmptyState(
                message = stringResource(R.string.detail_error),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            DetailStatus.Content -> state.expense?.let { DetailContent(it, state.deleteFailed) }
        }
    }
    val expense = state.expense
    if (state.showDeleteDialog && expense != null) {
        ConfirmDialog(
            title = stringResource(R.string.detail_delete_title),
            body = stringResource(
                R.string.detail_delete_body,
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
private fun DetailContent(expense: PersonalExpense, deleteFailed: Boolean) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val stampFormatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }
    fun stamp(millis: Long) =
        stampFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4),
    ) {
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
        Field(stringResource(R.string.detail_category), stringResource(expense.category.labelRes()))
        Field(stringResource(R.string.detail_date), dateFormatter.format(expense.date))
        expense.note?.let { Field(stringResource(R.string.detail_note), it) }
        Column {
            Text(
                text = stringResource(R.string.detail_added, stamp(expense.createdAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expense.updatedAt != expense.createdAt) {
                Text(
                    text = stringResource(R.string.detail_edited, stamp(expense.updatedAt)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (deleteFailed) {
            Text(
                text = stringResource(R.string.detail_delete_failed),
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
private fun ExpenseDetailPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        ExpenseDetailScreen(
            state = ExpenseDetailUiState(
                status = DetailStatus.Content,
                expense = PersonalExpense(
                    "1", Paise(12_550), "Tea", Category.Food,
                    LocalDate.of(2026, 10, 20), "with Sam", 1_760_000_000_000, 1_760_100_000_000,
                ),
            ),
            onBack = {}, onEdit = {}, onDeleteRequested = {}, onDeleteConfirmed = {}, onDeleteCancelled = {},
        )
    }
}
