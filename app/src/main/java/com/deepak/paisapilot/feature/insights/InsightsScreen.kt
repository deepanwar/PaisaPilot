package com.deepak.paisapilot.feature.insights

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.deepak.paisapilot.R
import com.deepak.paisapilot.core.ui.EmptyState
import com.deepak.paisapilot.core.ui.MoneyText
import com.deepak.paisapilot.core.ui.MonthSwitcher
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
fun InsightsScreen(
    state: InsightsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCategory: (Category) -> Unit,
    onOpenExpense: (String) -> Unit,
    onAdd: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            InsightsUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(Spacing.space8),
            )
            is InsightsUiState.Error -> EmptyState(
                message = stringResource(R.string.insights_error),
                actionLabel = stringResource(R.string.retry),
                onAction = onRetry,
            )
            is InsightsUiState.Content -> InsightsContent(
                state = state,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onCategory = onCategory,
                onOpenExpense = onOpenExpense,
                onAdd = onAdd,
            )
        }
    }
}

@Composable
private fun InsightsContent(
    state: InsightsUiState.Content,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCategory: (Category) -> Unit,
    onOpenExpense: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val monthLabel = remember(state.month) {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(state.month.start)
    }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val countText = if (state.count == 1) {
        stringResource(R.string.insights_count_one)
    } else {
        stringResource(R.string.insights_count, state.count)
    }
    val heroDescription = stringResource(
        R.string.insights_hero_description,
        stringResource(R.string.insights_hero_label),
        formatInr(state.total),
        countText,
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.space6),
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
                    Text(
                        text = stringResource(R.string.insights_hero_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    MoneyText(paise = state.total, style = AmountTextStyle)
                    Text(text = countText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (state.count == 0) {
            item {
                EmptyState(
                    message = stringResource(R.string.insights_empty),
                    actionLabel = stringResource(R.string.add_expense),
                    onAction = onAdd,
                )
            }
        } else {
            item {
                SectionTitle(stringResource(R.string.insights_by_category))
            }
            items(state.categories, key = { it.category.key }) { row ->
                CategoryRow(row = row, onClick = { onCategory(row.category) })
            }
            item {
                SectionTitle(stringResource(R.string.insights_recent))
            }
            items(state.recent, key = { "recent-" + it.id }) { row ->
                val category = stringResource(row.category.labelRes())
                val title = row.title ?: category
                TransactionRow(
                    title = title,
                    subtitle = "$category · ${dateFormatter.format(row.date)}",
                    amount = row.amount,
                    description = stringResource(
                        R.string.row_description,
                        title,
                        category,
                        dateFormatter.format(row.date),
                        formatInr(row.amount),
                    ),
                    onClick = { onOpenExpense(row.id) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(horizontal = Spacing.space4)
            .padding(top = Spacing.space4, bottom = Spacing.space1),
    )
}

@Composable
private fun CategoryRow(row: CategoryRowUi, onClick: () -> Unit) {
    val label = stringResource(row.category.labelRes())
    val description = stringResource(
        R.string.insights_category_description,
        label,
        formatInr(row.amount),
        row.percent,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            }
            .padding(horizontal = Spacing.space4, vertical = Spacing.space2),
        verticalArrangement = Arrangement.spacedBy(Spacing.space1),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space3),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.insights_percent, row.percent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyText(paise = row.amount, style = MaterialTheme.typography.titleMedium)
        }
        // Decorative: the amount and percentage above carry all the information.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
                .clearAndSetSemantics {},
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(row.fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
            )
        }
    }
}

private fun previewContent(empty: Boolean) = InsightsUiState.Content(
    month = monthContaining(LocalDate.of(2026, 10, 20)),
    isCurrentMonth = true,
    total = Paise(if (empty) 0 else 15_000),
    count = if (empty) 0 else 2,
    categories = if (empty) {
        emptyList()
    } else {
        listOf(
            CategoryRowUi(Category.Food, Paise(10_000), 67, 0.67f),
            CategoryRowUi(Category.Bills, Paise(5_000), 33, 0.33f),
        )
    },
    recent = if (empty) {
        emptyList()
    } else {
        listOf(RecentRowUi("1", "Tea", Category.Food, LocalDate.of(2026, 10, 20), Paise(10_000)))
    },
    canGoPrevious = true,
    canGoNext = true,
)

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun InsightsScreenPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        InsightsScreen(
            state = previewContent(empty = false),
            onPreviousMonth = {}, onNextMonth = {}, onCategory = {}, onOpenExpense = {},
            onAdd = {}, onRetry = {},
        )
    }
}

@Preview(name = "Empty")
@Composable
private fun InsightsScreenEmptyPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        InsightsScreen(
            state = previewContent(empty = true),
            onPreviousMonth = {}, onNextMonth = {}, onCategory = {}, onOpenExpense = {},
            onAdd = {}, onRetry = {},
        )
    }
}
