package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.data.local.AndroidSecureStorage
import com.synapse.social.studioasinc.shared.data.local.SecureStorage
import com.synapse.social.studioasinc.shared.security.AndroidSecurityCipher
import com.synapse.social.studioasinc.shared.security.SecurityCipher
import org.koin.dsl.module
import org.koin.core.module.Module

actual val secureStorageModule = module {
    single<SecureStorage> { AndroidSecureStorage(get()) }
    single<SecurityCipher> { AndroidSecurityCipher() }
}
