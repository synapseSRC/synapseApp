
package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StorageConfig(
    val photoProvider: StorageProvider = StorageProvider.DEFAULT,
    val videoProvider: StorageProvider = StorageProvider.DEFAULT,
    val otherProvider: StorageProvider = StorageProvider.DEFAULT,


    val imgBBKey: String = "",


    val cloudinaryCloudName: String = "",
    val cloudinaryApiKey: String = "",
    val cloudinaryApiSecret: String = "",
    val cloudinaryUploadPreset: String = "",


    val supabaseUrl: String = "",
    val supabaseKey: String = "",
    val supabaseBucket: String = "",


    val r2AccountId: String = "",
    val r2AccessKeyId: String = "",
    val r2SecretAccessKey: String = "",
    val r2BucketName: String = "",
    val compressImages: Boolean = true
) {
    fun isProviderConfigured(provider: StorageProvider): Boolean {
        return when (provider) {
            StorageProvider.DEFAULT -> true
            StorageProvider.IMGBB -> imgBBKey.isNotBlank()
            StorageProvider.CLOUDINARY -> cloudinaryCloudName.isNotBlank() &&
                (cloudinaryUploadPreset.isNotBlank() || (cloudinaryApiKey.isNotBlank() && cloudinaryApiSecret.isNotBlank()))
            StorageProvider.SUPABASE -> supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()
            StorageProvider.CLOUDFLARE_R2 -> r2AccountId.isNotBlank() && r2AccessKeyId.isNotBlank() && r2SecretAccessKey.isNotBlank() && r2BucketName.isNotBlank()

        }
    }
}
