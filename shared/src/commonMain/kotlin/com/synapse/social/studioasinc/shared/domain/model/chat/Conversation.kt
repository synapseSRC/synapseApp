package com.synapse.social.studioasinc.shared.domain.model.chat

data class Conversation(
    val chatId: String,
    val participantId: String,
    val participantName: String,
    val participantAvatar: String?,
    val lastMessage: String,
    val lastMessageTime: String?,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)
