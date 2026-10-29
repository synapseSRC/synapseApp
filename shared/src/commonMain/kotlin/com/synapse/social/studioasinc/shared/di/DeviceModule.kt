package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.datasource.ChatParticipantSettingsDataSource
import com.synapse.social.studioasinc.shared.data.datasource.DeviceRegistrationDataSource
import com.synapse.social.studioasinc.shared.domain.usecase.device.RegisterDeviceUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.device.UpdateDevicePresenceUseCase
import org.koin.dsl.module

val deviceModule = module {
    single { DeviceRegistrationDataSource(SupabaseClient.client) }
    single { ChatParticipantSettingsDataSource(SupabaseClient.client) }
    
    single { RegisterDeviceUseCase(get()) }
    single { UpdateDevicePresenceUseCase(get()) }
}
