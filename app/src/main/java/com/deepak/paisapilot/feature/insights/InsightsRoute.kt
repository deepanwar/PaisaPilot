package com.deepak.paisapilot.feature.insights

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun InsightsRoute(
    onOpenExpense: (String) -> Unit,
    onShowCategoryInActivity: () -> Unit,
    onAdd: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InsightsScreen(
        state = state,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onCategory = {
            viewModel.onCategorySelected(it)
            onShowCategoryInActivity()
        },
        onOpenExpense = onOpenExpense,
        onAdd = onAdd,
        onRetry = viewModel::onRetry,
    )
}
