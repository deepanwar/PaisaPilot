package com.deepak.paisapilot.feature.groups.balances

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.deepak.paisapilot.R
import com.deepak.paisapilot.core.ui.EmptyState
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.feature.groups.detail.DetailStatus
import com.deepak.paisapilot.ui.theme.LedgerShapes
import com.deepak.paisapilot.ui.theme.LocalStatusColors
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.PillShape
import com.deepak.paisapilot.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesScreen(
    state: BalancesUiState,
    onBack: () -> Unit,
    onRecord: () -> Unit,
    onRecordSuggestion: (SuggestionUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.balances_title),
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
        )
        when (state.status) {
            DetailStatus.Loading -> CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(Spacing.space8),
            )
            DetailStatus.NotFound -> EmptyState(
                message = stringResource(R.string.balances_unavailable),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            DetailStatus.Error -> EmptyState(
                message = stringResource(R.string.balances_error),
                actionLabel = stringResource(R.string.back),
                onAction = onBack,
            )
            DetailStatus.Content -> BalancesContent(state, onRecord, onRecordSuggestion)
        }
    }
}

@Composable
private fun BalancesContent(
    state: BalancesUiState,
    onRecord: () -> Unit,
    onRecordSuggestion: (SuggestionUi) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4),
    ) {
        if (state.archived) {
            Text(
                text = stringResource(R.string.group_archived_banner),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(R.string.balances_basis),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.reviewBalances) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                shape = LedgerShapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(Spacing.space4)) {
                    Text(
                        text = stringResource(R.string.balances_review),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.balances_review_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        state.rows.forEach { BalanceRow(it) }
        if (state.allSettled && state.suggestions.isEmpty()) {
            Text(
                text = stringResource(R.string.balances_everyone_settled),
                style = MaterialTheme.typography.titleMedium,
                color = LocalStatusColors.current.settled,
            )
        } else if (state.suggestions.isNotEmpty()) {
            Text(
                text = stringResource(R.string.balances_suggested_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            state.suggestions.forEach { SuggestionRow(it, enabled = !state.archived, onClick = onRecordSuggestion) }
        }
        Text(
            text = stringResource(R.string.balances_no_transfer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onRecord,
            enabled = state.canRecord,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = PillShape,
        ) {
            Text(stringResource(R.string.balances_record))
        }
    }
}

@Composable
private fun personLabel(name: String, isOwner: Boolean): String =
    if (isOwner) stringResource(R.string.group_owner_chip, name) else name

@Composable
private fun BalanceRow(row: BalanceRowUi) {
    val name = personLabel(row.name, row.isOwner)
    val amount = formatInr(Paise(kotlin.math.abs(row.netPaise)))
    val colors = LocalStatusColors.current
    // Words carry the direction; color only reinforces it.
    val (text, color) = when {
        row.netPaise > 0L -> stringResource(R.string.shared_net_owed, name, amount) to colors.owed
        row.netPaise < 0L -> stringResource(R.string.shared_net_owes, name, amount) to colors.owe
        else -> stringResource(R.string.balances_settled_row, name) to colors.settled
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
private fun SuggestionRow(item: SuggestionUi, enabled: Boolean, onClick: (SuggestionUi) -> Unit) {
    val text = stringResource(
        R.string.balances_suggestion,
        personLabel(item.fromName, item.fromIsOwner),
        personLabel(item.toName, item.toIsOwner),
        formatInr(item.amount),
    )
    OutlinedButton(
        onClick = { onClick(item) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Text(text = text, modifier = Modifier.weight(1f))
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun BalancesPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        BalancesScreen(
            state = BalancesUiState(
                status = DetailStatus.Content,
                groupName = "Flat",
                rows = listOf(
                    BalanceRowUi("m1", "Me", true, 10_000),
                    BalanceRowUi("m2", "Alex", false, -6_000),
                    BalanceRowUi("m3", "Sam", false, -4_000),
                ),
                suggestions = listOf(
                    SuggestionUi("m2", "Alex", false, "m1", "Me", true, Paise(6_000)),
                    SuggestionUi("m3", "Sam", false, "m1", "Me", true, Paise(4_000)),
                ),
                allSettled = false,
                reviewBalances = true,
            ),
            onBack = {},
            onRecord = {},
            onRecordSuggestion = {},
        )
    }
}
