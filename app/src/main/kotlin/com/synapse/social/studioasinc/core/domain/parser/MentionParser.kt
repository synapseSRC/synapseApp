package com.synapse.social.studioasinc.core.domain.parser



object MentionParser {
    private val MENTION_REGEX = "@(\\w+)".toRegex()



    fun extractMentions(text: String): List<String> =
        MENTION_REGEX.findAll(text)
            .map { it.groupValues[1] }
            .toList()
}
