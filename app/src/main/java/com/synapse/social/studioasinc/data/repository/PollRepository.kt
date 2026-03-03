package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.domain.model.PollOptionResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import javax.inject.Inject



class PollRepository @Inject constructor(
    private val client: SupabaseClient = com.synapse.social.studioasinc.shared.core.network.SupabaseClient.client
) {

    @Serializable
    private data class PollVote(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("option_index") val optionIndex: Int,
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    private data class PollVoteCount(
        @SerialName("post_id") val postId: String,
        @SerialName("option_index") val optionIndex: Int,
        @SerialName("vote_count") val voteCount: Long
    )

    @Serializable
    private data class PostPollData(
        val id: String,
        @SerialName("poll_options") val pollOptions: List<PollOption>,
        @SerialName("poll_end_time") val pollEndTime: String?
    )



    suspend fun getUserVote(postId: String): Result<Int?> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not authenticated"))

        val votes = client.from("poll_votes")
            .select(Columns.list("option_index")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<PollVote>()

        votes.firstOrNull()?.optionIndex
    }



    suspend fun submitVote(postId: String, optionIndex: Int): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not authenticated"))


        val post = client.from("posts")
            .select(Columns.list("id", "poll_options", "poll_end_time")) {
                filter { eq("id", postId) }
            }
            .decodeSingle<PostPollData>()

        post.pollEndTime?.let { endTime ->
            if (Instant.parse(endTime).isBefore(Instant.now())) {
                return Result.failure(Exception("Poll has ended"))
            }
        }


        if (optionIndex < 0 || optionIndex >= post.pollOptions.size) {
            return Result.failure(Exception("Invalid option index"))
        }


        val existingVote = client.from("poll_votes")
            .select(Columns.list("id")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<PollVote>()
            .firstOrNull()

        if (existingVote != null) {

            client.from("poll_votes")
                .update({
                    set("option_index", optionIndex)
                }) {
                    filter { eq("id", existingVote.id!!) }
                }
        } else {

            client.from("poll_votes")
                .insert(PollVote(
                    postId = postId,
                    userId = userId,
                    optionIndex = optionIndex
                ))
        }

        Log.d(TAG, "Vote submitted: post=$postId, option=$optionIndex")
    }



    suspend fun revokeVote(postId: String): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not authenticated"))


        client.from("poll_votes").delete {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }

        Log.d(TAG, "Vote revoked: post=$postId")
    }



    suspend fun getPollResults(postId: String): Result<List<PollOptionResult>> = runCatching {

        val post = client.from("posts")
            .select(Columns.list("poll_options")) {
                filter { eq("id", postId) }
            }
            .decodeSingle<PostPollData>()


        val votes = client.from("poll_votes")
            .select(Columns.list("option_index")) {
                filter { eq("post_id", postId) }
            }
            .decodeList<PollVote>()


        val voteCounts = votes.groupingBy { it.optionIndex }.eachCount()

        PollOptionResult.calculateResults(post.pollOptions.map { it.text }, voteCounts)
    }





    suspend fun getBatchUserVotes(postIds: List<String>): Result<Map<String, Int>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not authenticated"))

        if (postIds.isEmpty()) return Result.success(emptyMap())

        val votes = client.from("poll_votes")
            .select(Columns.list("post_id", "option_index")) {
                filter {
                    isIn("post_id", postIds)
                    eq("user_id", userId)
                }
            }
            .decodeList<PollVote>()

        votes.associate { it.postId to it.optionIndex }
    }



    suspend fun getBatchPollVotes(postIds: List<String>): Result<Map<String, Map<Int, Int>>> = runCatching {
        if (postIds.isEmpty()) return Result.success(emptyMap())

        val counts = client.postgrest.rpc(
            "get_poll_votes_count",
            mapOf("post_ids" to postIds)
        ).decodeList<PollVoteCount>()

        counts.groupBy { it.postId }
            .mapValues { (_, postCounts) ->
                // Clamping to Int.MAX_VALUE to prevent negative numbers on overflow, preserving best effort count
                postCounts.associate { it.optionIndex to it.voteCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() }
            }
    }

    companion object {
        private const val TAG = "PollRepository"
    }
}
