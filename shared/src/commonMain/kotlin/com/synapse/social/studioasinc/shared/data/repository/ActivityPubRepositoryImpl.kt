package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.dto.activitypub.ActorDto
import com.synapse.social.studioasinc.shared.data.dto.activitypub.ObjectDto
import com.synapse.social.studioasinc.shared.domain.model.activitypub.ActivityPubActor
import com.synapse.social.studioasinc.shared.domain.model.activitypub.ActivityPubObject
import com.synapse.social.studioasinc.shared.domain.repository.IActivityPubRepository
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib

class ActivityPubRepositoryImpl(
    private val client: SupabaseClientLib = SupabaseClient.client
) : IActivityPubRepository {

    override suspend fun searchFederatedActors(query: String): Result<List<ActivityPubActor>> = runCatching {
        val response = client.functions.invoke("federation-search", mapOf("query" to query))
        // Assuming the function returns a list of ActorDto
        val actors = client.functions.serializer.decodeFromString<List<ActorDto>>(response.bodyAsText())
        actors.map { it.toDomain() }
    }

    override suspend fun getFederatedPost(postId: String): Result<ActivityPubObject> = runCatching {
        val response = client.functions.invoke("federation-get-post", mapOf("postId" to postId))
        val dto = client.functions.serializer.decodeFromString<ObjectDto>(response.bodyAsText())
        dto.toDomain()
    }

    override suspend fun followFederatedActor(actorId: String): Result<Unit> = runCatching {
        client.functions.invoke("federation-follow", mapOf("actorId" to actorId))
        Unit
    }

    override suspend fun replyToFederatedPost(postId: String, content: String): Result<Unit> = runCatching {
        client.functions.invoke("federation-reply", mapOf("postId" to postId, "content" to content))
        Unit
    }

    private fun ActorDto.toDomain(): ActivityPubActor = ActivityPubActor(
        id = id,
        type = type,
        preferredUsername = preferredUsername,
        name = name,
        summary = summary,
        iconUrl = icon?.url,
        url = url,
        inbox = inbox,
        outbox = outbox
    )

    private fun ObjectDto.toDomain(): ActivityPubObject = ActivityPubObject(
        id = id,
        type = type,
        attributedTo = attributedTo,
        content = content,
        published = published,
        url = url,
        inReplyTo = inReplyTo
    )
}
