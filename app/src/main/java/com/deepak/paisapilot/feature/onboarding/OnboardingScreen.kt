package com.deepak.paisapilot.feature.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.deepak.paisapilot.R
import com.deepak.paisapilot.domain.profile.DisplayNameResult
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import com.deepak.paisapilot.ui.theme.PillShape
import com.deepak.paisapilot.ui.theme.Spacing

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.nameError) {
        if (state.nameError != null) focusRequester.requestFocus()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4),
    ) {
        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = stringResource(R.string.onboarding_privacy))
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(R.string.onboarding_name_label)) },
            isError = state.nameError != null,
            supportingText = state.nameError?.let { error ->
                { Text(nameErrorText(error)) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        Button(
            onClick = onStart,
            enabled = !state.saving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = PillShape,
        ) {
            Text(stringResource(R.string.onboarding_start))
        }
        TextButton(
            onClick = onSkip,
            enabled = !state.saving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.onboarding_skip))
        }
        if (state.saveFailed) {
            Text(text = stringResource(R.string.onboarding_save_failed))
            Button(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.onboarding_retry))
            }
        }
        Text(text = stringResource(R.string.onboarding_footer))
    }
}

@Composable
private fun nameErrorText(error: DisplayNameResult): String = when (error) {
    DisplayNameResult.Blank -> stringResource(R.string.onboarding_name_blank)
    DisplayNameResult.TooLong -> stringResource(R.string.onboarding_name_too_long)
    is DisplayNameResult.Valid -> ""
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingScreenPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        OnboardingScreen(
            state = OnboardingUiState(),
            onNameChange = {},
            onStart = {},
            onSkip = {},
            onRetry = {},
        )
    }
}
