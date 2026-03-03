package com.synapse.social.studioasinc.shared.core.config

expect object SynapseConfig {
    val SUPABASE_URL: String
    val SUPABASE_ANON_KEY: String
    val SUPABASE_SYNAPSE_S3_ENDPOINT_URL: String
    val SUPABASE_SYNAPSE_S3_ENDPOINT_REGION: String
    val SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID: String
    val SUPABASE_SYNAPSE_S3_ACCESS_KEY: String
    val IMGBB_API_KEY: String
    val CLOUDINARY_CLOUD_NAME: String
    val CLOUDINARY_API_KEY: String
    val CLOUDINARY_API_SECRET: String
}
