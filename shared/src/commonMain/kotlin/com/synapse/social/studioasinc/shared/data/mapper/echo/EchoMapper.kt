package com.synapse.social.studioasinc.shared.data.mapper.echo

import com.synapse.social.studioasinc.shared.data.dto.echo.EchoFollowedMemberDto
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoLobbyRowDto
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoMediaCredentialsDto
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoParticipantDto
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoRoomDetailRowDto
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoRoomDto
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoFollowedMember
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoHostProfile
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoMediaCredentials
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRole
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoom
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomDetail
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomParticipant
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomStatus
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomSummary
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoVisibility
import com.synapse.social.studioasinc.shared.util.TimestampFormatter

object EchoMapper {

    fun toDomain(dto: EchoRoomDto): EchoRoom = EchoRoom(
        id = dto.id,
        hostId = dto.hostId,
        title = dto.title,
        topic = dto.topic?.takeIf { it.isNotBlank() },
        visibility = EchoVisibility.fromRaw(dto.visibility) ?: EchoVisibility.PUBLIC,
        status = EchoRoomStatus.fromRaw(dto.status) ?: EchoRoomStatus.LIVE,
        maxSpeakers = dto.maxSpeakers,
        createdAtMillis = millis(dto.createdAt),
        endedAtMillis = TimestampFormatter.parseInstant(dto.endedAt)?.toEpochMilliseconds()
    )

    fun toSummary(dto: EchoLobbyRowDto): EchoRoomSummary = EchoRoomSummary(
        room = EchoRoom(
            id = dto.id,
            hostId = dto.hostId,
            title = dto.title,
            topic = dto.topic?.takeIf { it.isNotBlank() },
            visibility = EchoVisibility.fromRaw(dto.visibility) ?: EchoVisibility.PUBLIC,
            status = EchoRoomStatus.LIVE,
            maxSpeakers = dto.maxSpeakers,
            createdAtMillis = millis(dto.createdAt)
        ),
        host = EchoHostProfile(
            userId = dto.hostId,
            username = dto.hostUsername,
            displayName = dto.hostDisplayName,
            avatarUrl = dto.hostAvatar
        ),
        speakerCount = dto.speakerCount,
        listenerCount = dto.listenerCount,
        followedInRoom = dto.followedInRoom.map(::toDomain)
    )

    fun toDetail(dto: EchoRoomDetailRowDto): EchoRoomDetail = EchoRoomDetail(
        room = EchoRoom(
            id = dto.id,
            hostId = dto.hostId,
            title = dto.title,
            topic = dto.topic?.takeIf { it.isNotBlank() },
            visibility = EchoVisibility.fromRaw(dto.visibility) ?: EchoVisibility.PUBLIC,
            status = EchoRoomStatus.fromRaw(dto.status) ?: EchoRoomStatus.LIVE,
            maxSpeakers = dto.maxSpeakers,
            createdAtMillis = millis(dto.createdAt),
            endedAtMillis = TimestampFormatter.parseInstant(dto.endedAt)?.toEpochMilliseconds()
        ),
        host = EchoHostProfile(
            userId = dto.hostId,
            username = dto.hostUsername,
            displayName = dto.hostDisplayName,
            avatarUrl = dto.hostAvatar
        ),
        myRole = EchoRole.fromRaw(dto.myRole),
        viewerId = dto.viewerId,
        participants = dto.participants.map(::toDomain)
    )

    fun toDomain(dto: EchoFollowedMemberDto): EchoFollowedMember = EchoFollowedMember(
        userId = dto.userId,
        username = dto.username,
        avatarUrl = dto.avatar
    )

    fun toDomain(dto: EchoParticipantDto): EchoRoomParticipant = EchoRoomParticipant(
        userId = dto.userId,
        role = EchoRole.fromRaw(dto.role) ?: EchoRole.AUDIENCE,
        handRaised = dto.handRaised,
        joinedAtMillis = millis(dto.joinedAt),
        username = dto.username,
        displayName = dto.displayName,
        avatarUrl = dto.avatar
    )

    fun toDomain(dto: EchoMediaCredentialsDto): EchoMediaCredentials = EchoMediaCredentials(
        serverUrl = dto.url,
        token = dto.token
    )

    private fun millis(timestamp: String?): Long =
        TimestampFormatter.parseInstant(timestamp)?.toEpochMilliseconds() ?: 0L
}

internal fun EchoVisibility.toWireValue(): String = when (this) {
    EchoVisibility.PUBLIC -> "public"
    EchoVisibility.UNLISTED -> "unlisted"
}

internal fun EchoRole.toWireValue(): String = when (this) {
    EchoRole.HOST -> "host"
    EchoRole.SPEAKER -> "speaker"
    EchoRole.AUDIENCE -> "audience"
}
