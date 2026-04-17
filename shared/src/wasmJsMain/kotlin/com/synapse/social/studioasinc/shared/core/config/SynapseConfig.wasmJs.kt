package com.synapse.social.studioasinc.shared.core.config

private fun getEnvVar(key: String): String = js("typeof process !== 'undefined' && process.env ? process.env[key] : ''")

actual object SynapseConfig {
    actual val SUPABASE_URL: String = getEnvVar("SUPABASE_URL")
    actual val SUPABASE_ANON_KEY: String = getEnvVar("SUPABASE_ANON_KEY")
    actual val SUPABASE_SYNAPSE_S3_ENDPOINT_URL: String = getEnvVar("SUPABASE_SYNAPSE_S3_ENDPOINT_URL")
    actual val SUPABASE_SYNAPSE_S3_ENDPOINT_REGION: String = getEnvVar("SUPABASE_SYNAPSE_S3_ENDPOINT_REGION")
    actual val SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID: String = getEnvVar("SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID")
    actual val SUPABASE_SYNAPSE_S3_ACCESS_KEY: String = getEnvVar("SUPABASE_SYNAPSE_S3_ACCESS_KEY")
    actual val IMGBB_API_KEY: String = getEnvVar("IMGBB_API_KEY")
    actual val CLOUDINARY_CLOUD_NAME: String = getEnvVar("CLOUDINARY_CLOUD_NAME")
    actual val CLOUDINARY_API_KEY: String = getEnvVar("CLOUDINARY_API_KEY")
    actual val CLOUDINARY_API_SECRET: String = getEnvVar("CLOUDINARY_API_SECRET")
}
