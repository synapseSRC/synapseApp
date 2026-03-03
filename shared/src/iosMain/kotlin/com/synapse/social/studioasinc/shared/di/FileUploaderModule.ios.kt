package com.synapse.social.studioasinc.shared.di

import org.koin.dsl.module
import com.synapse.social.studioasinc.shared.data.FileUploader
import org.koin.core.module.Module
import com.synapse.social.studioasinc.shared.core.media.IosMediaCompressor
import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import org.koin.dsl.bind

actual val fileUploaderModule: Module = module {
    single { FileUploader() }
    single { IosMediaCompressor() } bind MediaCompressor::class
}
