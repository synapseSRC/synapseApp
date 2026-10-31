package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRole
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoom
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomDetail
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomStatus
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomSummary
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoVisibility
import kotlinx.coroutines.flow.Flow

interface EchoRoomRepository {
    suspend fun lobby(limit: Int = DEFAULT_LOBBY_LIMIT): Result<List<EchoRoomSummary>>

    suspend fun create(
        title: String,
        topic: String?,
        visibility: EchoVisibility,
        maxSpeakers: Int
    ): Result<EchoRoom>

    suspend fun join(roomId: String): Result<EchoRole>
    suspend fun leave(roomId: String): Result<Unit>

    /** The 30-second liveness write; its answer also tells the screen whether the room still exists. */
    suspend fun heartbeat(roomId: String): Result<EchoRoomStatus>

    suspend fun end(roomId: String): Result<Unit>

    suspend fun setRole(roomId: String, userId: String, role: EchoRole): Result<Unit>
    suspend fun setHandRaised(roomId: String, raised: Boolean): Result<Unit>

    suspend fun fetchRoom(roomId: String): Result<EchoRoomDetail>

    /**
     * Room state changes, starting with one immediate read.
     *
     * Realtime is only a change signal: every emission is a fresh server read, so a missed
     * event costs latency rather than correctness.
     */
    fun observeRoom(roomId: String): Flow<EchoRoomDetail>

    suspend fun expireStale(): Result<Int>

    companion object {
        const val DEFAULT_LOBBY_LIMIT = 30
    }
}
