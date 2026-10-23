package com.deepak.paisapilot.feature.groups.list

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.deepak.paisapilot.R
import com.deepak.paisapilot.core.ui.EmptyState
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.PillShape
import com.deepak.paisapilot.ui.theme.Spacing

@Composable
fun GroupListScreen(
    state: GroupListUiState,
    onNewGroup: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        GroupListUiState.Loading -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(Spacing.space8))
        }
        GroupListUiState.Error -> EmptyState(
            message = stringResource(R.string.groups_error),
            actionLabel = stringResource(R.string.retry),
            onAction = onRetry,
            modifier = modifier,
        )
        is GroupListUiState.Content ->
            if (state.active.isEmpty() && state.archived.isEmpty()) {
                EmptyState(
                    message = stringResource(R.string.groups_empty),
                    actionLabel = stringResource(R.string.groups_new),
                    onAction = onNewGroup,
                    modifier = modifier,
                )
            } else {
                GroupList(state, onNewGroup, onOpenGroup, modifier)
            }
    }
}

@Composable
private fun GroupList(
    state: GroupListUiState.Content,
    onNewGroup: () -> Unit,
    onOpenGroup: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.space2),
        verticalArrangement = Arrangement.spacedBy(Spacing.space1),
    ) {
        item {
            Button(
                onClick = onNewGroup,
                modifier = Modifier
                    .padding(horizontal = Spacing.space4, vertical = Spacing.space2)
                    .heightIn(min = 48.dp),
                shape = PillShape,
            ) {
                Text(stringResource(R.string.groups_new))
            }
        }
        items(state.active, key = { it.id }) { GroupRow(it, onOpenGroup) }
        if (state.archived.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.groups_archived_section),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(horizontal = Spacing.space4, vertical = Spacing.space2)
                        .semantics { heading() },
                )
            }
            items(state.archived, key = { it.id }) { GroupRow(it, onOpenGroup) }
        }
    }
}

@Composable
private fun GroupRow(row: GroupRowUi, onOpen: (String) -> Unit) {
    val members = stringResource(R.string.groups_row_members, row.memberCount)
    val net = stringResource(R.string.groups_row_settled)
    val description = stringResource(R.string.groups_row_description, row.name, row.memberCount, net) +
        if (row.archived) ", " + stringResource(R.string.groups_row_archived) else ""
    ListItem(
        headlineContent = { Text(row.name) },
        supportingContent = { Text(members) },
        trailingContent = { Text(net, style = MaterialTheme.typography.labelLarge) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onOpen(row.id) }
            .clearAndSetSemantics { contentDescription = description },
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GroupListPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        GroupListScreen(
            state = GroupListUiState.Content(
                active = listOf(GroupRowUi("1", "Flat", 3, false)),
                archived = listOf(GroupRowUi("2", "Goa trip", 5, true)),
            ),
            onNewGroup = {},
            onOpenGroup = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Empty")
@Composable
private fun GroupListEmptyPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        GroupListScreen(
            state = GroupListUiState.Content(emptyList(), emptyList()),
            onNewGroup = {},
            onOpenGroup = {},
            onRetry = {},
        )
    }
}
