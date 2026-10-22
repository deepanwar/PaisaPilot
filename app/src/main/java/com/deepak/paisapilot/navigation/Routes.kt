package com.deepak.paisapilot.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

@Serializable
data object TodayRoute

@Serializable
data object ActivityRoute

@Serializable
data object GroupsRoute

@Serializable
data object InsightsRoute

@Serializable
data object SettingsRoute

@Serializable
data class ExpenseFormRoute(val expenseId: String? = null)
