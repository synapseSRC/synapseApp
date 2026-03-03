package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.domain.model.User
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch



object UserProfileManager {

    private val dbService = SupabaseDatabaseService()
    private val authService = SupabaseAuthenticationService()
    private val profileCache = java.util.concurrent.ConcurrentHashMap<String, User>()



    suspend fun getUserProfile(uid: String): User? {

        profileCache[uid]?.let { return it }

        return try {
            val result = dbService.getSingle("users", "uid", uid).getOrNull()
            android.util.Log.d("UserProfileManager", "Database result for uid $uid: $result")
            if (result != null) {
                val user = User(
                    uid = result["uid"] as? String ?: "",
                    username = result["username"] as? String ?: "",
                    email = result["email"] as? String ?: "",
                    displayName = result["display_name"] as? String ?: "",
                    avatar = result["avatar"] as? String,
                    bio = result["bio"] as? String,
                    followersCount = (result["followers_count"] as? String)?.toIntOrNull() ?: 0,
                    followingCount = (result["following_count"] as? String)?.toIntOrNull() ?: 0,
                    postsCount = (result["posts_count"] as? String)?.toIntOrNull() ?: 0
                )

                profileCache[uid] = user
                user
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }



    suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Boolean {
        return try {
            dbService.update("users", updates, "uid", uid)

            profileCache.remove(uid)
            true
        } catch (e: Exception) {
            false
        }
    }



    suspend fun getCurrentUserProfile(): User? {
        val currentUid = authService.getCurrentUserId() ?: return null
        return getUserProfile(currentUid)
    }



    suspend fun updateCurrentUserProfile(updates: Map<String, Any?>): Boolean {
        val currentUid = authService.getCurrentUserId() ?: return false
        return updateUserProfile(currentUid, updates)
    }



    suspend fun searchUsers(query: String, limit: Int = 20): List<User> {
        return try {
            val results = dbService.selectWithFilter("users", "*", "username", "%$query%").getOrNull() ?: emptyList()

            results.take(limit).mapNotNull { result ->
                try {
                    User(
                        uid = result["uid"] as? String ?: "",
                        username = result["username"] as? String ?: "",
                        email = result["email"] as? String ?: "",
                        displayName = result["display_name"] as? String ?: "",
                        avatar = result["avatar"] as? String,
                        bio = result["bio"] as? String,
                        followersCount = (result["followers_count"] as? String)?.toIntOrNull() ?: 0,
                        followingCount = (result["following_count"] as? String)?.toIntOrNull() ?: 0,
                        postsCount = (result["posts_count"] as? String)?.toIntOrNull() ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }



    suspend fun getUserProfiles(uids: List<String>): List<User> {
        if (uids.isEmpty()) return emptyList()


        val cachedUsers = uids.mapNotNull { profileCache[it] }
        val missingUids = uids.filter { !profileCache.containsKey(it) }

        if (missingUids.isEmpty()) {
            return cachedUsers
        }

        return try {

            val results = dbService.selectWhereIn("users", "*", "uid", missingUids).getOrNull() ?: emptyList()

            val fetchedUsers = results.mapNotNull { result ->
                try {
                    val user = User(
                        uid = result["uid"] as? String ?: "",
                        username = result["username"] as? String ?: "",
                        email = result["email"] as? String ?: "",
                        displayName = result["display_name"] as? String ?: "",
                        avatar = result["avatar"] as? String,
                        bio = result["bio"] as? String,
                        followersCount = (result["followers_count"] as? String)?.toIntOrNull() ?: 0,
                        followingCount = (result["following_count"] as? String)?.toIntOrNull() ?: 0,
                        postsCount = (result["posts_count"] as? String)?.toIntOrNull() ?: 0
                    )

                    profileCache[user.uid] = user
                    user
                } catch (e: Exception) {
                    null
                }
            }


            cachedUsers + fetchedUsers
        } catch (e: Exception) {
            cachedUsers
        }
    }



    fun clearCache() {
        profileCache.clear()
    }



    fun clearUserCache(uid: String) {
        profileCache.remove(uid)
    }



    suspend fun userExists(uid: String): Boolean {
        return try {
            val result = dbService.getSingle("users", "uid", uid).getOrNull()
            result != null
        } catch (e: Exception) {
            false
        }
    }



    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val results = dbService.selectWithFilter("users", "*", "username", username).getOrNull() ?: emptyList()
            results.isEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
