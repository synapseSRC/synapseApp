package com.synapse.social.studioasinc.ui.navigation
import kotlinx.serialization.Serializable
sealed interface AppDestination {
    @Serializable
    data object Auth : AppDestination
    @Serializable
    data object Home : AppDestination
    @Serializable
    data object PermissionOnboarding : AppDestination
    @Serializable
    data object PermissionManager : AppDestination
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
        val participantAvatar: String? = null,
        val messageId: String? = null
    ) : AppDestination
    @Serializable
    data class Call(
        val peerUserId: String,
        val peerName: String,
        val peerAvatarUrl: String? = null,
        val chatId: String? = null,
        val isVideo: Boolean = false,
        val isIncoming: Boolean = false,
        /**
         * Answer the ring as soon as it is on screen, instead of waiting for a tap.
         *
         * Set only when the user arrived here by pressing "Accept" in the incoming-call notification.
         * The permission gate still applies, so this can open a dialog but never a microphone.
         */
        val autoAccept: Boolean = false
    ) : AppDestination
    @Serializable
    data class StoryViewer(val userId: String, val userIds: List<String> = emptyList()) : AppDestination
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
    data class ChatSettings(val chatId: String) : AppDestination
    @Serializable
    data class ChatSearch(val chatId: String) : AppDestination
    @Serializable
    data class HashtagFeed(val tag: String) : AppDestination
    @Serializable
    data object EchoLobby : AppDestination
    @Serializable
    data class EchoRoom(
        val roomId: String,
        /**
         * Ask for the microphone on entry.
         *
         * Set by the create flow, where the caller is the host and therefore publishes. A lobby
         * join or a shared link leaves it false: an audience member must never see a microphone
         * prompt, and a promoted speaker is offered the stage after the role arrives.
         */
        val speakOnEntry: Boolean = false
    ) : AppDestination
}
