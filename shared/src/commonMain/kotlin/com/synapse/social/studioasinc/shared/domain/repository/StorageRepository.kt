package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun getStorageConfig(): Flow<StorageConfig>
    suspend fun saveStorageConfig(config: StorageConfig)

    suspend fun updatePhotoProvider(provider: StorageProvider)
    suspend fun updateVideoProvider(provider: StorageProvider)
    suspend fun updateOtherProvider(provider: StorageProvider)

    suspend fun updateImgBBConfig(key: String)
    suspend fun updateCloudinaryConfig(cloudName: String, apiKey: String, apiSecret: String, uploadPreset: String = "")
    suspend fun updateSupabaseConfig(url: String, key: String, bucket: String)
    suspend fun updateR2Config(accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String)

    suspend fun updateCompression(enabled: Boolean)
    suspend fun ensureDefault()
}
