package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.domain.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseUserDataSource(
    private val client: SupabaseClient
) : UserDataSource {

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> = withContext(AppDispatchers.IO) {
        runCatching {
            val count = client.postgrest["users"].select {
                filter {
                    eq("username", username)
                }
                count(Count.EXACT)
            }.countOrNull() ?: 0
            count == 0L
        }
    }

    override suspend fun getUserProfile(uid: String): Result<User?> = withContext(AppDispatchers.IO) {
        runCatching {
            val user = client.postgrest["users"].select {
                filter {
                    eq("uid", uid)
                }
            }.decodeSingleOrNull<User>() ?: return@runCatching null

            val followersCount = try {
                client.postgrest["follows"].select {
                    filter { eq("following_id", uid) }
                    count(Count.EXACT)
                }.countOrNull()?.toInt() ?: user.followersCount
            } catch (e: Exception) { user.followersCount }

            val followingCount = try {
                client.postgrest["follows"].select {
                    filter { eq("follower_id", uid) }
                    count(Count.EXACT)
                }.countOrNull()?.toInt() ?: user.followingCount
            } catch (e: Exception) { user.followingCount }

            user.copy(
                followersCount = followersCount,
                followingCount = followingCount
            )
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> = withContext(AppDispatchers.IO) {
        runCatching {
            val currentUserId = client.auth.currentUserOrNull()?.id
            
            client.postgrest["users"].select {
                filter {
                    or {
                        ilike("username", "$query%")
                        ilike("display_name", "$query%")
                    }
                    // Exclude current user from search results
                    if (currentUserId != null && currentUserId.isNotBlank()) {
                        filterNot("uid", FilterOperator.EQ, currentUserId)
                    }
                }
                limit(20)
            }.decodeList<User>()
        }
    }

    override suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Result<User?> = withContext(AppDispatchers.IO) {
        runCatching {
            client.postgrest["users"].update(updates) {
                filter {
                    eq("uid", uid)
                }
                select()
            }.decodeSingleOrNull<User>()
        }
    }

    override suspend fun getCurrentUserAvatar(): Result<String?> = withContext(AppDispatchers.IO) {
        runCatching {
            val currentUserId = client.auth.currentUserOrNull()?.id ?: return@runCatching null

            client.postgrest["users"].select {
                filter {
                    eq("uid", currentUserId)
                }
            }.decodeSingleOrNull<User>()?.avatar
        }
    }
}
