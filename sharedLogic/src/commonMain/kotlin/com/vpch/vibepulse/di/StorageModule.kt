package com.vpch.vibepulse.di

import com.vpch.vibepulse.data.repository.local.LocalActivityRepositoryImpl
import com.vpch.vibepulse.data.repository.local.LocalUserRepositoryImpl
import com.vpch.vibepulse.domain.model.AppTierConfig
import com.vpch.vibepulse.domain.repository.LocalActivityRepository
import com.vpch.vibepulse.domain.repository.LocalUserRepository
import com.vpch.vibepulse.domain.repository.PreferencesRepository
import com.vpch.vibepulse.storage.PlaylistPreferencesStore
import com.vpch.vibepulse.storage.SecureTokenStorage
import com.vpch.vibepulse.storage.SubscriptionManager
import com.vpch.vibepulse.storage.TokenStorage
import org.koin.dsl.module

val storageModule = module {
    single<TokenStorage> { SecureTokenStorage(settings = get()) }
    single { AppTierConfig(isPremium = false) }
    single { SubscriptionManager(settings = get(), appTierConfig = get()) }
    single { PlaylistPreferencesStore(get()) }
    single<PreferencesRepository> { get<PlaylistPreferencesStore>() }
    single<LocalUserRepository> { LocalUserRepositoryImpl(get()) }
    single<LocalActivityRepository> { LocalActivityRepositoryImpl(get()) }
}