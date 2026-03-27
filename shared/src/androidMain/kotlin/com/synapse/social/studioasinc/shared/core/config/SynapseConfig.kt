package com.synapse.social.studioasinc.shared.core.config

import com.synapse.social.studioasinc.shared.BuildConfig

actual object SynapseConfig {
    actual val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    actual val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
    actual val SUPABASE_SYNAPSE_S3_ENDPOINT_URL: String = BuildConfig.SUPABASE_SYNAPSE_S3_ENDPOINT_URL
    actual val SUPABASE_SYNAPSE_S3_ENDPOINT_REGION: String = BuildConfig.SUPABASE_SYNAPSE_S3_ENDPOINT_REGION
    actual val SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID: String = BuildConfig.SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID
    actual val SUPABASE_SYNAPSE_S3_ACCESS_KEY: String = BuildConfig.SUPABASE_SYNAPSE_S3_ACCESS_KEY
    actual val IMGBB_API_KEY: String = BuildConfig.IMGBB_API_KEY
    actual val CLOUDINARY_CLOUD_NAME: String = BuildConfig.CLOUDINARY_CLOUD_NAME
    actual val CLOUDINARY_API_KEY: String = BuildConfig.CLOUDINARY_API_KEY
    actual val CLOUDINARY_API_SECRET: String = BuildConfig.CLOUDINARY_API_SECRET
}
