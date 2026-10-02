package com.example.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.about.AboutScreen
import com.example.ui.apps.AppsScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.speedtest.SpeedTestScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object DashboardRoute
@Serializable data object AppsRoute
@Serializable data object HistoryRoute
@Serializable data object SpeedRoute
@Serializable data object SettingsRoute
@Serializable data object AboutRoute

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val labelRes: Int, val icon: ImageVector, val tag: String)

private val topLevel = listOf(
    TopLevel(DashboardRoute, DashboardRoute::class, R.string.nav_dashboard, Icons.Default.Home, "nav_dashboard"),
    TopLevel(AppsRoute, AppsRoute::class, R.string.nav_apps, Icons.AutoMirrored.Filled.ViewList, "nav_apps"),
    TopLevel(HistoryRoute, HistoryRoute::class, R.string.nav_history, Icons.Default.BarChart, "nav_history"),
    TopLevel(SpeedRoute, SpeedRoute::class, R.string.nav_speed, Icons.Default.Speed, "nav_speed")
)

/**
 * The app frame: top bar, bottom navigation and snackbar host. Shared by the real navigation
 * host and by screenshot tests so both render the same chrome.
 *
 * @param selectedTab index of the selected bottom tab, or null on a secondary screen (which
 *   shows a back arrow instead of the settings action and hides the bottom bar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetPulseChrome(
    title: String,
    selectedTab: Int?,
    onTabSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (selectedTab == null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (selectedTab != null) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("open_settings")) {
                            Icon(Icons.Default.Settings, stringResource(R.string.settings))
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedTab != null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    topLevel.forEachIndexed { index, item ->
                        val selected = selectedTab == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onTabSelected(index) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = {
                                Text(
                                    stringResource(item.labelRes),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.tag)
                        )
                    }
                }
            }
        },
        content = content
    )
}

/** Title shown in the top bar for bottom tab [index]. */
@Composable
fun topLevelTitle(index: Int): String =
    if (index == 0) stringResource(R.string.app_name) else stringResource(topLevel[index].labelRes)

@Composable
fun NetPulseNavHost(
    navController: NavHostController = rememberNavController()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val currentTab = topLevel
        .indexOfFirst { t -> destination?.hierarchy?.any { it.hasRoute(t.routeClass) } == true }
        .takeIf { it >= 0 }

    val title = when {
        currentTab != null -> topLevelTitle(currentTab)
        destination?.hasRoute(SettingsRoute::class) == true -> stringResource(R.string.settings)
        destination?.hasRoute(AboutRoute::class) == true -> stringResource(R.string.about)
        else -> ""
    }

    NetPulseChrome(
        title = title,
        selectedTab = currentTab,
        onTabSelected = { navController.navigateTopLevel(topLevel[it].route) },
        onBack = { navController.popBackStack() },
        onOpenSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
        snackbarHostState = snackbarHostState
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier.padding(padding)
        ) {
            composable<DashboardRoute> {
                DashboardScreen(
                    onOpenApps = { navController.navigateTopLevel(AppsRoute) },
                    onOpenSpeedTest = { navController.navigateTopLevel(SpeedRoute) },
                    onOpenHistory = { navController.navigateTopLevel(HistoryRoute) }
                )
            }
            composable<AppsRoute> { AppsScreen() }
            composable<HistoryRoute> { HistoryScreen() }
            composable<SpeedRoute> { SpeedTestScreen() }
            composable<SettingsRoute> {
                SettingsScreen(snackbarHostState = snackbarHostState, onOpenAbout = { navController.navigate(AboutRoute) })
            }
            composable<AboutRoute> { AboutScreen() }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
