package com.synapse.social.studioasinc.shared.core.config

import com.synapse.social.studioasinc.shared.BuildConfig

actual object SynapseConfig {
    actual val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    actual val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
    actual val SUPABASE_SYNAPSE_S3_ENDPOINT_URL: String = ""
    actual val SUPABASE_SYNAPSE_S3_ENDPOINT_REGION: String = ""
    actual val SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID: String = ""
    actual val SUPABASE_SYNAPSE_S3_ACCESS_KEY: String = ""
    actual val IMGBB_API_KEY: String = ""
    actual val CLOUDINARY_CLOUD_NAME: String = BuildConfig.CLOUDINARY_CLOUD_NAME
    actual val CLOUDINARY_API_KEY: String = ""
    actual val CLOUDINARY_API_SECRET: String = ""
    actual val GEMINI_API_KEY: String = ""

    actual val OPENAI_API_ENDPOINT: String = BuildConfig.OPENAI_API_ENDPOINT
    actual val ANTHROPIC_API_ENDPOINT: String = BuildConfig.ANTHROPIC_API_ENDPOINT
    actual val OPENROUTER_API_ENDPOINT: String = BuildConfig.OPENROUTER_API_ENDPOINT
    actual val GEMINI_API_ENDPOINT: String = BuildConfig.GEMINI_API_ENDPOINT
    actual val IMGBB_API_ENDPOINT: String = BuildConfig.IMGBB_API_ENDPOINT
    actual val CLOUDINARY_API_BASE_URL: String = BuildConfig.CLOUDINARY_API_BASE_URL
    actual val APP_WEBSITE_URL: String = BuildConfig.APP_WEBSITE_URL
    actual val GITHUB_BUG_REPORT_URL: String = BuildConfig.GITHUB_BUG_REPORT_URL
    actual val DEFAULT_QUIET_HOURS_START: String = BuildConfig.DEFAULT_QUIET_HOURS_START
    actual val DEFAULT_QUIET_HOURS_END: String = BuildConfig.DEFAULT_QUIET_HOURS_END
    actual val SUPABASE_REDIRECT_URL: String = BuildConfig.SUPABASE_REDIRECT_URL
    actual val X_BASE_URL: String = BuildConfig.X_BASE_URL
    actual val INSTAGRAM_BASE_URL: String = BuildConfig.INSTAGRAM_BASE_URL
    actual val FACEBOOK_BASE_URL: String = BuildConfig.FACEBOOK_BASE_URL
    actual val LINKEDIN_BASE_URL: String = BuildConfig.LINKEDIN_BASE_URL
    actual val GITHUB_BASE_URL: String = BuildConfig.GITHUB_BASE_URL
    actual val YOUTUBE_BASE_URL: String = BuildConfig.YOUTUBE_BASE_URL
}
