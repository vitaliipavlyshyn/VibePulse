package com.vpch.vibepulse

import android.app.Application
import com.vpch.vibepulse.di.initKoin
import com.vpch.vibepulse.ui.ThemeRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class VibePulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(this, isPremium = BuildConfig.IS_PREMIUM_TIER)

        loadKoinModules(
            module {
                single { ThemeRepositoryImpl(androidContext()) }
            }
        )
    }
}