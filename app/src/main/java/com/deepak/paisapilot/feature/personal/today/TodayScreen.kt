package com.deepak.paisapilot.feature.personal.today

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.deepak.paisapilot.R
import com.deepak.paisapilot.core.ui.EmptyState
import com.deepak.paisapilot.core.ui.MonthSwitcher
import com.deepak.paisapilot.core.ui.MoneyText
import com.deepak.paisapilot.core.ui.TransactionRow
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.domain.time.monthContaining
import com.deepak.paisapilot.feature.personal.labelRes
import com.deepak.paisapilot.ui.theme.AmountTextStyle
import com.deepak.paisapilot.ui.theme.LedgerShapes
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun TodayScreen(
    state: TodayUiState,
    onAdd: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onViewAll: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            TodayUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(Spacing.space8),
            )
            is TodayUiState.Error -> Column(modifier = Modifier.fillMaxSize()) {
                EmptyState(
                    message = stringResource(R.string.today_error),
                    actionLabel = stringResource(R.string.retry),
                    onAction = onRetry,
                )
            }
            is TodayUiState.Content -> TodayContent(
                state = state,
                onOpenExpense = onOpenExpense,
                onViewAll = onViewAll,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onAdd = onAdd,
            )
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.space4),
        ) {
            Text(stringResource(R.string.add_expense))
        }
    }
}

@Composable
private fun TodayContent(
    state: TodayUiState.Content,
    onOpenExpense: (String) -> Unit,
    onViewAll: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAdd: () -> Unit,
) {
    val monthLabel = remember(state.month) {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(state.month.start)
    }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val heroLabel = stringResource(
        if (state.isCurrentMonth) R.string.today_personal_this_month else R.string.today_personal_month,
    )
    val heroDescription = stringResource(R.string.today_hero_description, heroLabel, formatInr(state.total))
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.space1),
    ) {
        item {
            MonthSwitcher(
                label = monthLabel,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                previousEnabled = state.canGoPrevious,
                nextEnabled = state.canGoNext,
                modifier = Modifier.padding(horizontal = Spacing.space2),
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.space4)
                    .semantics(mergeDescendants = true) { contentDescription = heroDescription },
                shape = LedgerShapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.space6),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space1),
                ) {
                    Text(text = heroLabel, style = MaterialTheme.typography.titleMedium)
                    MoneyText(paise = state.total, style = AmountTextStyle)
                }
            }
        }
        if (state.rows.isEmpty()) {
            item {
                EmptyState(
                    message = stringResource(
                        if (state.isCurrentMonth) R.string.today_empty else R.string.today_empty_month,
                    ),
                    actionLabel = stringResource(R.string.add_expense),
                    onAction = onAdd,
                )
            }
        } else {
            val byDate = state.rows.groupBy { it.date }
            byDate.forEach { (date, rows) ->
                item(key = "header-$date") {
                    Text(
                        text = dateFormatter.format(date),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = Spacing.space4, vertical = Spacing.space2)
                            .semantics { heading() },
                    )
                }
                items(rows, key = { it.id }) { row ->
                    val title = row.title ?: stringResource(row.category.labelRes())
                    val category = stringResource(row.category.labelRes())
                    val dateText = dateFormatter.format(row.date)
                    TransactionRow(
                        title = title,
                        subtitle = category,
                        amount = row.amount,
                        description = stringResource(
                            R.string.row_description,
                            title,
                            category,
                            dateText,
                            formatInr(row.amount),
                        ),
                        onClick = { onOpenExpense(row.id) },
                    )
                }
            }
            if (state.hasMore) {
                item {
                    TextButton(
                        onClick = onViewAll,
                        modifier = Modifier.padding(horizontal = Spacing.space2),
                    ) {
                        Text(stringResource(R.string.today_view_all))
                    }
                }
            }
        }
    }
}

private fun previewContent(rows: List<TodayRowUi>, total: Long) = TodayUiState.Content(
    month = monthContaining(LocalDate.of(2026, 10, 20)),
    total = Paise(total),
    rows = rows,
    hasMore = false,
    isCurrentMonth = true,
    canGoPrevious = true,
    canGoNext = true,
)

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun TodayScreenPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        TodayScreen(
            state = previewContent(
                listOf(
                    TodayRowUi("1", "Tea", Category.Food, LocalDate.of(2026, 10, 20), Paise(12_550)),
                    TodayRowUi("2", null, Category.Transport, LocalDate.of(2026, 10, 19), Paise(8_000)),
                ),
                20_550,
            ),
            onAdd = {}, onOpenExpense = {}, onViewAll = {}, onPreviousMonth = {}, onNextMonth = {}, onRetry = {},
        )
    }
}

@Preview(name = "Empty")
@Composable
private fun TodayScreenEmptyPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        TodayScreen(
            state = previewContent(emptyList(), 0),
            onAdd = {}, onOpenExpense = {}, onViewAll = {}, onPreviousMonth = {}, onNextMonth = {}, onRetry = {},
        )
    }
}

@Preview(name = "Error")
@Composable
private fun TodayScreenErrorPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        TodayScreen(
            state = TodayUiState.Error(monthContaining(LocalDate.of(2026, 10, 20))),
            onAdd = {}, onOpenExpense = {}, onViewAll = {}, onPreviousMonth = {}, onNextMonth = {}, onRetry = {},
        )
    }
}
