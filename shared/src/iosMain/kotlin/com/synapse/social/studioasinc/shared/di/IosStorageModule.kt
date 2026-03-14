package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.data.datasource.MeshDataSource
import com.synapse.social.studioasinc.shared.data.datasource.IosMeshDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual val meshDataSourceModule: Module = module {
    single<MeshDataSource> { IosMeshDataSource() }
}
