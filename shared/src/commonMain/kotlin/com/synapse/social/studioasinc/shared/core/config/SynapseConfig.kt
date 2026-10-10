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
    val GEMINI_API_KEY: String

    val OPENAI_API_ENDPOINT: String
    val ANTHROPIC_API_ENDPOINT: String
    val OPENROUTER_API_ENDPOINT: String
    val GEMINI_API_ENDPOINT: String
    val IMGBB_API_ENDPOINT: String
    val CLOUDINARY_API_BASE_URL: String
    val APP_WEBSITE_URL: String
    val GITHUB_BUG_REPORT_URL: String
    val DEFAULT_QUIET_HOURS_START: String
    val DEFAULT_QUIET_HOURS_END: String
    val SUPABASE_REDIRECT_URL: String
    val X_BASE_URL: String
    val INSTAGRAM_BASE_URL: String
    val FACEBOOK_BASE_URL: String
    val LINKEDIN_BASE_URL: String
    val GITHUB_BASE_URL: String
    val YOUTUBE_BASE_URL: String
}