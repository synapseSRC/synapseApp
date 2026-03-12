package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserStatus {
    @SerialName("online") ONLINE,
    @SerialName("offline") OFFLINE;
}

@Serializable
data class User(
    val id: String? = null,
    val uid: String,
    val email: String? = null,
    val username: String? = null,
    val nickname: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val name: String? = null, // From UserProfile
    val bio: String? = null,
    val avatar: String? = null,
    @SerialName("avatar_history_type")
    val avatarHistoryType: String = "local",
    @SerialName("profile_cover_image")
    val profileCoverImage: String? = null,
    @SerialName("cover_image_url") // From UserProfile
    val coverImageUrl: String? = null,
    @SerialName("account_premium")
    val accountPremium: Boolean = false,
    @SerialName("user_level_xp")
    val userLevelXp: Int = 500,
    val verify: Boolean = false,
    @SerialName("is_verified") // From UserProfile (alias?)
    val isVerified: Boolean = false,
    @SerialName("is_private") // From UserProfile
    val isPrivate: Boolean = false,
    @SerialName("account_type")
    val accountType: String = "user",
    val gender: String = "hidden",
    val banned: Boolean = false,
    @SerialName("status")
    val status: UserStatus = UserStatus.OFFLINE,
    @SerialName("join_date")
    val joinDate: String? = null,
    @SerialName("joined_date") // From UserProfile
    val joinedDate: Long = 0,
    @SerialName("one_signal_player_id")
    val oneSignalPlayerId: String? = null,
    @SerialName("last_seen")
    val lastSeen: String? = null,
    @SerialName("chatting_with")
    val chattingWith: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("followers_count")
    val followersCount: Int = 0,
    @SerialName("following_count")
    val followingCount: Int = 0,
    @SerialName("posts_count")
    val postsCount: Int = 0,
    @SerialName("post_count") // From UserProfile (alias?)
    val postCount: Int = 0,
    @SerialName("follower_count") // From UserProfile (alias?)
    val followerCount: Int = 0,

    // Additional fields from UserProfile
    val location: String? = null,
    @SerialName("relationship_status")
    val relationshipStatus: String? = null,
    val birthday: String? = null,
    val work: String? = null,
    val education: String? = null,
    @SerialName("current_city")
    val currentCity: String? = null,
    val hometown: String? = null,
    val website: String? = null,
    val pronouns: String? = null,
    @SerialName("linked_accounts")
    val linkedAccounts: List<LinkedAccount> = emptyList(),
    @SerialName("privacy_settings")
    val privacySettings: Map<String, String> = emptyMap()
)
