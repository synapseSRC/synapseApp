package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshMessage
import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshPeer
import kotlinx.coroutines.Dispatchers
import com.synapse.social.studioasinc.shared.util.MeshIO

import kotlinx.coroutines.withContext

class SqlDelightMeshDao(
    private val db: StorageDatabase
) : MeshDao {
    override suspend fun insertMessage(message: MeshMessage) = withContext(Dispatchers.MeshIO) {
        db.meshQueries.insertMessage(
            id = message.id,
            senderId = message.senderId,
            recipientId = message.recipientId,
            chatId = message.chatId,
            content = message.content,
            timestamp = message.timestamp,
            type = message.type,
            isSynced = message.isSynced
        )
    }

    override suspend fun getAllMessages(): List<MeshMessage> = withContext(Dispatchers.MeshIO) {
        db.meshQueries.selectAllMessages().executeAsList().map {
            MeshMessage(it.id, it.senderId, it.recipientId, it.chatId, it.content, it.timestamp, it.type, it.isSynced ?: false)
        }
    }

    override suspend fun getPendingSyncMessages(): List<MeshMessage> = withContext(Dispatchers.MeshIO) {
        db.meshQueries.selectPendingSync().executeAsList().map {
            MeshMessage(it.id, it.senderId, it.recipientId, it.chatId, it.content, it.timestamp, it.type, it.isSynced ?: false)
        }
    }

    override suspend fun markAsSynced(id: String) = withContext(Dispatchers.MeshIO) {
        db.meshQueries.markAsSynced(id)
    }

    override suspend fun insertPeer(peer: MeshPeer) = withContext(Dispatchers.MeshIO) {
        db.meshQueries.insertPeer(peer.id, peer.name, peer.lastSeen)
    }

    override suspend fun getAllPeers(): List<MeshPeer> = withContext(Dispatchers.MeshIO) {
        db.meshQueries.selectAllPeers().executeAsList().map {
            MeshPeer(it.id, it.name, it.lastSeen)
        }
    }
}
