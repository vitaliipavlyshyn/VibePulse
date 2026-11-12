package com.vpch.vibepulse.di

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module

@OptIn(ExperimentalSettingsImplementation::class)
val iosModule = module {
    single<Settings> {
        KeychainSettings(service = "com.vpch.vibepulse")
    }
}