package com.deepak.paisapilot.feature.onboarding

import com.deepak.paisapilot.domain.profile.DisplayNameResult

data class OnboardingUiState(
    val name: String = "Me",
    val nameError: DisplayNameResult? = null,
    val saveFailed: Boolean = false,
    val saving: Boolean = false,
    val completed: Boolean = false,
)
