package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.repository.PasskeyRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
import com.synapse.social.studioasinc.shared.domain.usecase.auth.AddPasskeyUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.LoadPasskeysUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.RemovePasskeyUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.SignInWithPasskeyUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

/** Platform implementations register [PasskeyPlatformManager] here. */
expect val passkeyPlatformModule: Module

val passkeyModule = module {
    includes(passkeyPlatformModule)

    single<PasskeyRepository> { PasskeyRepositoryImpl(SupabaseClient.client) }
    single { LoadPasskeysUseCase(get()) }
    single { RemovePasskeyUseCase(get()) }
    single { AddPasskeyUseCase(get(), get()) }
    single { SignInWithPasskeyUseCase(get(), get(), get()) }
}
