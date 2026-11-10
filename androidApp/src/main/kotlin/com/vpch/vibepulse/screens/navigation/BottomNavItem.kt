package com.vpch.vibepulse.screens.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.vpch.vibepulse.R

data class BottomNavItem(
    val route: Route,
    @DrawableRes val icon: Int,
    @StringRes val text: Int,
    val contentDescription: String? = null
)


val bottomBarItems = listOf(
    BottomNavItem(
        route = Route.Home,
        icon = R.drawable.home_filled,
        text = R.string.home,
        contentDescription = "Home"
    ),
    BottomNavItem(
        route = Route.Statistics,
        icon = R.drawable.outline_history,
        text = R.string.statistics,
        contentDescription = "Statistics"
    ),
    BottomNavItem(
        route = Route.Profile,
        icon = R.drawable.account_circle,
        text = R.string.profile,
        contentDescription = "Profile"
    )
)