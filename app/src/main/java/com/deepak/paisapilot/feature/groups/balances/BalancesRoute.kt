package com.deepak.paisapilot.feature.groups.balances

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BalancesRoute(
    onClose: () -> Unit,
    onRecord: (fromId: String?, toId: String?, amountPaise: Long?) -> Unit,
    viewModel: BalancesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BalancesScreen(
        state = state,
        onBack = onClose,
        onRecord = { onRecord(null, null, null) },
        onRecordSuggestion = { onRecord(it.fromId, it.toId, it.amount.value) },
    )
}
