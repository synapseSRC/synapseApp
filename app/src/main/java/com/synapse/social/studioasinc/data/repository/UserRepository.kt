package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.shared.data.local.database.UserDao
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import com.synapse.social.studioasinc.core.network.SupabaseErrorHandler
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient as SharedSupabaseClient
import com.synapse.social.studioasinc.shared.core.util.sanitizeSearchQuery

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val client: SupabaseClient = SharedSupabaseClient.client
) {

    suspend fun refreshUser(userId: String): Result<User?> {
        return try {
            val userProfile = client.from(SharedSupabaseClient.TABLE_USERS)
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<UserProfile>()

            if (userProfile != null) {
                val cachedUser = userDao.getUserById(userId)?.let { UserMapper.toModel(it) }

                val updatedUser = cachedUser?.copy(
                    username = userProfile.username,
                    displayName = userProfile.displayName,
                    avatar = userProfile.avatar?.let { url -> if (url.startsWith("http")) url else SharedSupabaseClient.constructAvatarUrl(url) },
                    email = userProfile.email,
                    verify = userProfile.verify
                ) ?: User(
                    uid = userProfile.id ?: userProfile.uid,
                    username = userProfile.username,
                    displayName = userProfile.displayName,
                    avatar = userProfile.avatar?.let { url -> if (url.startsWith("http")) url else SharedSupabaseClient.constructAvatarUrl(url) },
                    email = userProfile.email,
                    verify = userProfile.verify
                )

                userDao.insertUser(UserMapper.toUserEntity(updatedUser))
                Result.success(updatedUser)
            } else {
                 Result.success(null)
            }
        } catch (e: Exception) {
            return SupabaseErrorHandler.toResult(e, "UserRepository", "Failed to refresh user: $userId")
        }
    }

    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            // First try to get from cache
            var user = userDao.getUserById(userId)?.let { UserMapper.toModel(it) }
            
            // If not in cache or cache is stale, fetch from Supabase
            if (user == null) {
                val userProfile = client.from(SharedSupabaseClient.TABLE_USERS)
                    .select() {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingleOrNull<UserProfile>()

                userProfile?.let {
                    user = User(
                        uid = it.id ?: it.uid,
                        username = it.username,
                        displayName = it.displayName,
                        email = it.email,
                        avatar = it.avatar?.let { url -> if (url.startsWith("http")) url else SharedSupabaseClient.constructAvatarUrl(url) },
                        verify = it.verify,
                        bio = it.bio,
                        followersCount = it.followersCount ?: 0,
                        followingCount = it.followingCount ?: 0,
                        postsCount = it.postsCount ?: 0
                    )
                    userDao.insertUser(UserMapper.toUserEntity(user!!))
                }
            }
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to fetch user by ID: $userId", e)
            return SupabaseErrorHandler.toResult(e, "UserRepository", "Failed to fetch user by ID: $userId")
        }
    }

    suspend fun getUserByUsername(username: String): Result<UserProfile?> {
        return try {
            if (username.isBlank()) {
                return Result.failure(Exception("Username cannot be empty"))
            }

            val user = client.from(SharedSupabaseClient.TABLE_USERS)
                .select() {
                    filter {
                        eq("username", username)
                    }
                }
                .decodeSingleOrNull<UserProfile>()

            Result.success(user)
        } catch (e: Exception) {
            return SupabaseErrorHandler.toResult(e, "UserRepository", "Failed to fetch user by username: $username")
        }
    }

    suspend fun updateUser(user: UserProfile): Result<UserProfile> {
        return try {
            if (user.uid.isBlank()) {
                return Result.failure(Exception("User ID cannot be empty"))
            }

            val updateData = mapOf(
                "username" to user.username,
                "display_name" to user.displayName,
                "email" to user.email,
                "bio" to user.bio,
                "avatar" to user.avatar,
                "followers_count" to user.followersCount,
                "following_count" to user.followingCount,
                "posts_count" to user.postsCount,
                "status" to user.status,
                "account_type" to user.account_type,
                "verify" to user.verify,
                "banned" to user.banned
            )

            client.from(SharedSupabaseClient.TABLE_USERS)
                .update(updateData) {
                    filter {
                        eq("id", user.uid)
                    }
                }

            android.util.Log.d("UserRepository", "User updated successfully: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            return SupabaseErrorHandler.toResult(e, "UserRepository", "Failed to update user: ${user.uid}")
        }
    }

    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<UserProfile>> {
        return try {
            val sanitizedQuery = sanitizeSearchQuery(query)
            if (sanitizedQuery.isBlank()) {
                return Result.success(emptyList())
            }

            val users = client.from(SharedSupabaseClient.TABLE_USERS)
                .select() {
                    filter {
                        or {
                            ilike("username", "%$sanitizedQuery%")
                            ilike("display_name", "%$sanitizedQuery%")
                        }
                    }
                    limit(limit.toLong())
                }
                .decodeList<UserProfile>()

            android.util.Log.d("UserRepository", "Search found ${users.size} users for query: $sanitizedQuery")
            Result.success(users)
        } catch (e: Exception) {
            return SupabaseErrorHandler.toResult(e, "UserRepository", "Failed to search users with query: $query")
        }
    }

    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val existingUser = client.from(SharedSupabaseClient.TABLE_USERS)
                .select() {
                    filter {
                        eq("username", username)
                    }
                }
                .decodeSingleOrNull<UserProfile>()

            Result.success(existingUser == null)
        } catch (e: Exception) {
            return SupabaseErrorHandler.toResult(e, "UserRepository", "Failed to check username availability: $username")
        }
    }

    suspend fun isUsernameAvailable(username: String): Result<Boolean> {
        return checkUsernameAvailability(username)
    }

    @Serializable
    private data class UserProfileDto(
        val uid: String,
        val username: String? = null,
        @SerialName("display_name") val displayName: String? = null,
        val email: String? = null,
        val bio: String? = null,
        @SerialName("account_premium") val accountPremium: Boolean? = false,
        val verify: Boolean? = false,
        val region: String? = null,
        @SerialName("created_at") val createdAt: String? = null
    )
}
