package com.synapse.social.studioasinc.shared.data.datasource

import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshMessage
import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshPeer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class IosMeshDataSource : MeshDataSource {
    private val _nearbyPeers = MutableStateFlow<List<MeshPeer>>(emptyList())
    override val nearbyPeers = _nearbyPeers.asStateFlow()

    private val _meshMessages = MutableSharedFlow<MeshMessage>()
    override val meshMessages = _meshMessages.asSharedFlow()

    override suspend fun startDiscovery() {
        // Stub for Multipeer Connectivity
    }

    override suspend fun stopDiscovery() {
        // Stub for Multipeer Connectivity
    }

    override suspend fun startAdvertising() {
        // Stub for Multipeer Connectivity
    }

    override suspend fun stopAdvertising() {
        // Stub for Multipeer Connectivity
    }

    override suspend fun sendPayload(peerId: String, payload: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun broadcastPayload(payload: String): Result<Unit> {
        return Result.success(Unit)
    }
}
