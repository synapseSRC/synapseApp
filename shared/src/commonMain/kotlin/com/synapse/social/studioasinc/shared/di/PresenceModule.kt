package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.data.repository.SupabasePresenceRepository
import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserPresenceUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.StartPresenceTrackingUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.UpdatePresenceUseCase
import org.koin.dsl.module

val presenceModule = module {
    single<PresenceRepository> { SupabasePresenceRepository() }
    single { UpdatePresenceUseCase(get()) }
    single { StartPresenceTrackingUseCase(get()) }
    single { ObserveUserPresenceUseCase(get()) }
}
