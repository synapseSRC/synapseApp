package com.synapse.social.studioasinc.shared.data.dto.echo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors a `public.echo_rooms` row (the composite `create_echo_room` returns). */
@Serializable
data class EchoRoomDto(
    val id: String,
    @SerialName("host_id") val hostId: String,
    val title: String,
    val topic: String? = null,
    val visibility: String? = null,
    val status: String? = null,
    @SerialName("max_speakers") val maxSpeakers: Int = 8,
    @SerialName("livekit_room") val livekitRoom: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null
)

/** One row of `get_echo_lobby`; counts and the followed members are computed server-side. */
@Serializable
data class EchoLobbyRowDto(
    val id: String,
    @SerialName("host_id") val hostId: String,
    val title: String,
    val topic: String? = null,
    val visibility: String? = null,
    @SerialName("max_speakers") val maxSpeakers: Int = 8,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("host_username") val hostUsername: String? = null,
    @SerialName("host_display_name") val hostDisplayName: String? = null,
    @SerialName("host_avatar") val hostAvatar: String? = null,
    @SerialName("speaker_count") val speakerCount: Int = 0,
    @SerialName("listener_count") val listenerCount: Int = 0,
    @SerialName("followed_in_room") val followedInRoom: List<EchoFollowedMemberDto> = emptyList()
)

@Serializable
data class EchoFollowedMemberDto(
    @SerialName("user_id") val userId: String,
    val username: String? = null,
    val avatar: String? = null
)

/** One row of `get_echo_room`. */
@Serializable
data class EchoRoomDetailRowDto(
    val id: String,
    @SerialName("host_id") val hostId: String,
    val title: String,
    val topic: String? = null,
    val visibility: String? = null,
    val status: String? = null,
    @SerialName("max_speakers") val maxSpeakers: Int = 8,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("host_username") val hostUsername: String? = null,
    @SerialName("host_display_name") val hostDisplayName: String? = null,
    @SerialName("host_avatar") val hostAvatar: String? = null,
    @SerialName("viewer_id") val viewerId: String? = null,
    @SerialName("my_role") val myRole: String? = null,
    val participants: List<EchoParticipantDto> = emptyList()
)

@Serializable
data class EchoParticipantDto(
    @SerialName("user_id") val userId: String,
    val role: String? = null,
    @SerialName("hand_raised") val handRaised: Boolean = false,
    @SerialName("joined_at") val joinedAt: String? = null,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val avatar: String? = null
)

@Serializable
data class EchoJoinResultDto(
    val role: String? = null
)

@Serializable
data class EchoHeartbeatResultDto(
    val status: String? = null
)

/** Response of the `livekit-token` Edge Function. */
@Serializable
data class EchoMediaCredentialsDto(
    val url: String,
    val token: String
)

@Serializable
data class EchoFunctionErrorDto(
    val error: String? = null
)
