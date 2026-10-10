package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient



@Serializable
data class PostDetail(
    val post: Post,
    val author: UserProfile,
    @Transient
    val reactionSummary: Map<ReactionType, Int> = emptyMap(),
    @Transient
    val userReaction: ReactionType? = null,
    val isBookmarked: Boolean = false,
    val hasReshared: Boolean = false,
    val pollResults: List<PollOptionResult>? = null,
    val userPollVote: Int? = null
) {


    fun hasMedia(): Boolean = !post.mediaItems.isNullOrEmpty()



    fun hasPoll(): Boolean = post.hasPoll == true



    fun hasLocation(): Boolean = post.hasLocation == true



    fun hasYouTubeEmbed(): Boolean = !post.youtubeUrl.isNullOrEmpty()



    fun isEncrypted(): Boolean = post.isEncrypted == true



    fun isEdited(): Boolean = post.isEdited == true



    fun getTotalReactions(): Int = reactionSummary.values.sum()



    fun isAuthorVerified(): Boolean = author.verify



    fun isAuthorPremium(): Boolean = author.account_type == "premium"
}
