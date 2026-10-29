package com.synapse.social.studioasinc.domain.model

data class StoryWithUser(
    val user: User,
    val stories: List<Story>,
    val hasUnseenStories: Boolean = true,
    val latestStoryTime: String? = null
) {
    fun isOwnStory(currentUserId: String): Boolean = user.uid == currentUserId
}

data class StoryTrayState(
    val myStory: StoryWithUser? = null,
    val friendStories: List<StoryWithUser> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val allStories: List<StoryWithUser> get() = buildList {
        myStory?.let { add(it) }
        addAll(friendStories)
    }
}
