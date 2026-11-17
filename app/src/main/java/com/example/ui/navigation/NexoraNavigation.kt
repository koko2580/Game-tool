package com.example.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.R
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.rememberNexoraHaptics
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GameCenterScreen
import com.example.ui.screens.GameTurboScreen
import com.example.ui.screens.HiddenSettingsScreen
import com.example.ui.screens.LabsScreen
import com.example.ui.screens.PerformanceMonitorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.GameCenterViewModel
import com.example.ui.viewmodel.GameTurboViewModel
import com.example.ui.viewmodel.HiddenSettingsViewModel
import com.example.ui.viewmodel.LabTab
import com.example.ui.viewmodel.LabsViewModel
import com.example.ui.viewmodel.PerformanceMonitorViewModel
import com.example.ui.viewmodel.SettingsViewModel

object NexoraRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_GAME_CENTER = "game_center"
    const val ROUTE_GAME_TURBO = "game_turbo"
    const val ROUTE_PERFORMANCE_MONITOR = "performance_monitor"
    const val ROUTE_HIDDEN_SETTINGS = "hidden_settings"
    const val ROUTE_LABS = "labs"
    const val ROUTE_LABS_PATTERN = "labs?tab={tab}"
    const val ROUTE_SETTINGS = "settings"
}

data class TopLevelDestination(
    val route: String,
    val baseMatch: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = NexoraRoutes.ROUTE_DASHBOARD,
        baseMatch = NexoraRoutes.ROUTE_DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Rounded.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    TopLevelDestination(
        route = NexoraRoutes.ROUTE_GAME_CENTER,
        baseMatch = NexoraRoutes.ROUTE_GAME_CENTER,
        labelRes = R.string.nav_games,
        selectedIcon = Icons.Rounded.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports
    ),
    TopLevelDestination(
        route = NexoraRoutes.ROUTE_PERFORMANCE_MONITOR,
        baseMatch = NexoraRoutes.ROUTE_PERFORMANCE_MONITOR,
        labelRes = R.string.nav_monitor,
        selectedIcon = Icons.Rounded.Speed,
        unselectedIcon = Icons.Outlined.Speed
    ),
    TopLevelDestination(
        route = NexoraRoutes.ROUTE_HIDDEN_SETTINGS,
        baseMatch = NexoraRoutes.ROUTE_HIDDEN_SETTINGS,
        labelRes = R.string.nav_shortcuts,
        selectedIcon = Icons.Rounded.DeveloperMode,
        unselectedIcon = Icons.Outlined.DeveloperMode
    ),
    TopLevelDestination(
        route = "${NexoraRoutes.ROUTE_LABS}?tab=network",
        baseMatch = NexoraRoutes.ROUTE_LABS,
        labelRes = R.string.nav_labs,
        selectedIcon = Icons.Rounded.Science,
        unselectedIcon = Icons.Outlined.Science
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexoraRootApp(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isSettingsRoute = currentDestination?.route == NexoraRoutes.ROUTE_SETTINGS
    val isTurboRoute = currentDestination?.route == NexoraRoutes.ROUTE_GAME_TURBO
    val isDetailRoute = isSettingsRoute || isTurboRoute
    val reducedMotion = LocalReducedMotion.current
    val triggerHaptic = rememberNexoraHaptics()

    val dashboardVm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            appContainer.telemetryRepository,
            appContainer.gamesRepository
        )
    )
    val gameCenterVm: GameCenterViewModel = viewModel(
        factory = GameCenterViewModel.Factory(appContainer.gamesRepository)
    )
    val gameTurboVm: GameTurboViewModel = viewModel(
        factory = GameTurboViewModel.Factory(
            appContainer.telemetryRepository,
            appContainer.gamesRepository,
            appContainer.userPreferencesRepository,
            appContainer.dndController,
            appContainer.mediaCaptureController,
            appContainer.systemSettingsNavigator
        )
    )
    val performanceVm: PerformanceMonitorViewModel = viewModel(
        factory = PerformanceMonitorViewModel.Factory(appContainer.telemetryRepository)
    )
    val hiddenSettingsVm: HiddenSettingsViewModel = viewModel(
        factory = HiddenSettingsViewModel.Factory(
            appContainer.systemSettingsNavigator,
            appContainer.deviceTelemetryManager
        )
    )
    val labsVm: LabsViewModel = viewModel(
        factory = LabsViewModel.Factory(
            appContainer.telemetryRepository,
            appContainer.systemSettingsNavigator
        )
    )
    val settingsVm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            appContainer.userPreferencesRepository,
            appContainer.telemetryRepository,
            appContainer.systemSettingsNavigator,
            appContainer.mediaCaptureController,
            appContainer.dndController
        )
    )

    val prefs by appContainer.userPreferencesRepository.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.preferences.UserPreferences()
    )

    val navigateTopLevel: (String) -> Unit = { route ->
        triggerHaptic()
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        if (isDetailRoute) {
                            IconButton(
                                onClick = {
                                    triggerHaptic()
                                    navController.popBackStack()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("nav_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_navigate_back)
                                )
                            }
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isDetailRoute) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "X",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = when {
                                    isSettingsRoute -> stringResource(R.string.settings_title)
                                    isTurboRoute -> stringResource(R.string.turbo_title)
                                    else -> stringResource(R.string.app_name)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        if (!isDetailRoute) {
                            val hasActiveSession = prefs.activeSession != null
                            Surface(
                                onClick = {
                                    triggerHaptic()
                                    navController.navigate(NexoraRoutes.ROUTE_GAME_TURBO) {
                                        launchSingleTop = true
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = if (hasActiveSession) {
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (hasActiveSession) {
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    }
                                ),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("nav_game_turbo")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = stringResource(R.string.turbo_title),
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.turbo_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    triggerHaptic()
                                    navController.navigate(NexoraRoutes.ROUTE_SETTINGS) {
                                        launchSingleTop = true
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("nav_settings")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.cd_open_settings),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail && !isDetailRoute) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        tonalElevation = 0.dp
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route?.startsWith(dest.baseMatch) == true
                            } == true
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_${dest.baseMatch}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !isDetailRoute) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(0.1f))
                        topLevelDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route?.startsWith(dest.baseMatch) == true
                            } == true
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_${dest.baseMatch}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = NexoraRoutes.ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f),
                    enterTransition = {
                        if (reducedMotion) {
                            EnterTransition.None
                        } else {
                            fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 14 }
                        }
                    },
                    exitTransition = {
                        if (reducedMotion) {
                            ExitTransition.None
                        } else {
                            fadeOut(tween(150)) +
                                slideOutHorizontally(tween(180)) { -it / 18 }
                        }
                    },
                    popEnterTransition = {
                        if (reducedMotion) {
                            EnterTransition.None
                        } else {
                            fadeIn(tween(200, easing = FastOutSlowInEasing))
                        }
                    },
                    popExitTransition = {
                        if (reducedMotion) {
                            ExitTransition.None
                        } else {
                            fadeOut(tween(150))
                        }
                    }
                ) {
                    composable(NexoraRoutes.ROUTE_DASHBOARD) {
                        DashboardScreen(
                            viewModel = dashboardVm,
                            onNavigateToGameTurbo = {
                                triggerHaptic()
                                navController.navigate(NexoraRoutes.ROUTE_GAME_TURBO) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToGames = { navigateTopLevel(NexoraRoutes.ROUTE_GAME_CENTER) },
                            onNavigateToMonitor = { navigateTopLevel(NexoraRoutes.ROUTE_PERFORMANCE_MONITOR) },
                            onNavigateToShortcuts = { navigateTopLevel(NexoraRoutes.ROUTE_HIDDEN_SETTINGS) },
                            onNavigateToNetworkLab = {
                                navigateTopLevel("${NexoraRoutes.ROUTE_LABS}?tab=network")
                            },
                            onNavigateToDisplayLab = {
                                navigateTopLevel("${NexoraRoutes.ROUTE_LABS}?tab=display")
                            }
                        )
                    }

                    composable(NexoraRoutes.ROUTE_GAME_CENTER) {
                        GameCenterScreen(viewModel = gameCenterVm)
                    }

                    composable(NexoraRoutes.ROUTE_GAME_TURBO) {
                        GameTurboScreen(
                            viewModel = gameTurboVm,
                            onOpenGameCenter = { navigateTopLevel(NexoraRoutes.ROUTE_GAME_CENTER) }
                        )
                    }

                    composable(NexoraRoutes.ROUTE_PERFORMANCE_MONITOR) {
                        PerformanceMonitorScreen(viewModel = performanceVm)
                    }

                    composable(NexoraRoutes.ROUTE_HIDDEN_SETTINGS) {
                        HiddenSettingsScreen(viewModel = hiddenSettingsVm)
                    }

                    composable(
                        route = NexoraRoutes.ROUTE_LABS_PATTERN,
                        arguments = listOf(
                            navArgument("tab") {
                                type = NavType.StringType
                                defaultValue = "network"
                            }
                        )
                    ) { entry ->
                        val tabArg = entry.arguments?.getString("tab")
                        val initialTab = if (tabArg == "display") LabTab.DISPLAY else LabTab.NETWORK
                        LabsScreen(
                            viewModel = labsVm,
                            initialTab = initialTab
                        )
                    }

                    composable(NexoraRoutes.ROUTE_SETTINGS) {
                        SettingsScreen(viewModel = settingsVm)
                    }
                }
            }
        }
    }
}
