package com.synapse.social.studioasinc.shared.data.repository
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.sanitizeSearchQuery
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.UserRepository
import com.synapse.social.studioasinc.shared.data.model.EncryptedString
import com.synapse.social.studioasinc.shared.data.datasource.UserDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val database: StorageDatabase,
    private val userDataSource: UserDataSource
) : UserRepository {

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> = withContext(AppDispatchers.IO) {
        userDataSource.isUsernameAvailable(username)
    }

    override suspend fun getUserProfile(uid: String): Result<User?> = withContext(AppDispatchers.IO) {
        runCatching {
            // Try local DB first
            val localUser = database.userQueries.selectById(uid).executeAsOneOrNull()?.let { mapDbUser(it) }
            if (localUser != null) return@runCatching localUser

            // Fetch from network
            val user = userDataSource.getUserProfile(uid).getOrThrow()

            val mappedUser = user?.let { it.copy(avatar = it.avatar?.let { avatar -> constructAvatarUrl(avatar) }) }
            // Cache to DB if found
            if (mappedUser != null) {
                database.userQueries.insertUser(mapDomainUser(mappedUser))
            }
            mappedUser
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> = withContext(AppDispatchers.IO) {
        runCatching {
            val sanitizedQuery = sanitizeSearchQuery(query)
            if (sanitizedQuery.isBlank()) return@runCatching emptyList()

            val users = userDataSource.searchUsers(sanitizedQuery).getOrThrow()
            users.map { user ->
                user.copy(avatar = user.avatar?.let { constructAvatarUrl(it) })
            }
        }
    }

    override suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Result<Boolean> = withContext(AppDispatchers.IO) {
        runCatching {
            val user = userDataSource.updateUserProfile(uid, updates).getOrThrow()
            val mappedUser = user?.let { it.copy(avatar = it.avatar?.let { avatar -> constructAvatarUrl(avatar) }) }
            if (mappedUser != null) {
                database.userQueries.insertUser(mapDomainUser(mappedUser))
            }
            true
        }
    }

    override suspend fun getCurrentUserAvatar(): Result<String?> = withContext(AppDispatchers.IO) {
        runCatching {
            userDataSource.getCurrentUserAvatar().getOrThrow()?.let { constructAvatarUrl(it) }
        }
    }

    private fun constructAvatarUrl(path: String): String {
        if (path.startsWith("http")) return path
        val baseUrl = SynapseConfig.SUPABASE_URL
        val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        return "$cleanBaseUrl/storage/v1/object/public/avatars/$path"
    }

    private fun mapDbUser(dbUser: com.synapse.social.studioasinc.shared.data.database.User): User {
        return User(
            uid = dbUser.id,
            username = dbUser.username,
            email = dbUser.email?.value,
            displayName = dbUser.fullName,
            avatar = dbUser.avatarUrl?.let { constructAvatarUrl(it) },
            bio = dbUser.bio,
            website = dbUser.website,
            location = dbUser.location,
            isVerified = dbUser.isVerified,
            followersCount = dbUser.followersCount,
            followingCount = dbUser.followingCount,
            postsCount = dbUser.postsCount
        )
    }

    private fun mapDomainUser(user: User): com.synapse.social.studioasinc.shared.data.database.User {
        return com.synapse.social.studioasinc.shared.data.database.User(
            id = user.uid,
            username = user.username ?: "",
            email = user.email?.let { EncryptedString(it) },
            fullName = user.displayName,
            avatarUrl = user.avatar,
            bio = user.bio,
            website = user.website,
            location = user.location,
            isVerified = user.isVerified,
            followersCount = user.followersCount,
            followingCount = user.followingCount,
            postsCount = user.postsCount
        )
    }
}
