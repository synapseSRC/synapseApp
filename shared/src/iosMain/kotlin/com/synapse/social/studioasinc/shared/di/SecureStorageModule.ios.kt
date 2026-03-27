package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.data.local.IosSecureStorage
import com.synapse.social.studioasinc.shared.data.local.SecureStorage
import com.synapse.social.studioasinc.shared.security.IosSecurityCipher
import com.synapse.social.studioasinc.shared.security.SecurityCipher
import org.koin.dsl.module
import org.koin.core.module.Module

actual val secureStorageModule = module {
    single<SecureStorage> { IosSecureStorage() }
    single<SecurityCipher> { IosSecurityCipher() }
}
