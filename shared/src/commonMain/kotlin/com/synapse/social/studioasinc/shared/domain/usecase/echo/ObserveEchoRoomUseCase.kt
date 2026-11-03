package com.synapse.social.studioasinc.shared.domain.usecase.echo

import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomDetail
import com.synapse.social.studioasinc.shared.domain.repository.EchoRoomRepository
import kotlinx.coroutines.flow.Flow

class ObserveEchoRoomUseCase(
    private val repository: EchoRoomRepository
) {
    operator fun invoke(roomId: String): Flow<EchoRoomDetail> = repository.observeRoom(roomId)
}
