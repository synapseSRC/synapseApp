package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PendingAction(
    val id: String,
    val actionType: ActionType,
    val targetId: String,
    val payload: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null
) {
    @Serializable
    enum class ActionType {
        EDIT,
        DELETE,
        FORWARD,
        LIKE,
        BOOKMARK,
        POLL_VOTE,
        SEND_MESSAGE
    }
}
