package com.synapse.social.studioasinc.data.repository

import android.content.Context
import com.synapse.social.studioasinc.shared.domain.usecase.UploadMediaUseCase
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import com.synapse.social.studioasinc.shared.domain.model.MediaType
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.core.network.SupabaseErrorHandler
import com.synapse.social.studioasinc.shared.core.util.EducationSanitizer

import com.synapse.social.studioasinc.domain.model.UserProfile
import com.synapse.social.studioasinc.domain.model.UserStatus
import com.synapse.social.studioasinc.domain.model.Gender
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.HistoryItem
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import com.synapse.social.studioasinc.core.util.toJsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class EditProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uploadMediaUseCase: UploadMediaUseCase
) {

    private val client = SupabaseClient.client

    suspend fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    fun getUserProfile(userId: String): Flow<Result<UserProfile>> = flow {
        try {
            val result = client.from("users")
                .select(columns = Columns.raw("*")) {
                    filter { eq("uid", userId) }
                }
                .decodeSingleOrNull<JsonObject>()

            if (result != null) {

                val user = UserProfile(
                    uid = result["uid"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: userId,
                    username = result["username"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: "",
                    displayName = result["display_name"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                        ?: result["nickname"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    email = result["email"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    bio = result["bio"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    avatar = result["avatar"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    profileCoverImage = result["profile_cover_image"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    gender = Gender.fromString(result["gender"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull),
                    region = result["region"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    status = UserStatus.fromString(result["status"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull),
                    followersCount = result["followers_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.intOrNull ?: 0,
                    followingCount = result["following_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.intOrNull ?: 0,
                    postsCount = result["posts_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.intOrNull ?: 0,
                    currentCity = result["current_city"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    hometown = result["hometown"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    occupation = result["occupation"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    workplace = result["workplace"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    education = result["education"]?.let { element ->
                        val rawList = when (element) {
                            is kotlinx.serialization.json.JsonArray -> element.mapNotNull { item ->
                                (item as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                            }
                            is kotlinx.serialization.json.JsonPrimitive -> element.contentOrNull?.let { listOf(it) } ?: emptyList()
                            else -> emptyList()
                        }
                        EducationSanitizer.sanitizeEducationList(rawList)
                    } ?: emptyList(),
                    workHistory = result["work_history"]?.let { element ->
                        when (element) {
                            is kotlinx.serialization.json.JsonArray -> element.mapNotNull { item ->
                                (item as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                            }
                            else -> emptyList()
                        }
                    } ?: emptyList(),
                    interests = result["interests"]?.let { element ->
                        when (element) {
                            is kotlinx.serialization.json.JsonArray -> element.mapNotNull { item ->
                                (item as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                            }
                            else -> emptyList()
                        }
                    } ?: emptyList(),
                    travel = result["travel"]?.let { element ->
                        when (element) {
                            is kotlinx.serialization.json.JsonArray -> element.mapNotNull { item ->
                                (item as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                            }
                            else -> emptyList()
                        }
                    } ?: emptyList(),
                    pronouns = result["pronouns"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    birthday = result["birthday"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    relationshipStatus = result["relationship_status"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    discordTag = result["discord_tag"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    githubProfile = result["github_profile"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    personalWebsite = result["personal_website"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    publicEmail = result["public_email"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                )
                emit(Result.success(user))
            } else {
                emit(Result.failure(Exception("User not found")))
            }
        } catch (e: Exception) {
            emit(SupabaseErrorHandler.toResult(e, "EditProfileRepository", "Failed to get user profile: $userId"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun updateProfile(userId: String, updateData: Map<String, Any?>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("users").update(updateData.toJsonObject()) {
                    filter { eq("uid", userId) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                SupabaseErrorHandler.toResult(e, "EditProfileRepository", "Failed to update profile: $userId")
            }
        }
    }

    suspend fun syncUsernameChange(oldUsername: String, newUsername: String, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("usernames").delete {
                    filter { eq("username", oldUsername) }
                }

                val email = client.auth.currentUserOrNull()?.email

                val usernameData = mapOf(
                    "uid" to userId,
                    "email" to email,
                    "username" to newUsername
                )
                client.from("usernames").upsert(usernameData)
                Result.success(Unit)
            } catch (e: Exception) {
                SupabaseErrorHandler.toResult(e, "EditProfileRepository", "Failed to sync username: $newUsername")
            }
        }
    }

    suspend fun checkUsernameAvailability(username: String, currentUserId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from("users")
                    .select(columns = Columns.raw("uid")) {
                        filter { eq("username", username) }
                    }
                    .decodeList<JsonObject>()



                if (result.isEmpty()) {
                    Result.success(true)
                } else {
                    val existingUserId = result.first()["uid"]?.toString()?.removeSurrounding("\"")
                    Result.success(existingUserId == currentUserId)
                }
            } catch (e: Exception) {
                SupabaseErrorHandler.toResult(e, "EditProfileRepository", "Failed to check username availability: $username")
            }
        }
    }

    suspend fun uploadAvatar(userId: String, imagePath: String): Result<String> {
        return uploadFile(imagePath, SupabaseClient.BUCKET_USER_AVATARS)
    }

    suspend fun uploadCover(userId: String, imagePath: String): Result<String> {
        return uploadFile(imagePath, SupabaseClient.BUCKET_USER_COVERS)
    }

    private suspend fun uploadFile(filePath: String, bucketName: String): Result<String> {
        return uploadMediaUseCase(
            filePath = filePath,
            mediaType = MediaType.PHOTO,
            bucketName = bucketName,
            onProgress = {}
        )
    }

    suspend fun addToProfileHistory(userId: String, imageUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val historyData = mapOf(
                    "user_id" to userId,
                    "avatar" to imageUrl.trim()
                )
                client.from("profile_history").insert(historyData.toJsonObject())
                Result.success(Unit)
            } catch (e: Exception) {
                SupabaseErrorHandler.toResult(e, "EditProfileRepository", "Failed to add to profile history for user: $userId")
            }
        }
    }

    suspend fun addToCoverHistory(userId: String, imageUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val historyData = mapOf(
                    "user_id" to userId,
                    "cover_image_url" to imageUrl.trim()
                )
                client.from("cover_image_history").insert(historyData.toJsonObject())
                Result.success(Unit)
            } catch (e: Exception) {
                SupabaseErrorHandler.toResult(e, "EditProfileRepository", "Failed to add to cover history for user: $userId")
            }
        }
    }

    fun getProfileHistory(userId: String): Flow<Result<List<HistoryItem>>> = flow {
        try {
            val result = client.from("profile_history")
                .select(columns = Columns.raw("*")) {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<JsonObject>()

            val items = result.mapNotNull {
                parseProfileHistoryItem(it)
            }

            emit(Result.success(items))
        } catch (e: Exception) {
            emit(SupabaseErrorHandler.toResult(e, "EditProfileRepository", "Failed to get profile history: $userId"))
        }
    }.flowOn(Dispatchers.IO)

    fun getCoverHistory(userId: String): Flow<Result<List<HistoryItem>>> = flow {
        try {
            val result = client.from("cover_image_history")
                .select(columns = Columns.raw("*")) {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<JsonObject>()

            val items = result.mapNotNull {
                parseCoverHistoryItem(it)
            }

            emit(Result.success(items))
        } catch (e: Exception) {
            emit(SupabaseErrorHandler.toResult(e, "EditProfileRepository", "Failed to get cover history: $userId"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteProfileHistoryItem(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("profile_history").delete {
                    filter { eq("id", id) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                SupabaseErrorHandler.toResult(e, "EditProfileRepository", "Failed to delete profile history item: $id")
            }
        }
    }

    suspend fun deleteCoverHistoryItem(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("cover_image_history").delete {
                    filter { eq("id", id) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                SupabaseErrorHandler.toResult(e, "EditProfileRepository", "Failed to delete cover history item: $id")
            }
        }
    }

    internal fun parseProfileHistoryItem(json: JsonObject): HistoryItem? {
        val id = json["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return null
        val userId = json["user_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return null
        val avatar = json["avatar"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return null
        val createdAt = json["created_at"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull

        return HistoryItem(id = id, userId = userId, imageUrl = avatar, createdAt = createdAt)
    }

    internal fun parseCoverHistoryItem(json: JsonObject): HistoryItem? {
        val id = json["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return null
        val userId = json["user_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return null
        val coverImageUrl = json["cover_image_url"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return null
        val createdAt = json["created_at"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull

        return HistoryItem(id = id, userId = userId, imageUrl = coverImageUrl, createdAt = createdAt)
    }
}
