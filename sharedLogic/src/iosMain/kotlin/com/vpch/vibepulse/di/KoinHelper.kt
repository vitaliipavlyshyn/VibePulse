package com.vpch.vibepulse.di

import com.vpch.vibepulse.presentation.auth.AuthViewModel
import com.vpch.vibepulse.presentation.home.HomeViewModel
import com.vpch.vibepulse.presentation.profile.ProfileViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class KoinHelper : KoinComponent {
    fun getAuthViewModel(): AuthViewModel = get()
    fun getHomeViewModel(): HomeViewModel = get()
    fun getProfileViewModel(): ProfileViewModel = get()
}