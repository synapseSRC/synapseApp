package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostMetadata(
    @SerialName("layout_type")
    val layoutType: String? = null,
    @SerialName("background_color")
    val backgroundColor: Long? = null,
    val feeling: FeelingActivity? = null,
    @SerialName("tagged_people")
    val taggedPeople: List<User>? = null
)

@Serializable
data class FeelingActivity(
    val emoji: String,
    val text: String,
    val type: FeelingType = FeelingType.MOOD
)

@Serializable
enum class FeelingType {
    MOOD, ACTIVITY
}
