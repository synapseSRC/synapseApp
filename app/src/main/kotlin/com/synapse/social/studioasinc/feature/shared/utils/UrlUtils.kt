package com.synapse.social.studioasinc.feature.shared.utils

import com.synapse.social.studioasinc.shared.core.util.LinkMetadataParser

object UrlUtils {
    private val urlRegex = Regex("(https?://[a-zA-Z0-9-._~:/?#\\[\\]@!$&'()*+,;=%]+)|(www\\.[a-zA-Z0-9-._~:/?#\\[\\]@!$&'()*+,;=%]+)")

    fun extractFirstUrl(text: String?): String? = extractUrls(text).firstOrNull()

    /**
     * All distinct URLs found in [text], with trailing prose punctuation removed so that links
     * written at the end of a sentence still resolve to a fetchable URL.
     */
    fun extractUrls(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        return urlRegex.findAll(text)
            .map { LinkMetadataParser.trimTrailingPunctuation(it.value) }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }
}
