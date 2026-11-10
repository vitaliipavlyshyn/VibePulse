package com.vpch.vibepulse.screens.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.vpch.vibepulse.screens.auth.AuthScreen
import com.vpch.vibepulse.screens.home.HomeScreen
import com.vpch.vibepulse.screens.profile.ProfileScreen
import com.vpch.vibepulse.screens.statistics.StatisticsScreen
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


    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarItems.forEach {barItem ->
                        NavigationBarItem(
                            selected = currentRoute == barItem.route,
                            onClick = {
                                if (currentRoute != barItem.route) {
                                    backStack.removeAll {
                                        it is Route.Home || it is Route.Statistics || it is Route.Profile
                                    }
                                    if (backStack.lastOrNull() != barItem.route) {
                                        backStack.add(barItem.route)
                                    }
                                }
                            },
                            icon = {
                                Icon(imageVector = ImageVector.vectorResource(barItem.icon),
                                    contentDescription = barItem.contentDescription
                                )
                            },
                            label = {
                                Text(text = stringResource(barItem.text))
                            }
                        )
                    }

                }
            }
        }
    ) {innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {
                entry<Route.Auth> {
                    AuthScreen(onLoginSuccess =  {
                        backStack.clear()
                        backStack.add(Route.Home)
                    })
                }
                entry<Route.Home> {
                    HomeScreen()
                }
                entry<Route.Statistics> {
                    StatisticsScreen()
                }
                entry<Route.Profile> {
                    ProfileScreen()
                }
            }
        )

    }

}