package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.echo.EchoMediaCredentials

interface EchoTokenRepository {
    /**
     * Mints a token for the room the backend says this user may join.
     *
     * The role behind the publish grant is resolved server-side, so the caller cannot ask
     * for more than it has.
     */
    suspend fun mint(roomId: String): Result<EchoMediaCredentials>
}
