package com.synapse.social.studioasinc.shared.domain.usecase.echo

import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomSummary
import com.synapse.social.studioasinc.shared.domain.repository.EchoRoomRepository

/**
 * Loads the discoverable rooms.
 *
 * Expiry is best effort here: the lobby RPC expires again server-side before it reads, so a
 * failed cleanup call costs freshness, not the screen.
 */
class LoadEchoLobbyUseCase(
    private val repository: EchoRoomRepository
) {
    suspend operator fun invoke(limit: Int = EchoRoomRepository.DEFAULT_LOBBY_LIMIT): Result<List<EchoRoomSummary>> {
        repository.expireStale()
        return repository.lobby(limit)
    }
}
