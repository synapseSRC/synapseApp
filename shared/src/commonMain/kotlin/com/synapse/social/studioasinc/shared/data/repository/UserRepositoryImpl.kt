package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.sanitizeSearchQuery
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.UserRepository
import com.synapse.social.studioasinc.shared.data.model.EncryptedString
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val database: StorageDatabase,
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : UserRepository {

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> = withContext(Dispatchers.Default) {
        runCatching {
            val count = client.postgrest["users"].select {
                filter {
                    eq("username", username)
                }
                count(io.github.jan.supabase.postgrest.query.Count.EXACT)
            }.countOrNull() ?: 0
            count == 0L
        }
    }

    override suspend fun getUserProfile(uid: String): Result<User?> = withContext(Dispatchers.Default) {
        runCatching {
            // Try local DB first
            val localUser = database.userQueries.selectById(uid).executeAsOneOrNull()?.let { mapDbUser(it) }
            if (localUser != null) return@runCatching localUser

            // Fetch from network
            val user = client.postgrest["users"].select {
                filter {
                    eq("id", uid)
                }
            }.decodeSingleOrNull<User>()

            // Cache to DB if found
            if (user != null) {
                database.userQueries.insertUser(mapDomainUser(user))
            }
            user
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> = withContext(Dispatchers.Default) {
        runCatching {
            val sanitizedQuery = sanitizeSearchQuery(query)
            if (sanitizedQuery.isBlank()) return@runCatching emptyList()

            client.postgrest["users"].select {
                 filter {
                    or {
                        ilike("username", "$sanitizedQuery%")
                        ilike("display_name", "$sanitizedQuery%")
                    }
                 }
                 limit(20)
            }.decodeList<User>()
        }
    }

    override suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Result<Boolean> = withContext(Dispatchers.Default) {
        runCatching {
            val user = client.postgrest["users"].update(updates) {
                filter {
                    eq("id", uid)
                }
                select()
            }.decodeSingleOrNull<User>()

            if (user != null) {
                database.userQueries.insertUser(mapDomainUser(user))
            }
            true
        }
    }

    override suspend fun getCurrentUserAvatar(): Result<String?> = withContext(Dispatchers.Default) {
        runCatching {
            val currentUserId = client.auth.currentUserOrNull()?.id ?: return@runCatching null
            
            client.postgrest["users"].select {
                filter {
                    eq("id", currentUserId)
                }
            }.decodeSingleOrNull<User>()?.avatar?.let { constructAvatarUrl(it) }
        }
    }

    private fun constructAvatarUrl(path: String): String {
        if (path.startsWith("http")) return path
        val baseUrl = SynapseConfig.SUPABASE_URL
        val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        return "/storage/v1/object/public/avatars/"
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
