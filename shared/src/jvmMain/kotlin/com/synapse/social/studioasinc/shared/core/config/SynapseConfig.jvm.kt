package com.synapse.social.studioasinc.shared.core.config

import java.io.File
import java.util.Properties

private val fileProperties by lazy {
    Properties().apply {
        val searchPaths = listOf(
            File("gradle.properties"),
            File("../gradle.properties"),
            File("../../gradle.properties"),
            File("local.properties"),
            File("../local.properties")
        )
        for (file in searchPaths) {
            if (file.exists()) {
                try {
                    file.inputStream().use { load(it) }
                } catch (_: Exception) {
                }
            }
        }
    }
}

private fun getVal(key: String, fallback: String = ""): String {
    val envVal = System.getenv(key)
    if (!envVal.isNullAsStringBlank()) return envVal

    val propVal = System.getProperty(key)
    if (!propVal.isNullAsStringBlank()) return propVal

    val fileVal = fileProperties.getProperty(key)
    if (!fileVal.isNullAsStringBlank()) return fileVal

    return fallback
}

private fun String?.isNullAsStringBlank(): Boolean {
    return this == null || this.isBlank() || this == "https://your-project.supabase.co" || this == "your-anon-key-here"
}

actual object SynapseConfig {
    actual val SUPABASE_URL: String = getVal(
        "SUPABASE_URL",
        "https://apqvyyphlrtmuyjnzmuq.supabase.co"
    )
    actual val SUPABASE_ANON_KEY: String = getVal(
        "SUPABASE_ANON_KEY",
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFwcXZ5eXBobHJ0bXV5am56bXVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTg3MDUwODcsImV4cCI6MjA3NDI4MTA4N30.On7kjijj7bUg_xzr2HwCTYvLaV-f_1aDYqVTfKai7gc"
    )
    actual val SUPABASE_SYNAPSE_S3_ENDPOINT_URL: String = getVal("SUPABASE_SYNAPSE_S3_ENDPOINT_URL")
    actual val SUPABASE_SYNAPSE_S3_ENDPOINT_REGION: String = getVal("SUPABASE_SYNAPSE_S3_ENDPOINT_REGION")
    actual val SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID: String = getVal("SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID")
    actual val SUPABASE_SYNAPSE_S3_ACCESS_KEY: String = getVal("SUPABASE_SYNAPSE_S3_ACCESS_KEY")
    actual val IMGBB_API_KEY: String = getVal("IMGBB_API_KEY")
    actual val CLOUDINARY_CLOUD_NAME: String = getVal("CLOUDINARY_CLOUD_NAME")
    actual val CLOUDINARY_API_KEY: String = getVal("CLOUDINARY_API_KEY")
    actual val CLOUDINARY_API_SECRET: String = getVal("CLOUDINARY_API_SECRET")
    actual val GEMINI_API_KEY: String = getVal("GEMINI_API_KEY")

    actual val OPENAI_API_ENDPOINT: String = getVal("OPENAI_API_ENDPOINT", "https://api.openai.com/v1/chat/completions")
    actual val ANTHROPIC_API_ENDPOINT: String = getVal("ANTHROPIC_API_ENDPOINT", "https://api.anthropic.com/v1/messages")
    actual val OPENROUTER_API_ENDPOINT: String = getVal("OPENROUTER_API_ENDPOINT", "https://openrouter.ai/api/v1/chat/completions")
    actual val GEMINI_API_ENDPOINT: String = getVal("GEMINI_API_ENDPOINT", "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent")
    actual val IMGBB_API_ENDPOINT: String = getVal("IMGBB_API_ENDPOINT", "https://api.imgbb.com/1/upload")
    actual val CLOUDINARY_API_BASE_URL: String = getVal("CLOUDINARY_API_BASE_URL", "https://api.cloudinary.com/v1_1/")
    actual val APP_WEBSITE_URL: String = getVal("APP_WEBSITE_URL", "https://synapsesocial.vercel.app")
    actual val GITHUB_BUG_REPORT_URL: String = getVal("GITHUB_BUG_REPORT_URL", "https://github.com/synapseSRC/synapseApp/issues/new?template=bug_report.md")
    actual val DEFAULT_QUIET_HOURS_START: String = getVal("DEFAULT_QUIET_HOURS_START", "22:00")
    actual val DEFAULT_QUIET_HOURS_END: String = getVal("DEFAULT_QUIET_HOURS_END", "08:00")
    actual val SUPABASE_REDIRECT_URL: String = getVal("SUPABASE_REDIRECT_URL", "https://synapseofficial.vercel.app/")
    actual val X_BASE_URL: String = getVal("X_BASE_URL", "https://x.com/")
    actual val INSTAGRAM_BASE_URL: String = getVal("INSTAGRAM_BASE_URL", "https://instagram.com/")
    actual val FACEBOOK_BASE_URL: String = getVal("FACEBOOK_BASE_URL", "https://facebook.com/")
    actual val LINKEDIN_BASE_URL: String = getVal("LINKEDIN_BASE_URL", "https://linkedin.com/in/")
    actual val GITHUB_BASE_URL: String = getVal("GITHUB_BASE_URL", "https://github.com/")
    actual val YOUTUBE_BASE_URL: String = getVal("YOUTUBE_BASE_URL", "https://youtube.com/@")
}
