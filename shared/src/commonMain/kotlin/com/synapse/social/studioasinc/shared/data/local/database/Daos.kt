package com.synapse.social.studioasinc.shared.data.local.database

interface UserDao {
    suspend fun insertUser(user: UserEntity)
    suspend fun insertAll(users: List<UserEntity>)
    suspend fun getUserById(userId: String): UserEntity?
    suspend fun clearUsers()
}

