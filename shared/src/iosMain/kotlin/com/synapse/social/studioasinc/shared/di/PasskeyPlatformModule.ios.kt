package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.data.passkey.IosPasskeyPlatformManager
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyPlatformManager
import org.koin.dsl.module

actual val passkeyPlatformModule = module {
    single<PasskeyPlatformManager> { IosPasskeyPlatformManager() }
}
