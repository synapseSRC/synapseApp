package com.synapse.social.studioasinc.feature.shared.utils

object UrlUtils {
    private val urlRegex = Regex("(https?://[a-zA-Z0-9-._~:/?#\\[\\]@!$&'()*+,;=%]+)|(www\\.[a-zA-Z0-9-._~:/?#\\[\\]@!$&'()*+,;=%]+)")

    fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = urlRegex.find(text)
        return match?.value
    }

    fun extractUrls(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        return urlRegex.findAll(text).map { it.value }.distinct().toList()
    }
}
