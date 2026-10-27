package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallRequest
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import kotlinx.coroutines.flow.Flow

interface CallRepository {
    fun observeSession(callId: String): Flow<CallSession?>

    suspend fun start(request: CallRequest): Result<CallSession>
    suspend fun accept(callId: String): Result<Unit>
    suspend fun decline(callId: String): Result<Unit>
    suspend fun end(callId: String): Result<Unit>

    suspend fun setMuted(callId: String, muted: Boolean): Result<Unit>
    suspend fun setAudioRoute(callId: String, route: CallAudioRoute): Result<Unit>
    suspend fun setLocalVideoEnabled(callId: String, enabled: Boolean): Result<Unit>
    suspend fun flipCamera(callId: String): Result<Unit>
    suspend fun switchMediaType(callId: String, mediaType: CallMediaType): Result<Unit>
}
