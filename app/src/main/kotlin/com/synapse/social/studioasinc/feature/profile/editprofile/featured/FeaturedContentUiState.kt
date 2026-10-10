package com.synapse.social.studioasinc.feature.profile.editprofile.featured

import com.synapse.social.studioasinc.domain.model.Post

enum class FeaturedTab {
    POSTS,
    MEDIA,
    COLLECTIONS
}

data class FeaturedMediaItem(
    val url: String,
    val sourcePostId: String,
    val isVideo: Boolean = false,
    val caption: String? = null
)

data class FeaturedContentUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val currentUserId: String? = null,
    val selectedTab: FeaturedTab = FeaturedTab.POSTS,
    val candidatePosts: List<Post> = emptyList(),
    val candidateMedia: List<FeaturedMediaItem> = emptyList(),
    val selectedPostIds: List<String> = emptyList(),
    val selectedMediaUrls: List<String> = emptyList(),
    val hasPendingChanges: Boolean = false,
    val showBackendCapabilityNotice: Boolean = true
) {
    val selectedPosts: List<Post>
        get() = selectedPostIds.mapNotNull { id -> candidatePosts.find { it.id == id } }

    val selectedMedia: List<FeaturedMediaItem>
        get() = selectedMediaUrls.mapNotNull { url -> candidateMedia.find { it.url == url } }

    val hasSelectedContent: Boolean
        get() = selectedPostIds.isNotEmpty() || selectedMediaUrls.isNotEmpty()
}
