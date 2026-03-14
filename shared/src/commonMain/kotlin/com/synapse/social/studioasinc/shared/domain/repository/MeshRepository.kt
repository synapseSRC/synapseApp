package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshPeer
import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshMessage
import kotlinx.coroutines.flow.Flow

interface MeshRepository {
    fun getNearbyPeers(): Flow<List<MeshPeer>>
    fun getMeshMessages(): Flow<List<MeshMessage>>

    suspend fun startDiscovery()
    suspend fun stopDiscovery()
    suspend fun startAdvertising()
    suspend fun stopAdvertising()

    suspend fun sendMessage(message: MeshMessage): Result<Unit>
    suspend fun broadcastMessage(message: MeshMessage): Result<Unit>

    suspend fun syncWithWan(): Result<Unit>
    suspend fun getPendingSyncMessages(): List<MeshMessage>
    suspend fun markAsSynced(messageId: String): Result<Unit>
}
