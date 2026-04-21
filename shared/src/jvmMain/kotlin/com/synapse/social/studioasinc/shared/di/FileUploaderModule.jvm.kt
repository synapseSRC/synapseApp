package com.synapse.social.studioasinc.shared.di

import org.koin.dsl.module
import com.synapse.social.studioasinc.shared.domain.repository.FileUploader
import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import com.synapse.social.studioasinc.shared.core.media.JvmMediaCompressor
import org.koin.core.module.Module
import org.koin.dsl.bind

actual val fileUploaderModule: Module = module {
    single { FileUploader() }
    single { JvmMediaCompressor() } bind MediaCompressor::class
}
