package com.vpch.vibepulse.di

import androidx.lifecycle.viewmodel.compose.viewModel
import com.vpch.vibepulse.presentation.auth.AuthViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::AuthViewModel)
}