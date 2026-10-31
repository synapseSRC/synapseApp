package com.synapse.social.studioasinc.shared.domain.usecase.echo

import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoom
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoVisibility
import com.synapse.social.studioasinc.shared.domain.repository.EchoRoomRepository

/**
 * Opens a room and puts the caller on its stage.
 *
 * The backend returns the host's existing live room if there is one, so a double tap or a retry
 * after a dropped response cannot start a second room.
 */
class CreateEchoRoomUseCase(
    private val repository: EchoRoomRepository
) {
    suspend operator fun invoke(
        title: String,
        topic: String?,
        visibility: EchoVisibility,
        maxSpeakers: Int
    ): Result<EchoRoom> {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || cleanTitle.length > MAX_TITLE_LENGTH) {
            return Result.failure(IllegalArgumentException("Echo Room titles must be 1..$MAX_TITLE_LENGTH characters"))
        }
        val cleanTopic = topic?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanTopic != null && cleanTopic.length > MAX_TOPIC_LENGTH) {
            return Result.failure(IllegalArgumentException("Echo Room topics are limited to $MAX_TOPIC_LENGTH characters"))
        }
        val speakers = maxSpeakers.coerceIn(MIN_SPEAKERS, MAX_SPEAKERS)
        return repository.create(cleanTitle, cleanTopic, visibility, speakers)
    }

    private companion object {
        const val MAX_TITLE_LENGTH = 80
        const val MAX_TOPIC_LENGTH = 200
        const val MIN_SPEAKERS = 2
        const val MAX_SPEAKERS = 20
    }
}
