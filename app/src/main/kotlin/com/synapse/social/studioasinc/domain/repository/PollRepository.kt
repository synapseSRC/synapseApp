package com.synapse.social.studioasinc.domain.repository

interface PollRepository {
    suspend fun submitVote(postId: String, optionIndex: Int): Result<Unit>
    suspend fun revokeVote(postId: String): Result<Unit>
    suspend fun getBatchUserVotes(postIds: List<String>): Result<Map<String, Int>>
    suspend fun getBatchPollVotes(postIds: List<String>): Result<Map<String, Map<Int, Int>>>
}
