package com.synapse.social.studioasinc.shared.domain.model.chat

data class TypingStatus(
    val userId: String,
    val chatId: String,
    val isTyping: Boolean
)
