package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = UserStatusSerializer::class)
enum class UserStatus {
    @SerialName("online") ONLINE,
    @SerialName("offline") OFFLINE;

    companion object {
        fun fromString(status: String?): UserStatus {
            return when (status?.lowercase()) {
                "online" -> ONLINE
                else -> OFFLINE
            }
        }
    }
}

object UserStatusSerializer : KSerializer<UserStatus> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UserStatus", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UserStatus) {
        encoder.encodeString(value.name.lowercase())
    }

    override fun deserialize(decoder: Decoder): UserStatus {
        return try {
            val string = decoder.decodeString()
            UserStatus.fromString(string)
        } catch (e: IllegalArgumentException) {
            UserStatus.OFFLINE
        } catch (e: SerializationException) {
            UserStatus.OFFLINE
        }
    }
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
    val bio: String? = null,
    val avatar: String? = null,
    @SerialName("avatar_history_type")
    val avatarHistoryType: String = "local",
    @SerialName("profile_cover_image")
    val profileCoverImage: String? = null,
    @SerialName("account_premium")
    val accountPremium: Boolean = false,
    @SerialName("user_level_xp")
    val userLevelXp: Int = 500,
    val verify: Boolean = false,
    @SerialName("account_type")
    val accountType: String = "user",
    val gender: String = "hidden",
    val banned: Boolean = false,
    @SerialName("status")
    val status: UserStatus = UserStatus.OFFLINE,
    @SerialName("join_date")
    val joinDate: String? = null,
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
    val postsCount: Int = 0
)



fun HashMap<String, Any?>.toUser(): User {
    return User(
        id = this["id"] as? String,
        uid = this["uid"] as? String ?: "",
        email = this["email"] as? String,
        username = this["username"] as? String,
        nickname = this["nickname"] as? String,
        displayName = this["display_name"] as? String,
        bio = this["bio"] as? String,
        avatar = this["avatar"] as? String,
        avatarHistoryType = this["avatar_history_type"] as? String ?: "local",
        profileCoverImage = this["profile_cover_image"] as? String,
        accountPremium = this["account_premium"] as? Boolean ?: false,
        userLevelXp = this["user_level_xp"] as? Int ?: 500,
        verify = this["verify"] as? Boolean ?: false,
        accountType = this["account_type"] as? String ?: "user",
        gender = this["gender"] as? String ?: "hidden",
        banned = this["banned"] as? Boolean ?: false,
        status = UserStatus.fromString(this["status"] as? String),
        joinDate = this["join_date"] as? String,
        oneSignalPlayerId = this["one_signal_player_id"] as? String,
        lastSeen = this["last_seen"] as? String,
        chattingWith = this["chatting_with"] as? String,
        createdAt = this["created_at"] as? String,
        updatedAt = this["updated_at"] as? String,
        followersCount = this["followers_count"] as? Int ?: 0,
        followingCount = this["following_count"] as? Int ?: 0,
        postsCount = this["posts_count"] as? Int ?: 0
    )
}
