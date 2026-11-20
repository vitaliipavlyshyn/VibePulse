package com.vpch.vibepulse.di

import com.vpch.vibepulse.domain.repository.AuthRepository
import com.vpch.vibepulse.presentation.auth.AuthViewModel
import com.vpch.vibepulse.presentation.home.HomeViewModel
import com.vpch.vibepulse.presentation.profile.ProfileViewModel
import com.vpch.vibepulse.presentation.statistics.StatisticsViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class KoinHelper : KoinComponent {
    private val authRepository: AuthRepository = get()

    fun hasSession(): Boolean = authRepository.hasSession()
    fun getAuthViewModel(): AuthViewModel = get()
    fun getHomeViewModel(): HomeViewModel = get()
    fun getProfileViewModel(): ProfileViewModel = get()
    fun getStatisticsViewModel(): StatisticsViewModel = get()
}