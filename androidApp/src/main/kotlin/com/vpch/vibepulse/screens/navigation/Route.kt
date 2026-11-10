package com.vpch.vibepulse.screens.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route: NavKey {

    @Serializable
    data object Auth : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Statistics : Route
}