package com.synapse.social.studioasinc.shared.domain.usecase.echo

import com.synapse.social.studioasinc.shared.domain.repository.EchoTokenRepository
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioEngine

/**
 * Reconnects with a fresh token.
 *
 * Tokens expire, and a role change only takes effect in the next one, so reconnecting is always
 * mint-then-connect rather than reusing whatever the previous connection was granted.
 */
class ReconnectEchoAudioUseCase(
    private val tokenRepository: EchoTokenRepository,
    private val audioEngine: EchoAudioEngine
) {
    suspend operator fun invoke(roomId: String, publish: Boolean): Result<Unit> {
        val credentials = tokenRepository.mint(roomId).getOrElse { return Result.failure(it) }
        return audioEngine.reconnect(credentials, publish)
    }
}
