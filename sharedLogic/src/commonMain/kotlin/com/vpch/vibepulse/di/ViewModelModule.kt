package com.vpch.vibepulse.di

import androidx.lifecycle.viewmodel.compose.viewModel
import com.vpch.vibepulse.di.viewModelModule
import com.vpch.vibepulse.presentation.auth.AuthViewModel
import com.vpch.vibepulse.presentation.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::HomeViewModel)
}