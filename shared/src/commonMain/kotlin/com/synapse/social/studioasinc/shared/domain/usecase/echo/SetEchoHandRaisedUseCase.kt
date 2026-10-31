package com.synapse.social.studioasinc.shared.domain.usecase.echo

import com.synapse.social.studioasinc.shared.domain.repository.EchoRoomRepository

class SetEchoHandRaisedUseCase(
    private val repository: EchoRoomRepository
) {
    suspend operator fun invoke(roomId: String, raised: Boolean): Result<Unit> =
        repository.setHandRaised(roomId, raised)
}
