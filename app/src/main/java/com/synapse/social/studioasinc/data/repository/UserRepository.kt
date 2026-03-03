package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.shared.data.local.database.UserDao
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.UserProfile
import com.synapse.social.studioasinc.domain.model.AccountInfo
import com.synapse.social.studioasinc.domain.model.SubscriptionType
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
                        eq("uid", userId)
                    }
                }
                .decodeSingleOrNull<UserProfile>()

            if (userProfile != null) {
                val cachedUser = userDao.getUserById(userId)?.let { UserMapper.toModel(it) }

                val updatedUser = cachedUser?.copy(
                    username = userProfile.username,
                    displayName = userProfile.displayName,
                    avatar = userProfile.avatar,
                    email = userProfile.email,
                    verify = userProfile.verify
                ) ?: User(
                    uid = userProfile.uid,
                    username = userProfile.username,
                    displayName = userProfile.displayName,
                    avatar = userProfile.avatar,
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
            var user = userDao.getUserById(userId)?.let { UserMapper.toModel(it) }
            if (user == null) {
                val userProfile = client.from(SharedSupabaseClient.TABLE_USERS)
                    .select() {
                        filter {
                            eq("uid", userId)
                        }
                    }
                    .decodeSingleOrNull<UserProfile>()

                userProfile?.let {
                    user = User(
                        uid = it.uid,
                        username = it.username,
                        displayName = it.displayName,
                        email = it.email,
                        avatar = it.avatar,
                        verify = it.verify
                    )
                    userDao.insertUser(UserMapper.toUserEntity(user!!))
                }
            }
            Result.success(user)
        } catch (e: Exception) {
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
                        eq("uid", user.uid)
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

    suspend fun getAccountInfo(userId: String): Result<AccountInfo> {
        return try {
            val authUser = client.auth.currentUserOrNull()

            if (authUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            val profileResult = client.from(SharedSupabaseClient.TABLE_USERS).select {
                filter {
                    eq("uid", userId)
                }
            }.decodeSingleOrNull<UserProfileDto>()

            if (profileResult == null) {
                return Result.failure(Exception("Profile not found"))
            }

            val postsCount = client.from("posts").select(columns = Columns.list("id")) {
                count(Count.EXACT)
                filter { eq("author_uid", userId) }
            }.countOrNull() ?: 0

            val followersCount = client.from("follows").select(columns = Columns.list("id")) {
                count(Count.EXACT)
                filter { eq("following_id", userId) }
            }.countOrNull() ?: 0

            val followingCount = client.from("follows").select(columns = Columns.list("id")) {
                count(Count.EXACT)
                filter { eq("follower_id", userId) }
            }.countOrNull() ?: 0

            val storiesCount = client.from("stories").select(columns = Columns.list("id")) {
                count(Count.EXACT)
                filter { eq("user_id", userId) }
            }.countOrNull() ?: 0

            val reelsCount = client.from("reels").select(columns = Columns.list("id")) {
                count(Count.EXACT)
                filter { eq("creator_id", userId) }
            }.countOrNull() ?: 0

            val accountInfo = AccountInfo(
                userId = userId,
                username = profileResult.username ?: "N/A",
                displayName = profileResult.displayName ?: "N/A",
                email = profileResult.email ?: authUser.email ?: "N/A",
                phoneNumber = authUser.phone,
                bio = profileResult.bio,
                accountType = if (profileResult.accountPremium == true) SubscriptionType.PLUS else SubscriptionType.FREE,
                isVerified = profileResult.verify == true,
                createdAt = profileResult.createdAt,
                lastLoginAt = authUser.lastSignInAt?.let { kotlinx.datetime.Instant.fromEpochMilliseconds(it.toEpochMilliseconds()) },
                postsCount = postsCount.toInt(),
                followersCount = followersCount.toInt(),
                followingCount = followingCount.toInt(),
                storiesCount = storiesCount.toInt(),
                reelsCount = reelsCount.toInt(),
                region = profileResult.region ?: "Unknown",
                language = "English"
            )

            Result.success(accountInfo)
        } catch (e: Exception) {
            SupabaseErrorHandler.toResult(e, "UserRepository", "Failed to fetch account info")
        }
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
