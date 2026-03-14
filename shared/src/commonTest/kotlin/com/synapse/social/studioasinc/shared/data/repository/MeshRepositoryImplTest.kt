package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.datasource.MeshDataSource
import com.synapse.social.studioasinc.shared.data.local.database.MeshDao
import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshMessage
import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshPeer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MeshRepositoryImplTest {

    private class FakeMeshDataSource : MeshDataSource {
        override val nearbyPeers = MutableStateFlow<List<MeshPeer>>(emptyList())
        override val meshMessages = MutableSharedFlow<MeshMessage>()
        override suspend fun startDiscovery() {}
        override suspend fun stopDiscovery() {}
        override suspend fun startAdvertising() {}
        override suspend fun stopAdvertising() {}
        override suspend fun sendPayload(peerId: String, payload: String): Result<Unit> = Result.success(Unit)
        override suspend fun broadcastPayload(payload: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeMeshDao : MeshDao {
        val messages = mutableListOf<MeshMessage>()
        val peers = mutableListOf<MeshPeer>()
        override suspend fun insertMessage(message: MeshMessage) { messages.add(message) }
        override suspend fun getAllMessages(): List<MeshMessage> = messages
        override suspend fun getPendingSyncMessages(): List<MeshMessage> = messages.filter { !it.isSynced }
        override suspend fun markAsSynced(id: String) {
            val index = messages.indexOfFirst { it.id == id }
            if (index != -1) {
                messages[index] = messages[index].copy(isSynced = true)
            }
        }
        override suspend fun insertPeer(peer: MeshPeer) { peers.add(peer) }
        override suspend fun getAllPeers(): List<MeshPeer> = peers
    }

    @Test
    fun testSendMessageInsertsIntoDao() = runTest {
        val dataSource = FakeMeshDataSource()
        val dao = FakeMeshDao()
        val repository = MeshRepositoryImpl(dataSource, dao)

        val message = MeshMessage("1", "sender", "recipient", "chat", "hello", 1000L)
        repository.sendMessage(message)

        assertEquals(1, dao.messages.size)
        assertEquals("hello", dao.messages[0].content)
    }
}
