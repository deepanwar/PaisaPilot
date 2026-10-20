package com.deepak.paisapilot.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingRoute(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted()
    }
    OnboardingScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onStart = viewModel::onStart,
        onSkip = viewModel::onSkip,
        onRetry = viewModel::onRetry,
    )
}
