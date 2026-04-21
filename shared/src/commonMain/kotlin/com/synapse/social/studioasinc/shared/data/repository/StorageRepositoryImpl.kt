package com.synapse.social.studioasinc.shared.data.repository
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.database.Storage_config
import com.synapse.social.studioasinc.shared.data.local.SecureStorage
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import com.synapse.social.studioasinc.shared.util.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class StorageRepositoryImpl(
    db: StorageDatabase,
    private val secureStorage: SecureStorage
) : StorageRepository {
    private val queries = db.storageConfigQueries
    init { println("StorageRepositoryImpl initialized") }

    companion object {
        private const val KEY_IMGBB = "imgbb_key"
        private const val KEY_CLOUDINARY_API_KEY = "cloudinary_api_key"
        private const val KEY_CLOUDINARY_API_SECRET = "cloudinary_api_secret"
        private const val KEY_SUPABASE = "supabase_key"
        private const val KEY_R2_ACCESS_KEY_ID = "r2_access_key_id"
        private const val KEY_R2_SECRET_ACCESS_KEY = "r2_secret_access_key"
        private const val KEY_COMPRESS_IMAGES = "compress_images"
    }

    override fun getStorageConfig(): Flow<StorageConfig> {
        return queries.getConfig().asFlow()
            .onStart { ensureDefault() }
            .mapToOneOrNull(AppDispatchers.IO)
            .map { row -> mapToConfig(row) }
    }

    internal suspend fun mapToConfig(row: Storage_config?): StorageConfig = coroutineScope {
        if (row == null) return@coroutineScope StorageConfig()

        val imgBBKeyDeferred = async(AppDispatchers.IO) {
            secureStorage.getString(KEY_IMGBB)?.takeIf { it.isNotBlank() }
                ?: row.imgbb_key.takeIf { it.isNotBlank() }
                ?: SynapseConfig.IMGBB_API_KEY.takeIf { it.isNotBlank() }
                ?: ""
        }

        val cloudinaryApiKeyDeferred = async(AppDispatchers.IO) {
            secureStorage.getString(KEY_CLOUDINARY_API_KEY)?.takeIf { it.isNotBlank() }
                ?: row.cloudinary_api_key.takeIf { it.isNotBlank() }
                ?: ""
        }
        val cloudinaryApiSecretDeferred = async(AppDispatchers.IO) {
            secureStorage.getString(KEY_CLOUDINARY_API_SECRET)?.takeIf { it.isNotBlank() }
                ?: row.cloudinary_api_secret.takeIf { it.isNotBlank() }
                ?: ""
        }

        val supabaseKeyDeferred = async(AppDispatchers.IO) {
            secureStorage.getString(KEY_SUPABASE)?.takeIf { it.isNotBlank() }
                ?: row.supabase_key.takeIf { it.isNotBlank() }
                ?: ""
        }

        val r2AccessKeyIdDeferred = async(AppDispatchers.IO) {
            secureStorage.getString(KEY_R2_ACCESS_KEY_ID)?.takeIf { it.isNotBlank() }
                ?: row.r2_access_key_id.takeIf { it.isNotBlank() }
                ?: ""
        }
        val r2SecretAccessKeyDeferred = async(AppDispatchers.IO) {
            secureStorage.getString(KEY_R2_SECRET_ACCESS_KEY)?.takeIf { it.isNotBlank() }
                ?: row.r2_secret_access_key.takeIf { it.isNotBlank() }
                ?: ""
        }
        val compressImagesDeferred = async(AppDispatchers.IO) { secureStorage.getString(KEY_COMPRESS_IMAGES)?.toBoolean() ?: true }

        val cloudinaryCloudName = row.cloudinary_cloud_name
        val cloudinaryUploadPreset = row.cloudinary_upload_preset

        val supabaseUrl = row.supabase_url

        val r2AccountId = row.r2_account_id
        val r2BucketName = row.r2_bucket_name

        StorageConfig(
            photoProvider = row.photo_provider.toStorageProvider(),
            videoProvider = row.video_provider.toStorageProvider(),
            otherProvider = row.other_provider.toStorageProvider(),
            imgBBKey = imgBBKeyDeferred.await(),
            cloudinaryCloudName = cloudinaryCloudName,
            cloudinaryApiKey = cloudinaryApiKeyDeferred.await(),
            cloudinaryApiSecret = cloudinaryApiSecretDeferred.await(),
            cloudinaryUploadPreset = cloudinaryUploadPreset,
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKeyDeferred.await(),
            supabaseBucket = row.supabase_bucket,
            r2AccountId = r2AccountId,
            r2AccessKeyId = r2AccessKeyIdDeferred.await(),
            r2SecretAccessKey = r2SecretAccessKeyDeferred.await(),
            r2BucketName = r2BucketName,
            compressImages = compressImagesDeferred.await()
        )
    }

    override suspend fun saveStorageConfig(config: StorageConfig) = withContext(AppDispatchers.IO) {

        secureStorage.save(KEY_IMGBB, config.imgBBKey)
        secureStorage.save(KEY_CLOUDINARY_API_KEY, config.cloudinaryApiKey)
        secureStorage.save(KEY_CLOUDINARY_API_SECRET, config.cloudinaryApiSecret)
        secureStorage.save(KEY_SUPABASE, config.supabaseKey)
        secureStorage.save(KEY_R2_ACCESS_KEY_ID, config.r2AccessKeyId)
        secureStorage.save(KEY_R2_SECRET_ACCESS_KEY, config.r2SecretAccessKey)

        queries.transaction {
            val now = TimeProvider.nowMillis()
            queries.updatePhotoProvider(config.photoProvider.name, now)
            queries.updateVideoProvider(config.videoProvider.name, now)
            queries.updateOtherProvider(config.otherProvider.name, now)

            queries.updateImgBB("", now)
            queries.updateCloudinary(config.cloudinaryCloudName, "", "", config.cloudinaryUploadPreset, now)
            queries.updateSupabase(config.supabaseUrl, "", config.supabaseBucket, now)
            queries.updateR2(config.r2AccountId, "", "", config.r2BucketName, now)
        }
    }

    override suspend fun updatePhotoProvider(provider: StorageProvider): Unit = withContext(AppDispatchers.IO) {
        queries.updatePhotoProvider(provider.name, TimeProvider.nowMillis())
    }

    override suspend fun updateVideoProvider(provider: StorageProvider): Unit = withContext(AppDispatchers.IO) {
        queries.updateVideoProvider(provider.name, TimeProvider.nowMillis())
    }

    override suspend fun updateOtherProvider(provider: StorageProvider): Unit = withContext(AppDispatchers.IO) {
        queries.updateOtherProvider(provider.name, TimeProvider.nowMillis())
    }

    override suspend fun updateImgBBConfig(key: String): Unit = withContext(AppDispatchers.IO) {
        if (key.isBlank()) secureStorage.clear(KEY_IMGBB) else secureStorage.save(KEY_IMGBB, key)
        queries.updateImgBB("", TimeProvider.nowMillis())
    }

    override suspend fun updateCloudinaryConfig(cloudName: String, apiKey: String, apiSecret: String, uploadPreset: String): Unit = withContext(AppDispatchers.IO) {
        if (apiKey.isBlank()) secureStorage.clear(KEY_CLOUDINARY_API_KEY) else secureStorage.save(KEY_CLOUDINARY_API_KEY, apiKey)
        if (apiSecret.isBlank()) secureStorage.clear(KEY_CLOUDINARY_API_SECRET) else secureStorage.save(KEY_CLOUDINARY_API_SECRET, apiSecret)
        queries.updateCloudinary(cloudName, "", "", uploadPreset, TimeProvider.nowMillis())
    }

    override suspend fun updateSupabaseConfig(url: String, key: String, bucket: String): Unit = withContext(AppDispatchers.IO) {
        if (key.isBlank()) secureStorage.clear(KEY_SUPABASE) else secureStorage.save(KEY_SUPABASE, key)
        queries.updateSupabase(url, "", bucket, TimeProvider.nowMillis())
    }

    override suspend fun updateR2Config(
        accountId: String,
        accessKeyId: String,
        secretAccessKey: String,
        bucketName: String
    ): Unit = withContext(AppDispatchers.IO) {
        if (accessKeyId.isBlank()) secureStorage.clear(KEY_R2_ACCESS_KEY_ID) else secureStorage.save(KEY_R2_ACCESS_KEY_ID, accessKeyId)
        if (secretAccessKey.isBlank()) secureStorage.clear(KEY_R2_SECRET_ACCESS_KEY) else secureStorage.save(KEY_R2_SECRET_ACCESS_KEY, secretAccessKey)
        queries.updateR2(accountId, "", "", bucketName, TimeProvider.nowMillis())
    }


    override suspend fun updateCompression(enabled: Boolean): Unit = withContext(AppDispatchers.IO) {
        secureStorage.save(KEY_COMPRESS_IMAGES, enabled.toString())
        // Trigger flow update by updating last_updated in DB (dummy update to any field or dedicated field if available, but here we reuse updatePhotoProvider or similar if we want to trigger,
        // OR rely on the fact that flow emits when mapToConfig is called? No, mapToConfig is called when DB changes.
        // SecureStorage change doesn't trigger DB flow.
        // We need to trigger DB update to refresh config.
        // Let's just update photo provider with same value or something benign.
        // Actually, let's just update last_updated timestamp on the config row if possible.
        // The existing queries update specific fields.
        // I will use updateImgBB with existing key to trigger update.
        val currentKey = queries.getConfig().executeAsOneOrNull()?.imgbb_key ?: ""
        queries.updateImgBB(currentKey, TimeProvider.nowMillis())
    }

    override suspend fun clearProviderConfig(provider: StorageProvider): Unit = withContext(AppDispatchers.IO) {
        val now = TimeProvider.nowMillis()
        when (provider) {
            StorageProvider.IMGBB -> {
                secureStorage.clear(KEY_IMGBB)
                queries.updateImgBB("", now)
            }
            StorageProvider.CLOUDINARY -> {
                secureStorage.clear(KEY_CLOUDINARY_API_KEY)
                secureStorage.clear(KEY_CLOUDINARY_API_SECRET)
                queries.updateCloudinary("", "", "", "", now)
            }
            StorageProvider.SUPABASE -> {
                secureStorage.clear(KEY_SUPABASE)
                queries.updateSupabase("", "", "", now)
            }
            StorageProvider.CLOUDFLARE_R2 -> {
                secureStorage.clear(KEY_R2_ACCESS_KEY_ID)
                secureStorage.clear(KEY_R2_SECRET_ACCESS_KEY)
                queries.updateR2("", "", "", "", now)
            }
            StorageProvider.DEFAULT -> Unit
        }
    }

    override suspend fun ensureDefault() = withContext(AppDispatchers.IO) {
        queries.insertDefault()
        migrateSecrets()
    }

    private fun migrateSecrets() {
        val row = queries.getConfig().executeAsOneOrNull() ?: return
        val now = TimeProvider.nowMillis()

        if (row.imgbb_key.isNotBlank()) {
            secureStorage.save(KEY_IMGBB, row.imgbb_key)
            queries.updateImgBB("", now)
        }

        if (row.cloudinary_api_key.isNotBlank() || row.cloudinary_api_secret.isNotBlank()) {
            if (row.cloudinary_api_key.isNotBlank()) secureStorage.save(KEY_CLOUDINARY_API_KEY, row.cloudinary_api_key)
            if (row.cloudinary_api_secret.isNotBlank()) secureStorage.save(KEY_CLOUDINARY_API_SECRET, row.cloudinary_api_secret)
            queries.updateCloudinary(row.cloudinary_cloud_name, "", "", row.cloudinary_upload_preset, now)
        }

        if (row.supabase_key.isNotBlank()) {
             secureStorage.save(KEY_SUPABASE, row.supabase_key)
             queries.updateSupabase(row.supabase_url, "", row.supabase_bucket, now)
        }

        if (row.r2_access_key_id.isNotBlank() || row.r2_secret_access_key.isNotBlank()) {
            if (row.r2_access_key_id.isNotBlank()) secureStorage.save(KEY_R2_ACCESS_KEY_ID, row.r2_access_key_id)
            if (row.r2_secret_access_key.isNotBlank()) secureStorage.save(KEY_R2_SECRET_ACCESS_KEY, row.r2_secret_access_key)
            queries.updateR2(row.r2_account_id, "", "", row.r2_bucket_name, now)
        }
    }

    private fun String.toStorageProvider(): StorageProvider {
        return try {
            StorageProvider.valueOf(this)
        } catch (e: Exception) {
            StorageProvider.DEFAULT
        }
    }
}
