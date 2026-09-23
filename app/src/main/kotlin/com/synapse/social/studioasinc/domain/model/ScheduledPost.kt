package com.synapse.social.studioasinc.domain.model

data class ScheduledPost(
    val id: String = "",
    val userId: String = "",
    val postRequest: CreatePostRequest = CreatePostRequest(),
    val scheduledAt: String = "",
    val status: String = STATUS_SCHEDULED,
    val errorMessage: String? = null,
    val publishedPostId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    companion object {
        const val STATUS_SCHEDULED = "scheduled"
        const val STATUS_PUBLISHING = "publishing"
        const val STATUS_PUBLISHED = "published"
        const val STATUS_FAILED = "failed"
    }
}
