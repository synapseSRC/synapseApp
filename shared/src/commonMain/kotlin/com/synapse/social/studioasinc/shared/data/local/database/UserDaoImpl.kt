package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.database.User
import com.synapse.social.studioasinc.shared.data.model.EncryptedString

class UserDaoImpl(private val database: StorageDatabase) : UserDao {

    private fun mapToUser(user: UserEntity): User {
        return User(
            id = user.uid,
            username = user.username ?: "",
            email = user.email?.let { EncryptedString(it) },
            fullName = user.fullName,
            avatarUrl = user.avatarUrl,
            bio = user.bio,
            website = user.website,
            location = user.location,
            isVerified = user.isVerified,
            followersCount = user.followersCount,
            followingCount = user.followingCount,
            postsCount = user.postsCount
        )
    }

    override suspend fun insertUser(user: UserEntity) {
        database.userQueries.insertUser(mapToUser(user))
    }

    override suspend fun insertAll(users: List<UserEntity>) {
        database.userQueries.transaction {
            users.forEach { user ->
                database.userQueries.insertUser(mapToUser(user))
            }
        }
    }

    override suspend fun getUserById(userId: String): UserEntity? {
        return database.userQueries.selectById(userId).executeAsOneOrNull()?.let {
            UserEntity(
                uid = it.id,
                username = it.username,
                email = it.email?.value,
                fullName = it.fullName,
                avatarUrl = it.avatarUrl,
                bio = it.bio,
                website = it.website,
                location = it.location,
                isVerified = it.isVerified,
                followersCount = it.followersCount,
                followingCount = it.followingCount,
                postsCount = it.postsCount
            )
        }
    }

    override suspend fun clearUsers() {
        database.userQueries.deleteAll()
    }
}
