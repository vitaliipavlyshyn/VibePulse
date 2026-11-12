package com.vpch.vibepulse.di

import com.vpch.vibepulse.presentation.auth.AuthViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class KoinHelper: KoinComponent {
    fun getAuthViewModel(): AuthViewModel = get()
}