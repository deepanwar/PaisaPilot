package com.deepak.paisapilot.feature.personal.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TodayRoute(
    onAdd: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onViewAll: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TodayScreen(
        state = state,
        onAdd = onAdd,
        onOpenExpense = onOpenExpense,
        onViewAll = onViewAll,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onRetry = viewModel::onRetry,
    )
}
