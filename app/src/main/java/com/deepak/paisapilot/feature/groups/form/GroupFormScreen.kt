package com.deepak.paisapilot.feature.groups.form

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.deepak.paisapilot.core.ui.ConfirmDialog
import com.deepak.paisapilot.core.ui.StickySaveBar
import com.deepak.paisapilot.domain.group.MAX_GROUP_NAME_LENGTH
import com.deepak.paisapilot.domain.group.MAX_MEMBERS
import com.deepak.paisapilot.domain.group.MAX_MEMBER_NAME_LENGTH
import com.deepak.paisapilot.domain.group.MemberNameResult
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupFormScreen(
    state: GroupFormUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onNewMemberChange: (String) -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.isDirty) { onBack() }
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(stringResource(R.string.group_new_title), modifier = Modifier.semantics { heading() })
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.space4, vertical = Spacing.space2),
            verticalArrangement = Arrangement.spacedBy(Spacing.space4),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.group_name_label)) },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { error ->
                    {
                        Text(
                            when (error) {
                                NameError.Blank -> stringResource(R.string.group_name_required)
                                NameError.TooLong -> stringResource(R.string.group_name_too_long, MAX_GROUP_NAME_LENGTH)
                            },
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Text(
                text = stringResource(R.string.group_members_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.group_members_count, state.memberCount),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(R.string.group_owner_chip, state.ownerName)) },
                )
                state.members.forEach { member ->
                    val removeLabel = stringResource(R.string.group_member_remove, member)
                    InputChip(
                        selected = false,
                        onClick = { onRemoveMember(member) },
                        label = { Text(member) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.semantics { contentDescription = removeLabel },
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedTextField(
                    value = state.newMember,
                    onValueChange = onNewMemberChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.group_member_label)) },
                    isError = state.memberError != null,
                    supportingText = state.memberError?.let { error -> { Text(memberErrorText(error)) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddMember() }),
                )
                OutlinedButton(
                    onClick = onAddMember,
                    modifier = Modifier
                        .padding(top = Spacing.space2)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.group_member_add))
                }
            }
            if (state.tooFewMembers) {
                Text(
                    text = stringResource(R.string.group_members_min),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Text(
                text = stringResource(R.string.group_local_note),
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.saveFailed) {
                Text(
                    text = stringResource(R.string.group_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        StickySaveBar(
            enabled = !state.isSaving && state.createdGroupId == null,
            onSave = onSave,
            label = stringResource(R.string.group_create),
        )
    }
    if (state.showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(R.string.group_discard_title),
            body = stringResource(R.string.group_discard_body),
            confirmLabel = stringResource(R.string.group_discard_confirm),
            onConfirm = onDiscard,
            onDismiss = onKeepEditing,
        )
    }
}

@Composable
private fun memberErrorText(error: MemberNameResult): String = when (error) {
    MemberNameResult.Blank -> stringResource(R.string.group_member_blank)
    MemberNameResult.TooLong -> stringResource(R.string.group_member_too_long, MAX_MEMBER_NAME_LENGTH)
    MemberNameResult.Duplicate -> stringResource(R.string.group_member_duplicate)
    MemberNameResult.GroupFull -> stringResource(R.string.group_member_full, MAX_MEMBERS)
    is MemberNameResult.Valid -> ""
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GroupFormPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        GroupFormScreen(
            state = GroupFormUiState(name = "Flat", ownerName = "Me", members = listOf("Alex", "Bea")),
            onBack = {},
            onNameChange = {},
            onNewMemberChange = {},
            onAddMember = {},
            onRemoveMember = {},
            onSave = {},
            onDiscard = {},
            onKeepEditing = {},
        )
    }
}
