package com.deepak.paisapilot.feature.activity

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.deepak.paisapilot.R
import com.deepak.paisapilot.core.ui.EmptyState
import com.deepak.paisapilot.core.ui.TransactionRow
import com.deepak.paisapilot.domain.activity.ActivityFilter
import com.deepak.paisapilot.domain.activity.ActivityScope
import com.deepak.paisapilot.domain.activity.ActivityType
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.domain.personal.Category
import com.deepak.paisapilot.feature.personal.labelRes
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun ActivityScreen(
    state: ActivityUiState,
    onQueryChange: (String) -> Unit,
    onScopeChange: (ActivityScope) -> Unit,
    onTypeChange: (ActivityType) -> Unit,
    onMonthChange: (LocalDate?) -> Unit,
    onCategoryChange: (Category?) -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    onOpenExpense: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            ActivityUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(Spacing.space8),
            )
            ActivityUiState.Error -> EmptyState(
                message = stringResource(R.string.activity_error),
                actionLabel = stringResource(R.string.retry),
                onAction = onRetry,
            )
            is ActivityUiState.Content -> if (!state.hasAnyData) {
                EmptyState(
                    message = stringResource(R.string.activity_empty),
                    actionLabel = stringResource(R.string.add_expense),
                    onAction = onAdd,
                )
            } else {
                ActivityList(
                    state = state,
                    listState = listState,
                    onQueryChange = onQueryChange,
                    onScopeChange = onScopeChange,
                    onTypeChange = onTypeChange,
                    onMonthChange = onMonthChange,
                    onCategoryChange = onCategoryChange,
                    onClearFilters = onClearFilters,
                    onOpenExpense = onOpenExpense,
                )
            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityList(
    state: ActivityUiState.Content,
    listState: LazyListState,
    onQueryChange: (String) -> Unit,
    onScopeChange: (ActivityScope) -> Unit,
    onTypeChange: (ActivityType) -> Unit,
    onMonthChange: (LocalDate?) -> Unit,
    onCategoryChange: (Category?) -> Unit,
    onClearFilters: () -> Unit,
    onOpenExpense: (String) -> Unit,
) {
    val filter = state.filter
    val focusManager = LocalFocusManager.current
    var queryText by rememberSaveable { mutableStateOf(filter.query) }
    LaunchedEffect(filter.query) {
        if (queryText != filter.query) queryText = filter.query
    }
    var monthDialog by rememberSaveable { mutableStateOf(false) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val badge = stringResource(R.string.activity_badge_personal)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.space4),
                verticalArrangement = Arrangement.spacedBy(Spacing.space2),
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = {
                        queryText = it
                        onQueryChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.activity_search_label)) },
                    singleLine = true,
                    trailingIcon = if (queryText.isNotEmpty()) {
                        {
                            IconButton(onClick = {
                                queryText = ""
                                onQueryChange("")
                            }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.activity_search_clear),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    ActivityScope.entries.forEach { scope ->
                        val label = stringResource(scopeLabel(scope))
                        FilterChip(
                            selected = filter.scope == scope,
                            onClick = { onScopeChange(scope) },
                            label = { Text(label) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    ActivityType.entries.forEach { type ->
                        val label = stringResource(typeLabel(type))
                        FilterChip(
                            selected = filter.type == type,
                            onClick = { onTypeChange(type) },
                            label = { Text(label) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    FilterChip(
                        selected = filter.month != null,
                        onClick = { monthDialog = true },
                        label = {
                            Text(
                                filter.month?.let(monthFormatter::format)
                                    ?: stringResource(R.string.activity_month_all),
                            )
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    FilterChip(
                        selected = filter.category == null,
                        onClick = { onCategoryChange(null) },
                        label = { Text(stringResource(R.string.activity_category_all)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    Category.entries.forEach { category ->
                        FilterChip(
                            selected = filter.category == category,
                            onClick = { onCategoryChange(category) },
                            label = { Text(stringResource(category.labelRes())) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                Text(
                    text = if (state.resultCount == 1) {
                        stringResource(R.string.activity_count_one)
                    } else {
                        stringResource(R.string.activity_count, state.resultCount)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                state.scopeTotal?.let {
                    Text(
                        text = stringResource(R.string.activity_scope_total, formatInr(it)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        if (state.rows.isEmpty()) {
            item {
                EmptyState(
                    message = stringResource(R.string.activity_no_matches),
                    actionLabel = stringResource(R.string.activity_clear_filters),
                    onAction = onClearFilters,
                )
            }
        } else {
            state.rows.groupBy { it.date }.forEach { (date, rows) ->
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
                    val category = stringResource(row.category.labelRes())
                    val title = row.title ?: category
                    TransactionRow(
                        title = title,
                        subtitle = "$badge · $category",
                        amount = row.amount,
                        description = stringResource(
                            R.string.activity_row_description,
                            badge,
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
    if (monthDialog) {
        MonthPickerDialog(
            months = state.availableMonths,
            selected = filter.month,
            formatter = monthFormatter,
            onSelect = {
                monthDialog = false
                onMonthChange(it)
            },
            onDismiss = { monthDialog = false },
        )
    }
}

private fun scopeLabel(scope: ActivityScope): Int = when (scope) {
    ActivityScope.All -> R.string.activity_scope_all
    ActivityScope.Personal -> R.string.activity_scope_personal
}

private fun typeLabel(type: ActivityType): Int = when (type) {
    ActivityType.All -> R.string.activity_type_all
    ActivityType.Expenses -> R.string.activity_type_expenses
}

@Composable
private fun MonthPickerDialog(
    months: List<LocalDate>,
    selected: LocalDate?,
    formatter: DateTimeFormatter,
    onSelect: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.activity_month_choose)) },
        text = {
            LazyColumn {
                item {
                    MonthOption(
                        label = stringResource(R.string.activity_month_all),
                        selected = selected == null,
                        onClick = { onSelect(null) },
                    )
                }
                items(months, key = { it.toEpochDay() }) { month ->
                    MonthOption(
                        label = formatter.format(month),
                        selected = month == selected,
                        onClick = { onSelect(month) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun MonthOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val description = if (selected) {
        stringResource(R.string.activity_filter_selected, label)
    } else {
        label
    }
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { contentDescription = description },
    ) {
        Text(text = label, modifier = Modifier.fillMaxWidth())
    }
}

private fun previewState(filter: ActivityFilter = ActivityFilter()) = ActivityUiState.Content(
    filter = filter,
    rows = listOf(
        ActivityRowUi("1", "Tea", Category.Food, LocalDate.of(2026, 10, 20), Paise(12_550)),
        ActivityRowUi("2", null, Category.Bills, LocalDate.of(2026, 10, 3), Paise(80_000)),
    ),
    resultCount = 2,
    hasAnyData = true,
    availableMonths = listOf(LocalDate.of(2026, 10, 1)),
    scopeTotal = null,
)

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ActivityScreenPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        ActivityScreen(
            state = previewState(),
            onQueryChange = {}, onScopeChange = {}, onTypeChange = {}, onMonthChange = {},
            onCategoryChange = {}, onClearFilters = {}, onRetry = {}, onAdd = {}, onOpenExpense = {},
        )
    }
}

@Preview(name = "No matches")
@Composable
private fun ActivityScreenNoMatchPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        ActivityScreen(
            state = previewState(ActivityFilter(query = "zzz")).copy(rows = emptyList(), resultCount = 0),
            onQueryChange = {}, onScopeChange = {}, onTypeChange = {}, onMonthChange = {},
            onCategoryChange = {}, onClearFilters = {}, onRetry = {}, onAdd = {}, onOpenExpense = {},
        )
    }
}
