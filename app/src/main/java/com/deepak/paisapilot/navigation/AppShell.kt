package com.deepak.paisapilot.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.deepak.paisapilot.core.undo.UndoSnackbarEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.deepak.paisapilot.R
import com.deepak.paisapilot.feature.activity.ActivityRoute as ActivityScreenRoute
import com.deepak.paisapilot.feature.groups.balances.BalancesRoute as BalancesScreenRoute
import com.deepak.paisapilot.feature.groups.detail.GroupDetailRoute as GroupDetailScreenRoute
import com.deepak.paisapilot.feature.groups.settlements.detail.SettlementDetailRoute as SettlementDetailScreenRoute
import com.deepak.paisapilot.feature.groups.settlements.form.SettlementFormRoute as SettlementFormScreenRoute
import com.deepak.paisapilot.feature.groups.shared.detail.SharedDetailRoute as SharedDetailScreenRoute
import com.deepak.paisapilot.feature.groups.shared.form.SharedFormRoute as SharedFormScreenRoute
import com.deepak.paisapilot.feature.groups.form.GroupFormRoute as GroupFormScreenRoute
import com.deepak.paisapilot.feature.groups.list.GroupListRoute as GroupListScreenRoute
import com.deepak.paisapilot.feature.insights.InsightsRoute as InsightsScreenRoute
import com.deepak.paisapilot.feature.onboarding.OnboardingRoute as OnboardingScreenRoute
import com.deepak.paisapilot.feature.personal.detail.ExpenseDetailRoute as ExpenseDetailScreenRoute
import com.deepak.paisapilot.feature.personal.form.ExpenseFormRoute as ExpenseFormScreenRoute
import com.deepak.paisapilot.feature.personal.today.TodayRoute as TodayScreenRoute

private data class TabDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selected: (NavDestination?) -> Boolean,
)

@Composable
fun AppShell(startAtOnboarding: Boolean) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showOnboarding = destination?.hasRoute<OnboardingRoute>() == true ||
        (destination == null && startAtOnboarding)
    val showSettings = destination?.hasRoute<SettingsRoute>() == true
    val showForm = destination?.hasRoute<ExpenseFormRoute>() == true ||
        destination?.hasRoute<ExpenseDetailRoute>() == true ||
        destination?.hasRoute<NewGroupRoute>() == true ||
        destination?.hasRoute<GroupDetailRoute>() == true ||
        destination?.hasRoute<SharedFormRoute>() == true ||
        destination?.hasRoute<SharedDetailRoute>() == true ||
        destination?.hasRoute<BalancesRoute>() == true ||
        destination?.hasRoute<SettlementFormRoute>() == true ||
        destination?.hasRoute<SettlementDetailRoute>() == true
    val tabs = shellTabs()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current
    val atRoot = navController.previousBackStackEntry?.destination is NavGraph ||
        navController.previousBackStackEntry == null
    // Navigation 2.10 leaves the graph under the start destination and does not finish the
    // activity on API 36, so the root destination finishes itself.
    BackHandler(enabled = atRoot) {
        activity?.finish()
    }

    UndoSnackbarEffect(snackbarHostState)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!showOnboarding && !showForm) {
                ShellTopBar(
                    titleRes = titleFor(destination),
                    showMenu = !showSettings,
                    showBack = showSettings,
                    onSettings = { navController.navigate(SettingsRoute) },
                    onBack = { navController.popBackStack() },
                )
            }
        },
        bottomBar = {
            if (!showOnboarding && !showSettings && !showForm) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = tab.selected(destination),
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startAtOnboarding) OnboardingRoute else TodayRoute,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable<OnboardingRoute> {
                OnboardingScreenRoute(
                    onCompleted = {
                        navController.navigate(TodayRoute) {
                            popUpTo<OnboardingRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<TodayRoute> {
                TodayScreenRoute(
                    onAdd = { navController.navigate(ExpenseFormRoute()) },
                    onOpenExpense = { navController.navigate(ExpenseDetailRoute(it)) },
                    onViewAll = { navController.navigateToTab(ActivityRoute) },
                )
            }
            composable<ExpenseDetailRoute> {
                ExpenseDetailScreenRoute(
                    onClose = { navController.popBackStack() },
                    onEdit = { navController.navigate(ExpenseFormRoute(it)) },
                )
            }
            composable<ExpenseFormRoute> {
                ExpenseFormScreenRoute(onClose = { navController.popBackStack() })
            }
            composable<ActivityRoute> {
                ActivityScreenRoute(
                    onAdd = { navController.navigate(ExpenseFormRoute()) },
                    onOpenExpense = { navController.navigate(ExpenseDetailRoute(it)) },
                )
            }
            composable<GroupsRoute> {
                GroupListScreenRoute(
                    onNewGroup = { navController.navigate(NewGroupRoute) },
                    onOpenGroup = { navController.navigate(GroupDetailRoute(it)) },
                )
            }
            composable<NewGroupRoute> {
                GroupFormScreenRoute(
                    onClose = { navController.popBackStack() },
                    onCreated = { id ->
                        navController.navigate(GroupDetailRoute(id)) {
                            popUpTo<NewGroupRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<GroupDetailRoute> { entry ->
                val groupId = entry.toRoute<GroupDetailRoute>().groupId
                GroupDetailScreenRoute(
                    onClose = { navController.popBackStack() },
                    onAddShared = { navController.navigate(SharedFormRoute(groupId)) },
                    onOpenShared = { navController.navigate(SharedDetailRoute(groupId, it)) },
                    onOpenBalances = { navController.navigate(BalancesRoute(groupId)) },
                    onOpenSettlement = { navController.navigate(SettlementDetailRoute(groupId, it)) },
                )
            }
            composable<SharedFormRoute> {
                SharedFormScreenRoute(onClose = { navController.popBackStack() })
            }
            composable<SharedDetailRoute> { entry ->
                val route = entry.toRoute<SharedDetailRoute>()
                SharedDetailScreenRoute(
                    onClose = { navController.popBackStack() },
                    onEdit = { navController.navigate(SharedFormRoute(route.groupId, it)) },
                )
            }
            composable<BalancesRoute> { entry ->
                val groupId = entry.toRoute<BalancesRoute>().groupId
                BalancesScreenRoute(
                    onClose = { navController.popBackStack() },
                    onRecord = { from, to, amount ->
                        navController.navigate(SettlementFormRoute(groupId, null, from, to, amount))
                    },
                )
            }
            composable<SettlementFormRoute> {
                SettlementFormScreenRoute(onClose = { navController.popBackStack() })
            }
            composable<SettlementDetailRoute> { entry ->
                val route = entry.toRoute<SettlementDetailRoute>()
                SettlementDetailScreenRoute(
                    onClose = { navController.popBackStack() },
                    onEdit = { navController.navigate(SettlementFormRoute(route.groupId, it)) },
                )
            }
            composable<InsightsRoute> {
                InsightsScreenRoute(
                    onOpenExpense = { navController.navigate(ExpenseDetailRoute(it)) },
                    onShowCategoryInActivity = { navController.navigateToTab(ActivityRoute) },
                    onAdd = { navController.navigate(ExpenseFormRoute()) },
                )
            }
            composable<SettingsRoute> { PlaceholderHeading(stringResource(R.string.settings_title)) }
        }
    }
}

@Composable
private fun shellTabs(): List<TabDestination> = listOf(
    TabDestination(TodayRoute, R.string.tab_today, Icons.Filled.Home) {
        it == null || it.hasRoute<TodayRoute>()
    },
        TabDestination(ActivityRoute, R.string.tab_activity, Icons.AutoMirrored.Filled.List) {
        it?.hasRoute<ActivityRoute>() == true
    },
    TabDestination(GroupsRoute, R.string.tab_groups, Icons.Filled.Person) {
        it?.hasRoute<GroupsRoute>() == true
    },
    TabDestination(InsightsRoute, R.string.tab_insights, Icons.Filled.Info) {
        it?.hasRoute<InsightsRoute>() == true
    },
)

@StringRes
private fun titleFor(destination: NavDestination?): Int = when {
    destination?.hasRoute<ActivityRoute>() == true -> R.string.tab_activity
    destination?.hasRoute<GroupsRoute>() == true -> R.string.tab_groups
    destination?.hasRoute<InsightsRoute>() == true -> R.string.tab_insights
    destination?.hasRoute<SettingsRoute>() == true -> R.string.settings_title
    else -> R.string.tab_today
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellTopBar(
    @StringRes titleRes: Int,
    showMenu: Boolean,
    showBack: Boolean,
    onSettings: () -> Unit,
    onBack: () -> Unit,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(
                text = stringResource(titleRes),
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        actions = {
            if (showMenu && titleRes == R.string.tab_today) {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_title)) },
                        onClick = {
                            menuOpen = false
                            onSettings()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun PlaceholderHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
    )
}

private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
