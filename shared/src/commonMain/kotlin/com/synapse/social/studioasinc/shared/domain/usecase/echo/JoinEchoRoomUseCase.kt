package com.synapse.social.studioasinc.shared.domain.usecase.echo

import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRole
import com.synapse.social.studioasinc.shared.domain.repository.EchoRoomRepository
import com.synapse.social.studioasinc.shared.domain.repository.EchoTokenRepository
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioEngine
import kotlinx.coroutines.CancellationException

/**
 * Joins a room and starts listening.
 *
 * Membership is written first because the token function refuses anyone who is not a member.
 * [beforeConnect] runs in between: it is the caller's chance to bring up the foreground service
 * with the right type for the role just granted, before any microphone is opened. Anything that
 * fails after membership is undone, so a retry does not leave the join behind.
 */
class JoinEchoRoomUseCase(
    private val roomRepository: EchoRoomRepository,
    private val tokenRepository: EchoTokenRepository,
    private val audioEngine: EchoAudioEngine
) {
    suspend operator fun invoke(
        roomId: String,
        beforeConnect: suspend (EchoRole) -> Unit = {}
    ): Result<EchoRole> {
        if (roomId.isBlank()) {
            return Result.failure(IllegalArgumentException("An Echo Room needs a room id"))
        }

        val role = roomRepository.join(roomId).getOrElse { return Result.failure(it) }

        val credentials = tokenRepository.mint(roomId).getOrElse {
            roomRepository.leave(roomId)
            return Result.failure(it)
        }

        try {
            beforeConnect(role)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            audioEngine.disconnect()
            roomRepository.leave(roomId)
            return Result.failure(e)
        }

        val connected = audioEngine.connect(credentials, publish = role.canPublish)
        if (connected.isFailure) {
            audioEngine.disconnect()
            roomRepository.leave(roomId)
        }
        return connected.map { role }
    }
}
