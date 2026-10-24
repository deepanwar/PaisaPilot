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

@Serializable
data class ExpenseDetailRoute(val expenseId: String)

@Serializable
data object NewGroupRoute

@Serializable
data class GroupDetailRoute(val groupId: String)

@Serializable
data class SharedFormRoute(val groupId: String, val expenseId: String? = null)

@Serializable
data class SharedDetailRoute(val groupId: String, val expenseId: String)

@Serializable
data class BalancesRoute(val groupId: String)

@Serializable
data class SettlementFormRoute(
    val groupId: String,
    val settlementId: String? = null,
    val fromId: String? = null,
    val toId: String? = null,
    val amountPaise: Long? = null,
)

@Serializable
data class SettlementDetailRoute(val groupId: String, val settlementId: String)
