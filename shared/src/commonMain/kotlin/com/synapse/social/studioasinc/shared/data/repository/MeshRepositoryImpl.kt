package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.datasource.MeshDataSource
import com.synapse.social.studioasinc.shared.data.local.database.MeshDao
import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshMessage
import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshPeer
import com.synapse.social.studioasinc.shared.domain.repository.MeshRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MeshRepositoryImpl(
    private val dataSource: MeshDataSource,
    private val dao: MeshDao
) : MeshRepository {
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            dataSource.meshMessages.collect { message ->
                dao.insertMessage(message)
            }
        }
        scope.launch {
            dataSource.nearbyPeers.collect { peers ->
                peers.forEach { dao.insertPeer(it) }
            }
        }
    }

    override fun getNearbyPeers(): Flow<List<MeshPeer>> = dataSource.nearbyPeers

    override fun getMeshMessages(): Flow<List<MeshMessage>> = flow {
        emit(dao.getAllMessages())
    }

    override suspend fun startDiscovery() = dataSource.startDiscovery()
    override suspend fun stopDiscovery() = dataSource.stopDiscovery()
    override suspend fun startAdvertising() = dataSource.startAdvertising()
    override suspend fun stopAdvertising() = dataSource.stopAdvertising()

    override suspend fun sendMessage(message: MeshMessage): Result<Unit> {
        dao.insertMessage(message)
        message.recipientId?.let {
            return dataSource.sendPayload(it, Json.encodeToString(message))
        }
        return Result.failure(Exception("No recipient specified"))
    }

    override suspend fun broadcastMessage(message: MeshMessage): Result<Unit> {
        dao.insertMessage(message)
        return dataSource.broadcastPayload(Json.encodeToString(message))
    }

    override suspend fun syncWithWan(): Result<Unit> {
        // Implementation for SyncMeshDataUseCase
        return Result.success(Unit)
    }

    override suspend fun getPendingSyncMessages(): List<MeshMessage> {
        return dao.getPendingSyncMessages()
    }

    override suspend fun markAsSynced(messageId: String): Result<Unit> {
        dao.markAsSynced(messageId)
        return Result.success(Unit)
    }
}
