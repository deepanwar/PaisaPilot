package com.deepak.paisapilot.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.deepak.paisapilot.R
import com.deepak.paisapilot.feature.onboarding.OnboardingRoute as OnboardingScreenRoute
import com.deepak.paisapilot.feature.personal.form.ExpenseFormRoute as ExpenseFormScreenRoute
import com.deepak.paisapilot.ui.theme.Spacing

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
    val showForm = destination?.hasRoute<ExpenseFormRoute>() == true
    val tabs = shellTabs()
    val activity = LocalActivity.current
    val atRoot = navController.previousBackStackEntry?.destination is NavGraph ||
        navController.previousBackStackEntry == null
    // Navigation 2.10 leaves the graph under the start destination and does not finish the
    // activity on API 36, so the root destination finishes itself.
    BackHandler(enabled = atRoot) {
        activity?.finish()
    }

    Scaffold(
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
                // Temporary entry point until the real Today screen lands in M1.3.
                Box(modifier = Modifier.fillMaxSize()) {
                    PlaceholderHeading(stringResource(R.string.tab_today))
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(ExpenseFormRoute()) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Spacing.space4),
                    ) {
                        Text(stringResource(R.string.add_expense))
                    }
                }
            }
            composable<ExpenseFormRoute> {
                ExpenseFormScreenRoute(onClose = { navController.popBackStack() })
            }
            composable<ActivityRoute> { PlaceholderHeading(stringResource(R.string.tab_activity)) }
            composable<GroupsRoute> { PlaceholderHeading(stringResource(R.string.tab_groups)) }
            composable<InsightsRoute> { PlaceholderHeading(stringResource(R.string.tab_insights)) }
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
        title = { Text(stringResource(titleRes)) },
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
        modifier = Modifier.semantics { heading() },
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
