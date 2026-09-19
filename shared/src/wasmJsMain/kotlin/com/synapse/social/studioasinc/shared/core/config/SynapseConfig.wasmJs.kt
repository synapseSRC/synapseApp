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
    actual val GEMINI_API_KEY: String = getEnvVar("GEMINI_API_KEY")

    actual val OPENAI_API_ENDPOINT: String = getEnvVar("OPENAI_API_ENDPOINT").ifBlank { "https://api.openai.com/v1/chat/completions" }
    actual val ANTHROPIC_API_ENDPOINT: String = getEnvVar("ANTHROPIC_API_ENDPOINT").ifBlank { "https://api.anthropic.com/v1/messages" }
    actual val OPENROUTER_API_ENDPOINT: String = getEnvVar("OPENROUTER_API_ENDPOINT").ifBlank { "https://openrouter.ai/api/v1/chat/completions" }
    actual val GEMINI_API_ENDPOINT: String = getEnvVar("GEMINI_API_ENDPOINT").ifBlank { "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent" }
    actual val IMGBB_API_ENDPOINT: String = getEnvVar("IMGBB_API_ENDPOINT").ifBlank { "https://api.imgbb.com/1/upload" }
    actual val CLOUDINARY_API_BASE_URL: String = getEnvVar("CLOUDINARY_API_BASE_URL").ifBlank { "https://api.cloudinary.com/v1_1/" }
    actual val APP_WEBSITE_URL: String = getEnvVar("APP_WEBSITE_URL").ifBlank { "https://synapsesocial.vercel.app" }
    actual val GITHUB_BUG_REPORT_URL: String = getEnvVar("GITHUB_BUG_REPORT_URL").ifBlank { "https://github.com/synapseSRC/synapseApp/issues/new?template=bug_report.md" }
    actual val DEFAULT_QUIET_HOURS_START: String = getEnvVar("DEFAULT_QUIET_HOURS_START").ifBlank { "22:00" }
    actual val DEFAULT_QUIET_HOURS_END: String = getEnvVar("DEFAULT_QUIET_HOURS_END").ifBlank { "08:00" }
    actual val SUPABASE_REDIRECT_URL: String = getEnvVar("SUPABASE_REDIRECT_URL").ifBlank { "https://synapseofficial.vercel.app/" }
    actual val X_BASE_URL: String = getEnvVar("X_BASE_URL").ifBlank { "https://x.com/" }
    actual val INSTAGRAM_BASE_URL: String = getEnvVar("INSTAGRAM_BASE_URL").ifBlank { "https://instagram.com/" }
    actual val FACEBOOK_BASE_URL: String = getEnvVar("FACEBOOK_BASE_URL").ifBlank { "https://facebook.com/" }
    actual val LINKEDIN_BASE_URL: String = getEnvVar("LINKEDIN_BASE_URL").ifBlank { "https://linkedin.com/in/" }
    actual val GITHUB_BASE_URL: String = getEnvVar("GITHUB_BASE_URL").ifBlank { "https://github.com/" }
    actual val YOUTUBE_BASE_URL: String = getEnvVar("YOUTUBE_BASE_URL").ifBlank { "https://youtube.com/@" }
}
