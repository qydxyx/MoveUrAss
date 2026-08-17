package com.kegeltrainer.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kegeltrainer.app.ui.home.HomeScreen
import com.kegeltrainer.app.ui.knowledge.ArticleScreen
import com.kegeltrainer.app.ui.knowledge.KnowledgeScreen
import com.kegeltrainer.app.ui.library.LibraryScreen
import com.kegeltrainer.app.ui.library.WorkoutDetailScreen
import com.kegeltrainer.app.ui.onboarding.OnboardingScreen
import com.kegeltrainer.app.ui.player.PlayerScreen
import com.kegeltrainer.app.ui.progress.ProgressScreen
import com.kegeltrainer.app.ui.settings.DisclaimerScreen
import com.kegeltrainer.app.ui.settings.SettingsScreen
import com.kegeltrainer.app.ui.settings.SettingsViewModel
import androidx.compose.material3.MaterialTheme

object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val PLAYER = "player/{workoutId}?plan={plan}"
    const val DETAIL = "detail/{workoutId}"
    const val ARTICLE = "article/{articleId}"
    const val DISCLAIMER = "disclaimer"

    fun player(workoutId: String, plan: Boolean) = "player/$workoutId?plan=$plan"
    fun detail(workoutId: String) = "detail/$workoutId"
    fun article(id: String) = "article/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "今日", Icons.Outlined.Home),
    Tab("library", "课程", Icons.Outlined.FitnessCenter),
    Tab("progress", "履历", Icons.Outlined.CalendarMonth),
    Tab("profile", "我的", Icons.Outlined.Person),
)

@Composable
fun AppRoot(vm: SettingsViewModel = hiltViewModel()) {
    val onboarded by vm.onboarded.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    if (onboarded == null) return
    NavHost(
        navController = nav,
        startDestination = if (onboarded == true) Routes.MAIN else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    nav.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainTabs(
                onStart = { id, plan -> nav.navigate(Routes.player(id, plan)) },
                onDetail = { nav.navigate(Routes.detail(it)) },
                onArticle = { nav.navigate(Routes.article(it)) },
                onDisclaimer = { nav.navigate(Routes.DISCLAIMER) },
                onRedoOnboarding = {
                    nav.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("workoutId") { type = NavType.StringType },
                navArgument("plan") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            PlayerScreen(onExit = { nav.popBackStack() })
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType }),
        ) {
            WorkoutDetailScreen(
                onBack = { nav.popBackStack() },
                onStart = { id -> nav.navigate(Routes.player(id, false)) },
            )
        }
        composable(
            route = Routes.ARTICLE,
            arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
        ) {
            ArticleScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.DISCLAIMER) {
            DisclaimerScreen(onBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun MainTabs(
    onStart: (String, Boolean) -> Unit,
    onDetail: (String) -> Unit,
    onArticle: (String) -> Unit,
    onDisclaimer: () -> Unit,
    onRedoOnboarding: () -> Unit,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                tabs.forEach { tab ->
                    val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true
                    val colors = MaterialTheme.colorScheme
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.primary,
                            selectedTextColor = colors.primary,
                            unselectedIconColor = colors.onSurfaceVariant,
                            unselectedTextColor = colors.onSurfaceVariant,
                            indicatorColor = colors.primary.copy(alpha = 0.16f),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                HomeScreen(onStart = onStart, onDetail = onDetail)
            }
            composable("library") {
                LibraryScreen(onOpen = onDetail)
            }
            composable("progress") {
                ProgressScreen()
            }
            composable("profile") {
                SettingsScreen(
                    onArticle = onArticle,
                    onDisclaimer = onDisclaimer,
                    onKnowledge = { nav.navigate("knowledge") },
                    onRedoOnboarding = onRedoOnboarding,
                )
            }
            composable("knowledge") {
                KnowledgeScreen(onOpen = onArticle, onBack = { nav.popBackStack() })
            }
        }
    }
}
