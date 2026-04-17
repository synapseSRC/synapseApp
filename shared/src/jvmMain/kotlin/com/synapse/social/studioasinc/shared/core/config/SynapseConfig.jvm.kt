package com.synapse.social.studioasinc.shared.core.config

actual object SynapseConfig {
    actual val SUPABASE_URL: String = System.getenv("SUPABASE_URL") ?: ""
    actual val SUPABASE_ANON_KEY: String = System.getenv("SUPABASE_ANON_KEY") ?: ""
    actual val SUPABASE_SYNAPSE_S3_ENDPOINT_URL: String = System.getenv("SUPABASE_SYNAPSE_S3_ENDPOINT_URL") ?: ""
    actual val SUPABASE_SYNAPSE_S3_ENDPOINT_REGION: String = System.getenv("SUPABASE_SYNAPSE_S3_ENDPOINT_REGION") ?: ""
    actual val SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID: String = System.getenv("SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID") ?: ""
    actual val SUPABASE_SYNAPSE_S3_ACCESS_KEY: String = System.getenv("SUPABASE_SYNAPSE_S3_ACCESS_KEY") ?: ""
    actual val IMGBB_API_KEY: String = System.getenv("IMGBB_API_KEY") ?: ""
    actual val CLOUDINARY_CLOUD_NAME: String = System.getenv("CLOUDINARY_CLOUD_NAME") ?: ""
    actual val CLOUDINARY_API_KEY: String = System.getenv("CLOUDINARY_API_KEY") ?: ""
    actual val CLOUDINARY_API_SECRET: String = System.getenv("CLOUDINARY_API_SECRET") ?: ""
}
