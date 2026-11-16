package com.vpch.vibepulse.screens.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.vpch.vibepulse.screens.auth.AuthScreen
import com.vpch.vibepulse.screens.home.HomeScreen
import com.vpch.vibepulse.screens.profile.ProfileScreen
import com.vpch.vibepulse.screens.statistics.StatisticsScreen
import com.vpch.vibepulse.ui.AppTheme
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Auth::class)
            subclass(Route.Home::class)
            subclass(Route.Statistics::class)
            subclass(Route.Profile::class)
        }
    }
}

@Composable
fun AppNavigation(
    isUserLogged: Boolean = false
) {
    val initialRoute: Route = if (isUserLogged) Route.Home else Route.Auth
    val backStack = rememberNavBackStack(navConfig, initialRoute)

    val currentRoute = backStack.lastOrNull() as? Route
    val showBottomBar = currentRoute in listOf(Route.Home, Route.Statistics, Route.Profile)

    val navigateToRoute: (Route) -> Unit = { targetRoute ->
        if (currentRoute != targetRoute) {
            backStack.removeAll { it is Route.Statistics || it is Route.Profile }
            if (targetRoute != Route.Home) {
                backStack.add(targetRoute)
            }
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.35f to AppTheme.colors.background.copy(alpha = 0.78f),
                                1.0f to AppTheme.colors.background.copy(alpha = 0.94f)
                            )
                        )
                ) {
                    VibePulseFloatingNavBar(
                        currentRoute = currentRoute,
                        onSelectRoute = navigateToRoute
                    )
                }
            }
        }
    ) { innerPadding ->
        val bottomInset = innerPadding.calculateBottomPadding()

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            entryProvider = entryProvider {
                entry<Route.Auth> {
                    AuthScreen(onLoginSuccess = {
                        backStack.clear()
                        backStack.add(Route.Home)
                    })
                }
                entry<Route.Home> {
                    Box(modifier = Modifier.padding(bottom = bottomInset)) {
                        HomeScreen(
                            onSessionExpired = {
                                backStack.clear()
                                backStack.add(Route.Auth)
                            },
                            onNavigateToProfile = {
                                navigateToRoute(Route.Profile)
                            }
                        )
                    }
                }
                entry<Route.Statistics> {
                    Box(modifier = Modifier.padding(bottom = bottomInset)) {
                        StatisticsScreen()
                    }
                }
                entry<Route.Profile> {
                    ProfileScreen(
                        bottomPadding = bottomInset,
                        onLogoutSuccess = {
                            backStack.clear()
                            backStack.add(Route.Auth)
                        }
                    )
                }
            }
        )
    }
}