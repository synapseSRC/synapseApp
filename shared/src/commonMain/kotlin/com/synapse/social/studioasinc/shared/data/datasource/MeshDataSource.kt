package com.synapse.social.studioasinc.shared.data.datasource

import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshPeer
import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshMessage
import kotlinx.coroutines.flow.Flow

interface MeshDataSource {
    val nearbyPeers: Flow<List<MeshPeer>>
    val meshMessages: Flow<MeshMessage>

    suspend fun startDiscovery()
    suspend fun stopDiscovery()
    suspend fun startAdvertising()
    suspend fun stopAdvertising()

    suspend fun sendPayload(peerId: String, payload: String): Result<Unit>
    suspend fun broadcastPayload(payload: String): Result<Unit>
}
