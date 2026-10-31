package com.synapse.social.studioasinc.shared.domain.usecase.echo

import com.synapse.social.studioasinc.shared.domain.repository.EchoRoomRepository
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioEngine

/** Ends the room for everyone. Only the host can; the backend enforces it. */
class EndEchoRoomUseCase(
    private val roomRepository: EchoRoomRepository,
    private val audioEngine: EchoAudioEngine
) {
    suspend operator fun invoke(roomId: String): Result<Unit> {
        val ended = roomRepository.end(roomId)
        if (ended.isSuccess) {
            audioEngine.disconnect()
        }
        return ended
    }
}
