package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.core.util.sanitizeSearchQuery
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.UserRepository
import com.synapse.social.studioasinc.shared.data.model.EncryptedString
import com.synapse.social.studioasinc.shared.data.datasource.UserDataSource
import kotlinx.coroutines.withContext

/**
 * Implementation of [UserRepository] that coordinates data flow between
 * the local SQLDelight database and the remote Supabase data source.
 *
 * This repository follows a local-first strategy where appropriate to reduce
 * network latency and provide offline support.
 */
class UserRepositoryImpl(
    private val database: StorageDatabase,
    private val userDataSource: UserDataSource
) : UserRepository {

    /**
     * Checks if the given [username] is available for a new user or a profile update.
     *
     * @param username The username to check.
     * @return A [Result] containing true if the username is available, false otherwise.
     */
    override suspend fun isUsernameAvailable(username: String): Result<Boolean> = withContext(AppDispatchers.IO) {
        userDataSource.isUsernameAvailable(username)
    }

    /**
     * Retrieves a user's profile by their unique identifier [uid].
     *
     * This method first attempts to fetch the profile from the local database.
     * If not found, it fetches from the network and caches the result locally.
     *
     * @param uid The unique identifier of the user.
     * @return A [Result] containing the [User] profile, or null if not found.
     */
    override suspend fun getUserProfile(uid: String): Result<User?> = withContext(AppDispatchers.IO) {
        runCatching {
            // Check local cache first to minimize network overhead and support offline viewing
            val localUser = database.userQueries.selectById(uid).executeAsOneOrNull()?.let { mapDbUser(it) }
            if (localUser != null) return@runCatching localUser

            // Fetch from network if local cache is empty
            val user = userDataSource.getUserProfile(uid).getOrThrow()

            // Construct full avatar URLs as the backend only stores the path/filename
            val mappedUser = user?.let { it.copy(avatar = it.avatar?.let { avatar -> SupabaseClient.constructAvatarUrl(avatar) }) }

            // Persist to local database for future offline access
            if (mappedUser != null) {
                database.userQueries.insertUser(mapDomainUser(mappedUser))
            }
            mappedUser
        }
    }

    /**
     * Searches for users matching the given [query].
     *
     * @param query The search string (e.g., username or display name).
     * @return A [Result] containing a list of matching [User]s.
     */
    override suspend fun searchUsers(query: String): Result<List<User>> = withContext(AppDispatchers.IO) {
        runCatching {
            val sanitizedQuery = sanitizeSearchQuery(query)
            // Immediately return empty if query is blank after sanitization to avoid unnecessary network calls
            if (sanitizedQuery.isBlank()) return@runCatching emptyList()

            val users = userDataSource.searchUsers(sanitizedQuery).getOrThrow()
            users.map { user ->
                // Construct full avatar URLs for search results to ensure they load in the UI
                user.copy(avatar = user.avatar?.let { avatar -> SupabaseClient.constructAvatarUrl(avatar) })
            }
        }
    }

    /**
     * Updates the profile of the user identified by [uid] with the provided [updates].
     *
     * Successful updates are also reflected in the local database cache.
     *
     * @param uid The unique identifier of the user to update.
     * @param updates A map containing the fields to be updated and their new values.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Result<Boolean> = withContext(AppDispatchers.IO) {
        runCatching {
            val user = userDataSource.updateUserProfile(uid, updates).getOrThrow()
            val mappedUser = user?.let { it.copy(avatar = it.avatar?.let { avatar -> SupabaseClient.constructAvatarUrl(avatar) }) }

            // Keep local cache in sync with the remote state
            if (mappedUser != null) {
                database.userQueries.insertUser(mapDomainUser(mappedUser))
            }
            // Success is tied to whether a valid user object was returned/mapped
            mappedUser != null
        }
    }

    /**
     * Retrieves the avatar URL for the currently authenticated user.
     *
     * @return A [Result] containing the avatar URL, or null if not set.
     */
    override suspend fun getCurrentUserAvatar(): Result<String?> = withContext(AppDispatchers.IO) {
        runCatching {
            userDataSource.getCurrentUserAvatar().getOrThrow()?.let { SupabaseClient.constructAvatarUrl(it) }
        }
    }

    /**
     * Maps a database user entity to a domain user model.
     */
    private fun mapDbUser(dbUser: com.synapse.social.studioasinc.shared.data.database.User): User {
        return User(
            uid = dbUser.id,
            username = dbUser.username,
            email = dbUser.email?.value,
            displayName = dbUser.fullName,
            avatar = dbUser.avatarUrl?.let { SupabaseClient.constructAvatarUrl(it) },
            bio = dbUser.bio,
            website = dbUser.website,
            location = dbUser.location,
            isVerified = dbUser.isVerified,
            followersCount = dbUser.followersCount,
            followingCount = dbUser.followingCount,
            postsCount = dbUser.postsCount
        )
    }

    /**
     * Maps a domain user model to a database user entity for persistence.
     */
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
