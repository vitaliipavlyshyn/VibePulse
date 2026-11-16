package com.vpch.vibepulse.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import com.vpch.vibepulse.domain.model.AppTierConfig
import org.koin.dsl.module

actual fun platformModule(): Module = androidModule

fun initKoin(application: Application, isPremium: Boolean = false) {
    initKoin { koinApplication ->
        koinApplication.apply {
            androidContext(application)
            modules(
                module {
                    single { AppTierConfig(isPremium = isPremium) }
                }
            )
        }
    }
}
