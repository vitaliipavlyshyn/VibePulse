package com.vpch.vibepulse.di

import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

private var koinRef: Koin? = null

fun initKoin(config: (KoinApplication) -> Unit = {}) {
    if (koinRef == null) {
        val app = startKoin {
            config(this)
            modules(networkModule + viewModelModule)
        }
        koinRef = app.koin
    }
}

expect fun platformModule(): Module

fun getKoin(): Koin =
    koinRef ?: throw Throwable("Koin is not initialized. Call initKoin() first.")