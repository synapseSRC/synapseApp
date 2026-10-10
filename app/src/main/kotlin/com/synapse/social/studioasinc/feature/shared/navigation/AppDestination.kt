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
    data class CreatePost(val postId: String? = null, val replyToPostId: String? = null, val type: String = "post") : AppDestination
    @Serializable
    data class QuotePost(val postId: String) : AppDestination
    @Serializable
    data object Settings : AppDestination
    @Serializable
    data object EditProfile : AppDestination
    @Serializable
    data object ProfessionalProfile : AppDestination
    @Serializable
    data object BusinessPlatform : AppDestination
    @Serializable
    data object ScheduledPosts : AppDestination
    @Serializable
    data object ProfileMedia : AppDestination
    @Serializable
    data object PersonalInformation : AppDestination
    @Serializable
    data object Location : AppDestination
    @Serializable
    data object RegionSelection : AppDestination
    @Serializable
    data class PhotoHistory(val type: String) : AppDestination
    @Serializable
    data object InterestsSkills : AppDestination
    @Serializable
    data object SocialLinks : AppDestination
    @Serializable
    data object ProfileAppearance : AppDestination
    @Serializable
    data object FeaturedContent : AppDestination
    @Serializable
    data object WorkEducation : AppDestination
    @Serializable
    data class FollowList(val userId: String, val initialTab: Int = 0) : AppDestination
    @Serializable
    data class Chat(
        val chatId: String, 
        val userId: String? = null,
        val participantName: String? = null,
        val participantAvatar: String? = null
    ) : AppDestination
    @Serializable
    data class StoryViewer(val userId: String, val userIds: List<String> = emptyList()) : AppDestination
    @Serializable
    data object StoryCreator : AppDestination
    @Serializable
    data class ChatPrivacy(val chatId: String) : AppDestination
    @Serializable
    data class DisappearingMessages(val chatId: String) : AppDestination
    @Serializable
    data class SharedContent(val chatId: String, val initialTab: Int = 0) : AppDestination
    @Serializable
    data object LockProfile : AppDestination
    @Serializable
    data object CreateGroup : AppDestination
    @Serializable
    data class GroupInfo(val chatId: String, val groupName: String) : AppDestination
    @Serializable
    data class ChatInfo(val chatId: String, val userId: String) : AppDestination
    @Serializable
    data class HashtagFeed(val tag: String) : AppDestination
}
