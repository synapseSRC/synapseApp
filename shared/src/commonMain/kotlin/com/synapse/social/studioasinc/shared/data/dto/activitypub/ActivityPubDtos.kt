package com.synapse.social.studioasinc.shared.data.dto.activitypub

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActorDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("preferredUsername") val preferredUsername: String,
    @SerialName("name") val name: String? = null,
    @SerialName("summary") val summary: String? = null,
    @SerialName("icon") val icon: IconDto? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("inbox") val inbox: String? = null,
    @SerialName("outbox") val outbox: String? = null
)

@Serializable
data class IconDto(
    @SerialName("type") val type: String,
    @SerialName("url") val url: String
)

@Serializable
data class ObjectDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("attributedTo") val attributedTo: String,
    @SerialName("content") val content: String? = null,
    @SerialName("published") val published: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("inReplyTo") val inReplyTo: String? = null
)

@Serializable
data class ActivityDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("actor") val actor: String,
    @SerialName("object") val `object`: ObjectDto
)
