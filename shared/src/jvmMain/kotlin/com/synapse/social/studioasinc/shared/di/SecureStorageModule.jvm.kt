package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.data.local.SecureStorage
import com.synapse.social.studioasinc.shared.data.local.JvmSecureStorage
import com.synapse.social.studioasinc.shared.security.SecurityCipher
import com.synapse.social.studioasinc.shared.security.JvmSecurityCipher
import org.koin.dsl.module
import org.koin.core.module.Module

actual val secureStorageModule = module {
    single<SecureStorage> { JvmSecureStorage() }
    single<SecurityCipher> { JvmSecurityCipher() }
}
