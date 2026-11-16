package com.vpch.vibepulse.di

import com.vpch.vibepulse.domain.model.AppTierConfig
import com.vpch.vibepulse.storage.PlaylistPreferencesStore
import com.vpch.vibepulse.storage.SecureTokenStorage
import com.vpch.vibepulse.storage.TokenStorage
import org.koin.dsl.module

val storageModule = module {

    single<TokenStorage> { SecureTokenStorage(settings = get()) }
    single { AppTierConfig(isPremium = true) }
    single { PlaylistPreferencesStore(get()) }
}