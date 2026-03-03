package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.model.Reel
import com.synapse.social.studioasinc.shared.data.model.ReelCommentDto
import com.synapse.social.studioasinc.shared.data.model.ReelDto
import com.synapse.social.studioasinc.shared.domain.model.ReelComment
import com.synapse.social.studioasinc.shared.domain.model.ReelInteraction
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.UploadData
import io.github.aakira.napier.Napier
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*



class ReelRepository {
    private val client = SupabaseClient.client
    private val TAG = "ReelRepository"



    suspend fun getReels(page: Int = 0, pageSize: Int = 10): Result<List<Reel>> {
        return try {
            val currentUserId = client.auth.currentUserOrNull()?.id

            val reelsDto = client.from("reels")
                .select(columns = Columns.raw("*, users:creator_id(username, avatar)")) {
                    order(column = "created_at", order = Order.DESCENDING)
                    range(page * pageSize.toLong(), (page + 1) * pageSize.toLong() - 1)
                }.decodeList<ReelDto>()

            val reelIds = reelsDto.map { it.id }

            val userInteractions = if (currentUserId != null && reelIds.isNotEmpty()) {
                client.from("reel_interactions")
                    .select {
                        filter {
                            eq("user_id", currentUserId)
                            isIn("reel_id", reelIds)
                        }
                    }.decodeList<ReelInteraction>()
            } else {
                emptyList()
            }

            val reels = reelsDto.map { dto ->
                val interactions = userInteractions.filter { it.reelId == dto.id }
                dto.toDomain(
                    isLiked = interactions.any { it.interactionType == "like" },
                    isOpposed = interactions.any { it.interactionType == "oppose" }
                )
            }
            Result.success(reels)
        } catch (e: Exception) {
            Napier.e("Failed to fetch reels", e, tag = TAG)
            Result.failure(e)
        }
    }



    suspend fun reportReel(reelId: String, reason: String): Result<Unit> {
        return try {
            val currentUser = client.auth.currentUserOrNull() ?: throw Exception("Not logged in")
            val reportData = mapOf(
                "reel_id" to reelId,
                "reporter_id" to currentUser.id,
                "reason" to reason
            )
            client.from("reel_reports").insert(reportData)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to report reel $reelId", e, tag = TAG)
            Result.failure(e)
        }
    }



    suspend fun blockCreator(creatorId: String): Result<Unit> {
        return try {
            val currentUser = client.auth.currentUserOrNull() ?: throw Exception("Not logged in")
            val blockData = mapOf(
                "blocker_id" to currentUser.id,
                "blocked_id" to creatorId
            )
            client.from("user_blocks").insert(blockData)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to block creator $creatorId", e, tag = TAG)
            Result.failure(e)
        }
    }



    suspend fun getReel(reelId: String): Result<Reel> {
        return try {
            val currentUserId = client.auth.currentUserOrNull()?.id

            val dto = client.from("reels")
                .select(columns = Columns.raw("*, users:creator_id(username, avatar)")) {
                    filter { eq("id", reelId) }
                }.decodeSingle<ReelDto>()

            val interactions = if (currentUserId != null) {
                client.from("reel_interactions")
                    .select {
                        filter {
                            eq("user_id", currentUserId)
                            eq("reel_id", reelId)
                        }
                    }.decodeList<ReelInteraction>()
            } else {
                emptyList()
            }

            Result.success(dto.toDomain(
                isLiked = interactions.any { it.interactionType == "like" },
                isOpposed = interactions.any { it.interactionType == "oppose" }
            ))
        } catch (e: Exception) {
            Napier.e("Failed to fetch reel $reelId", e, tag = TAG)
            Result.failure(e)
        }
    }



    suspend fun likeReel(reelId: String): Result<Unit> {
        return performInteraction(reelId, "like")
    }



    suspend fun opposeReel(reelId: String, isAnonymous: Boolean = false): Result<Unit> {
        return try {
            performInteraction(reelId, "oppose", isAnonymous)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun uploadReel(
        dataChannel: ByteReadChannel,
        size: Long,
        fileName: String,
        caption: String,
        musicTrack: String,
        locationName: String? = null,
        locationAddress: String? = null,
        locationLatitude: Double? = null,
        locationLongitude: Double? = null,
        metadata: Map<String, Any?>? = null,
        onProgress: (Float) -> Unit
    ): Result<Unit> {
        return try {
            val currentUser = client.auth.currentUserOrNull() ?: throw Exception("Not logged in")
            val storagePath = "${currentUser.id}/$fileName"

            val uploadData = UploadData(dataChannel, size)


            val bucket = client.storage.from("reels")
            bucket.uploadAsFlow(storagePath, uploadData) {
                upsert = true
            }.onEach { status ->
                when (status) {
                    is UploadStatus.Progress -> {
                        val progress = status.totalBytesSend.toFloat() / status.contentLength.toFloat()
                        onProgress(progress)
                    }
                    is UploadStatus.Success -> {
                        onProgress(1.0f)
                    }
                }
            }.collect()

            val videoUrl = bucket.publicUrl(storagePath)
            val reelData = mutableMapOf<String, Any?>(
                "creator_id" to currentUser.id,
                "video_url" to videoUrl,
                "caption" to caption,
                "music_track" to musicTrack,
                "likes_count" to 0,
                "comment_count" to 0,
                "share_count" to 0,
                "oppose_count" to 0
            )

            locationName?.let { reelData["location_name"] = it }
            locationAddress?.let { reelData["location_address"] = it }
            locationLatitude?.let { reelData["location_latitude"] = it }
            locationLongitude?.let { reelData["location_longitude"] = it }
            metadata?.let { reelData["metadata"] = it }

            client.from("reels").insert(reelData)

            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to upload reel", e, tag = TAG)
            Result.failure(e)
        }
    }



    suspend fun getComments(reelId: String): Result<List<ReelComment>> {
        return try {
            val comments = client.from("reel_comments")
                .select(columns = Columns.raw("*, users(username, avatar)")) {
                    filter { eq("reel_id", reelId) }
                    order(column = "created_at", order = Order.DESCENDING)
                }.decodeList<ReelCommentDto>()

            Result.success(comments.map { it.toDomain() })
        } catch (e: Exception) {
            Napier.e("Failed to fetch comments for reel $reelId", e, tag = TAG)
            Result.failure(e)
        }
    }



    suspend fun addComment(reelId: String, content: String): Result<Unit> {
        return try {
            val currentUser = client.auth.currentUserOrNull() ?: throw Exception("Not logged in")
            val commentData = mapOf(
                "reel_id" to reelId,
                "user_id" to currentUser.id,
                "content" to content
            )
            client.from("reel_comments").insert(commentData)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to add comment to reel $reelId", e, tag = TAG)
            Result.failure(e)
        }
    }

    private suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelay: Long = 1000L,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxRetries - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Napier.w("Retry attempt ${attempt + 1} failed: ${e.message}", tag = TAG)
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return block()
    }

    private suspend fun performInteraction(reelId: String, type: String, anonymous: Boolean = false): Result<Unit> {
        return try {
            val currentUser = client.auth.currentUserOrNull() ?: throw Exception("Not logged in")

            val existing = client.from("reel_interactions").select {
                filter {
                    eq("user_id", currentUser.id)
                    eq("reel_id", reelId)
                    eq("interaction_type", type)
                }
            }.decodeList<ReelInteraction>()

            if (existing.isNotEmpty()) {

                client.from("reel_interactions").delete {
                    filter {
                         eq("user_id", currentUser.id)
                         eq("reel_id", reelId)
                         eq("interaction_type", type)
                    }
                }
            } else {

                val insertData = mapOf(
                    "user_id" to currentUser.id,
                    "reel_id" to reelId,
                    "interaction_type" to type,
                    "anonymous_oppose" to anonymous
                )
                client.from("reel_interactions").insert(insertData)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Interaction $type failed for reel $reelId", e, tag = TAG)
            Result.failure(e)
        }
    }

    private fun ReelDto.toDomain(isLiked: Boolean = false, isOpposed: Boolean = false): Reel {
        return Reel(
            id = id,
            creatorId = creatorId,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            caption = caption,
            musicTrack = musicTrack,
            likesCount = likesCount,
            commentCount = commentCount,
            shareCount = shareCount,
            opposeCount = opposeCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            creatorUsername = users?.username,
            creatorAvatarUrl = users?.avatar,
            isLikedByCurrentUser = isLiked,
            isOpposedByCurrentUser = isOpposed,
            locationName = locationName,
            locationAddress = locationAddress,
            locationLatitude = locationLatitude,
            locationLongitude = locationLongitude,
            metadata = metadata?.toString()
        )
    }

    private fun ReelCommentDto.toDomain(): ReelComment {
        return ReelComment(
            id = id,
            reelId = reelId,
            userId = userId,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
            userUsername = users?.username,
            userAvatarUrl = users?.avatar
        )
    }
}
