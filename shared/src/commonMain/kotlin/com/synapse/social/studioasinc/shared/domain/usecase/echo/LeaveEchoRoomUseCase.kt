package com.synapse.social.studioasinc.shared.domain.usecase.echo

import com.synapse.social.studioasinc.shared.domain.repository.EchoRoomRepository
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioEngine

/**
 * Leaves a room.
 *
 * Audio stops before the membership row changes: publishing one more second after leaving would
 * be a microphone nobody can see on the roster.
 */
class LeaveEchoRoomUseCase(
    private val roomRepository: EchoRoomRepository,
    private val audioEngine: EchoAudioEngine
) {
    suspend operator fun invoke(roomId: String): Result<Unit> {
        audioEngine.disconnect()
        return roomRepository.leave(roomId)
    }
}
