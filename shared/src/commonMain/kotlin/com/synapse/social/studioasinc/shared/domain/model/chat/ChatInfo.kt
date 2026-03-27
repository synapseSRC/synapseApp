package com.synapse.social.studioasinc.shared.domain.model.chat

data class ChatInfo(
    val id: String?,
    val name: String?,
    val description: String?,
    val avatarUrl: String?,
    val isGroup: Boolean,
    val createdBy: String?,
    val onlyAdminsCanMessage: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)