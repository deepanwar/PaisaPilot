package com.deepak.paisapilot.feature.activity

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ActivityRoute(
    onAdd: () -> Unit,
    onOpenExpense: (String) -> Unit,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ActivityScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onScopeChange = viewModel::onScopeChange,
        onTypeChange = viewModel::onTypeChange,
        onMonthChange = viewModel::onMonthChange,
        onCategoryChange = viewModel::onCategoryChange,
        onClearFilters = viewModel::onClearFilters,
        onRetry = viewModel::onRetry,
        onAdd = onAdd,
        onOpenExpense = onOpenExpense,
        listState = rememberLazyListState(),
    )
}
