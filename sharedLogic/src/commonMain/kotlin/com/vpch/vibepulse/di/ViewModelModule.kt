package com.vpch.vibepulse.di

import com.vpch.vibepulse.presentation.auth.AuthViewModel
import com.vpch.vibepulse.presentation.home.HomeViewModel
import com.vpch.vibepulse.presentation.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::ProfileViewModel)
}