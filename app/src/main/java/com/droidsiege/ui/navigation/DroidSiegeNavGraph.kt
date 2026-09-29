package com.droidsiege.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.droidsiege.ui.category.CategoryScreen
import com.droidsiege.ui.detail.ChallengeDetailScreen
import com.droidsiege.ui.hub.HubScreen
import com.droidsiege.ui.modules.ModuleScreen
import com.droidsiege.ui.modules.settings.SettingsScreen
import com.droidsiege.ui.shell.ShellViewModel
import com.droidsiege.ui.shell.SiegeAppDrawer
import com.droidsiege.ui.shell.SiegeAppScaffold
import com.droidsiege.ui.shell.SiegeModule
import kotlinx.coroutines.launch

@Composable
fun DroidSiegeNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val shellViewModel: ShellViewModel = hiltViewModel()
    val totalScore by shellViewModel.totalScore.collectAsStateWithLifecycle()

    val navigateToModule: (SiegeModule) -> Unit = { module ->
        scope.launch { drawerState.close() }
        navController.navigate(module.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SiegeAppDrawer(
                currentRoute = currentRoute,
                onDestinationClick = navigateToModule,
            )
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = SiegeModule.HUB.route,
        ) {
            composable(SiegeModule.HUB.route) {
                SiegeAppScaffold(
                    module = SiegeModule.HUB,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    totalScore = totalScore,
                ) { padding ->
                    HubScreen(
                        paddingValues = padding,
                        onCategoryClick = { categoryId ->
                            navController.navigate(Routes.category(categoryId))
                        },
                    )
                }
            }

            SiegeModule.topLevel
                .filter { it != SiegeModule.HUB && it != SiegeModule.SETTINGS }
                .forEach { module ->
                    composable(module.route) {
                        SiegeAppScaffold(
                            module = module,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            totalScore = totalScore,
                        ) { padding ->
                            ModuleScreen(
                                module = module,
                                paddingValues = padding,
                                onChallengeClick = { idKey ->
                                    navController.navigate(Routes.challenge(idKey))
                                },
                            )
                        }
                    }
                }

            composable(SiegeModule.SETTINGS.route) {
                SiegeAppScaffold(
                    module = SiegeModule.SETTINGS,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    totalScore = totalScore,
                ) { padding ->
                    SettingsScreen(paddingValues = padding)
                }
            }

            composable(
                route = Routes.CATEGORY,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            ) {
                CategoryScreen(
                    onBack = { navController.popBackStack() },
                    onChallengeClick = { idKey ->
                        navController.navigate(Routes.challenge(idKey))
                    },
                )
            }

            composable(
                route = Routes.CHALLENGE,
                arguments = listOf(navArgument("idKey") { type = NavType.StringType }),
            ) {
                ChallengeDetailScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
