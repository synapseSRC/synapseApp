package com.synapse.social.studioasinc.ui.navigation
import kotlinx.serialization.Serializable
sealed interface AppDestination {
    @Serializable
    data object Auth : AppDestination
    @Serializable
    data object Home : AppDestination
    @Serializable
    data class Profile(val userId: String) : AppDestination
    @Serializable
    data object Inbox : AppDestination
    @Serializable
    data object Search : AppDestination
    @Serializable
    data class PostDetail(val postId: String, val commentId: String? = null) : AppDestination
    @Serializable
    data class CreatePost(val postId: String? = null, val type: String = "post") : AppDestination
    @Serializable
    data class QuotePost(val postId: String) : AppDestination
    @Serializable
    data object Settings : AppDestination
    @Serializable
    data object EditProfile : AppDestination
    @Serializable
    data object RegionSelection : AppDestination
    @Serializable
    data class PhotoHistory(val type: String) : AppDestination
    @Serializable
    data class FollowList(val userId: String, val initialTab: Int = 0) : AppDestination
    @Serializable
    data class Chat(
        val chatId: String, 
        val userId: String? = null,
        val participantName: String? = null
    ) : AppDestination
    @Serializable
    data class StoryViewer(val userId: String) : AppDestination
    @Serializable
    data object StoryCreator : AppDestination
    @Serializable
    data object ChatPrivacy : AppDestination
    @Serializable
    data object CreateGroup : AppDestination
    @Serializable
    data class GroupInfo(val chatId: String, val groupName: String) : AppDestination
}
