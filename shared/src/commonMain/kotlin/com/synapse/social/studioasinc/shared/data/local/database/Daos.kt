package com.synapse.social.studioasinc.shared.data.local.database

interface UserDao {
    suspend fun insertUser(user: UserEntity)
    suspend fun insertAll(users: List<UserEntity>)
    suspend fun getUserById(userId: String): UserEntity?
    suspend fun clearUsers()
}

interface IdentityKeyDao {
    suspend fun insertIdentityKey(key: IdentityKeyEntity)
    suspend fun getIdentityKey(userId: String): IdentityKeyEntity?
}

interface MeshDao {
    suspend fun insertMessage(message: com.synapse.social.studioasinc.shared.domain.model.mesh.MeshMessage)
    suspend fun getAllMessages(): List<com.synapse.social.studioasinc.shared.domain.model.mesh.MeshMessage>
    suspend fun getPendingSyncMessages(): List<com.synapse.social.studioasinc.shared.domain.model.mesh.MeshMessage>
    suspend fun markAsSynced(id: String)
    suspend fun insertPeer(peer: com.synapse.social.studioasinc.shared.domain.model.mesh.MeshPeer)
    suspend fun getAllPeers(): List<com.synapse.social.studioasinc.shared.domain.model.mesh.MeshPeer>
}
