package com.synapse.social.studioasinc.shared.domain.model.activitypub

import kotlinx.serialization.Serializable

@Serializable
data class ActivityPubActor(
    val id: String,
    val type: String,
    val preferredUsername: String,
    val name: String? = null,
    val summary: String? = null,
    val iconUrl: String? = null,
    val url: String? = null,
    val inbox: String? = null,
    val outbox: String? = null
)

@Serializable
data class ActivityPubObject(
    val id: String,
    val type: String,
    val attributedTo: String,
    val content: String? = null,
    val published: String? = null,
    val url: String? = null,
    val inReplyTo: String? = null
)

@Serializable
data class ActivityPubActivity(
    val id: String,
    val type: String,
    val actor: String,
    val `object`: ActivityPubObject
)
