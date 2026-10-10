package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



@Serializable
data class Reaction(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("target_id")
    val targetId: String,
    @SerialName("target_type")
    val targetType: String = "post",
    @SerialName("reaction_type")
    val reactionType: String = "LIKE",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {


    fun getReactionTypeEnum(): ReactionType {
        return ReactionType.fromString(reactionType)
    }



    fun isLike(): Boolean {
        return reactionType.equals("LIKE", ignoreCase = true)
    }
}



fun HashMap<String, Any>.toReaction(): Reaction {
    return Reaction(
        id = this["id"] as? String,
        userId = this["user_id"] as? String ?: "",
        targetId = this["target_id"] as? String ?: "",
        targetType = this["target_type"] as? String ?: "post",
        reactionType = this["reaction_type"] as? String ?: "LIKE",
        createdAt = this["created_at"] as? String,
        updatedAt = this["updated_at"] as? String
    )
}
