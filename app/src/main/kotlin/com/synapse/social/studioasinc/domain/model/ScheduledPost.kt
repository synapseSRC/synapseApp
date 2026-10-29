package com.synapse.social.studioasinc.domain.model

data class ScheduledPost(
    val id: String = "",
    val userId: String = "",
    val postRequest: CreatePostRequest = CreatePostRequest(),
    val scheduledAt: String = "",
    val status: String = STATUS_PENDING,
    val errorMessage: String? = null,
    val publishedPostId: String? = null,
    val createdAt: String? = null,
    val publishedAt: String? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_PUBLISHED = "published"
        const val STATUS_FAILED = "failed"
    }
}
