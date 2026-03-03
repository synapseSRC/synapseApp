package com.synapse.social.studioasinc.shared.domain.model.chat

data class ChatParticipant(
    val id: String,
    val chatId: String,
    val userId: String,
    val isAdmin: Boolean = false,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val lastReadAt: String? = null
)
