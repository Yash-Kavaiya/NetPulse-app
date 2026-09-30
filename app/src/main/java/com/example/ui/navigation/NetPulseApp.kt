package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.example.ui.insights.InsightsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.speedtest.SpeedTestScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object DashboardRoute
@Serializable data object AppsRoute
@Serializable data object HistoryRoute
@Serializable data object SpeedRoute
@Serializable data object InsightsRoute
@Serializable data object SettingsRoute
@Serializable data object AboutRoute

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val labelRes: Int, val icon: ImageVector, val tag: String)

private val topLevel = listOf(
    TopLevel(DashboardRoute, DashboardRoute::class, R.string.nav_dashboard, Icons.Default.Home, "nav_dashboard"),
    TopLevel(AppsRoute, AppsRoute::class, R.string.nav_apps, Icons.Default.ViewList, "nav_apps"),
    TopLevel(HistoryRoute, HistoryRoute::class, R.string.nav_history, Icons.Default.BarChart, "nav_history"),
    TopLevel(SpeedRoute, SpeedRoute::class, R.string.nav_speed, Icons.Default.Speed, "nav_speed"),
    TopLevel(InsightsRoute, InsightsRoute::class, R.string.nav_insights, Icons.Default.AutoAwesome, "nav_insights")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetPulseNavHost(
    navController: NavHostController = rememberNavController()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val currentTop = topLevel.firstOrNull { t -> destination?.hierarchy?.any { it.hasRoute(t.routeClass) } == true }

    val title = when {
        currentTop != null -> if (currentTop.route == DashboardRoute) stringResource(R.string.app_name) else stringResource(currentTop.labelRes)
        destination?.hasRoute(SettingsRoute::class) == true -> stringResource(R.string.settings)
        destination?.hasRoute(AboutRoute::class) == true -> stringResource(R.string.about)
        else -> ""
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            run {
                CenterAlignedTopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (currentTop == null) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                            }
                        }
                    },
                    actions = {
                        if (currentTop != null) {
                            IconButton(
                                onClick = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
                                modifier = Modifier.testTag("open_settings")
                            ) {
                                Icon(Icons.Default.Settings, stringResource(R.string.settings))
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentTop != null) {
                NavigationBar {
                    topLevel.forEach { item ->
                        NavigationBarItem(
                            selected = currentTop == item,
                            onClick = { navController.navigateTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes), style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag(item.tag)
                        )
                    }
                }
            }
        }
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
                    onOpenInsights = { navController.navigateTopLevel(InsightsRoute) }
                )
            }
            composable<AppsRoute> { AppsScreen() }
            composable<HistoryRoute> { HistoryScreen() }
            composable<SpeedRoute> { SpeedTestScreen() }
            composable<InsightsRoute> { InsightsScreen() }
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
