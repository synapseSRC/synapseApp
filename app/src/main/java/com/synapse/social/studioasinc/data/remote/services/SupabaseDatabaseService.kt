package com.synapse.social.studioasinc.data.remote.services

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.remote.services.interfaces.IDatabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull



class SupabaseDatabaseService : IDatabaseService {

    companion object {
        private const val TAG = "SupabaseDB"
    }

    private val client = SupabaseClient.client



    private fun extractJsonValue(element: JsonElement): Any? {
        return if (element is JsonNull) {
            null
        } else {
            element.toString().removeSurrounding("\"")
        }
    }



    suspend fun insert(table: String, data: Map<String, Any?>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "Inserting data into table '$table'")

                val insertData = kotlinx.serialization.json.buildJsonObject {
                    data.forEach { (key, value) ->
                        val convertedValue = convertTimestampIfNeeded(key, value)
                        when (convertedValue) {
                            is String -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue))
                            is Number -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue))
                            is Boolean -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue))
                            null -> put(key, kotlinx.serialization.json.JsonNull)
                            else -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue.toString()))
                        }
                    }
                }

                client.from(table).insert(insertData)
                android.util.Log.d(TAG, "Data inserted successfully into table '$table'")

                Result.success(Unit)

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Database insertion failed for table '$table'", e)

                val errorMessage = when {
                    e.message?.contains("serialization", ignoreCase = true) == true ->
                        "Data serialization error: ${e.message}"
                    e.message?.contains("duplicate", ignoreCase = true) == true ->
                        "Duplicate entry error: ${e.message}"
                    e.message?.contains("constraint", ignoreCase = true) == true ->
                        "Database constraint violation: ${e.message}"
                    e.message?.contains("column", ignoreCase = true) == true ->
                        "Database column error: ${e.message}"
                    e.message?.contains("table", ignoreCase = true) == true ->
                        "Database table error: ${e.message}"
                    else -> e.message ?: "Database insertion failed"
                }
                Result.failure(Exception(errorMessage))
            }
        }
    }



    private fun convertTimestampIfNeeded(key: String, value: Any?): Any? {

        val timestampFields = listOf(
            "created_at", "updated_at", "last_seen", "timestamp",
            "publish_date", "last_message_time", "joined_at",
            "last_read_at", "edited_at", "push_date", "read_at", "delivered_at"
        )

        return when {
            key in timestampFields && value is Number -> {

                try {
                    java.time.Instant.ofEpochMilli(value.toLong()).toString()
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to convert timestamp for field '$key': $value", e)
                    value
                }
            }
            key in timestampFields && value is String -> {

                try {
                    val longValue = value.toLongOrNull()
                    if (longValue != null && longValue > 1000000000000) {
                        java.time.Instant.ofEpochMilli(longValue).toString()
                    } else {
                        value
                    }
                } catch (e: Exception) {
                    value
                }
            }
            else -> value
        }
    }



    override suspend fun update(table: String, data: Map<String, Any?>, filter: String, value: Any): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "Updating data in table '$table' where $filter=$value")


                val updateData = kotlinx.serialization.json.buildJsonObject {
                    data.forEach { (key, value) ->
                        val convertedValue = convertTimestampIfNeeded(key, value)
                        when (convertedValue) {
                            is String -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue))
                            is Number -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue))
                            is Boolean -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue))
                            null -> put(key, kotlinx.serialization.json.JsonNull)
                            else -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue.toString()))
                        }
                    }
                }

                client.from(table).update(updateData) {
                    filter {
                        eq(filter, value)
                    }
                }
                android.util.Log.d(TAG, "Data updated successfully in table '$table'")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Database update failed for table '$table'", e)
                Result.failure(e)
            }
        }
    }





    override suspend fun select(table: String, columns: String): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select(columns = Columns.raw(columns))
                    .decodeList<JsonObject>()

                val mappedResult = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        extractJsonValue(value)
                    }
                }
                Result.success(mappedResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    override suspend fun selectWhere(table: String, columns: String, filter: String, value: Any): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select(columns = Columns.raw(columns)) {
                    filter {
                        eq(filter, value)
                    }
                }.decodeList<JsonObject>()

                val mappedResult = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, jsonValue) ->
                        extractJsonValue(jsonValue)
                    }
                }
                Result.success(mappedResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    override suspend fun selectWhereIn(table: String, columns: String, filter: String, values: List<Any>): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select(columns = Columns.raw(columns)) {
                    filter {
                        isIn(filter, values)
                    }
                }.decodeList<JsonObject>()

                val mappedResult = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, jsonValue) ->
                        extractJsonValue(jsonValue)
                    }
                }
                Result.success(mappedResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    override suspend fun delete(table: String, filter: String, value: Any): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from(table).delete {
                    filter {
                        eq(filter, value)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    override suspend fun deleteWhere(table: String, filter: String, value: Any): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from(table).delete {
                    filter {
                        eq(filter, value)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    override suspend fun count(table: String): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select(columns = Columns.raw("*"))
                    .decodeList<JsonObject>()
                Result.success(result.size.toLong())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    override suspend fun exists(table: String, filter: String, value: Any): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select(columns = Columns.raw("*")) {
                    filter {
                        eq(filter, value)
                    }
                }.decodeList<JsonObject>()
                Result.success(result.isNotEmpty())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    override suspend fun upsert(table: String, data: Map<String, Any?>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val upsertData = kotlinx.serialization.json.buildJsonObject {
                    data.forEach { (key, value) ->
                        val convertedValue = convertTimestampIfNeeded(key, value)
                        when (convertedValue) {
                            is String -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue))
                            is Number -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue))
                            is Boolean -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue))
                            null -> put(key, kotlinx.serialization.json.JsonNull)
                            else -> put(key, kotlinx.serialization.json.JsonPrimitive(convertedValue.toString()))
                        }
                    }
                }
                client.from(table).upsert(upsertData)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    suspend fun getSingle(table: String, filter: String, value: Any): Result<Map<String, Any?>?> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select {
                    filter {
                        eq(filter, value)
                    }
                }.decodeSingleOrNull<JsonObject>()

                val mappedResult = result?.toMap()?.mapValues { (_, jsonValue) ->
                    extractJsonValue(jsonValue)
                }
                Result.success(mappedResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    suspend fun updatePresence(userId: String, isOnline: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {

                val timestamp = java.time.Instant.now().toString()
                val presenceData = kotlinx.serialization.json.buildJsonObject {
                    put("user_id", kotlinx.serialization.json.JsonPrimitive(userId))
                    put("is_online", kotlinx.serialization.json.JsonPrimitive(isOnline))
                    put("last_seen", kotlinx.serialization.json.JsonPrimitive(timestamp))
                }
                client.from("user_presence").upsert(presenceData)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    suspend fun selectWithFilter(table: String, columns: String = "*", filter: String, value: Any): Result<List<Map<String, Any?>>> {
        return selectWhere(table, columns, filter, value)
    }



    suspend fun selectById(table: String, id: String, columns: String = "*"): Result<Map<String, Any?>?> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select(columns = Columns.raw(columns)) {
                    filter { eq("id", id) }
                }.decodeSingleOrNull<JsonObject>()

                val mappedResult = result?.toMap()?.mapValues { (_, jsonValue) ->
                    jsonValue.toString().removeSurrounding("\"")
                }
                Result.success(mappedResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



    suspend fun searchPosts(query: String, limit: Int = 20): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "Searching posts with query: $query")


                val sanitizedQuery = sanitizeSearchQuery(query)

                val result = client.from("posts").select(columns = Columns.raw("*")) {
                    filter {
                        ilike("post_text", "%$sanitizedQuery%")
                    }
                    limit(limit.toLong())
                    order(column = "timestamp", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }.decodeList<JsonObject>()

                val mappedResult = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        extractJsonValue(value)
                    }
                }

                android.util.Log.d(TAG, "Found ${mappedResult.size} posts matching query")
                Result.success(mappedResult)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Post search failed", e)
                Result.failure(e)
            }
        }
    }



    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "Searching users with query: $query")


                val sanitizedQuery = sanitizeSearchQuery(query)

                val result = client.from("users").select(columns = Columns.raw("*")) {
                    filter {
                        or {
                            ilike("username", "%$sanitizedQuery%")
                            ilike("nickname", "%$sanitizedQuery%")
                        }
                    }
                    limit(limit.toLong())
                }.decodeList<JsonObject>()

                val mappedResult = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        extractJsonValue(value)
                    }
                }

                android.util.Log.d(TAG, "Found ${mappedResult.size} users matching query")
                Result.success(mappedResult)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "User search failed", e)
                Result.failure(e)
            }
        }
    }



    suspend fun searchMedia(
        query: String,
        mediaType: com.synapse.social.studioasinc.domain.model.SearchResult.MediaType? = null,
        limit: Int = 20
    ): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "Searching media with query: $query, type: $mediaType")

                val sanitizedQuery = sanitizeSearchQuery(query)

                val result = client.from("posts").select(columns = Columns.raw("*")) {
                    filter {

                        if (sanitizedQuery.isNotEmpty()) {
                            ilike("post_text", "%$sanitizedQuery%")
                        }


                        when (mediaType) {
                            com.synapse.social.studioasinc.domain.model.SearchResult.MediaType.PHOTO -> {

                                or {




                                    neq("post_image", "null")

                                }
                            }
                            com.synapse.social.studioasinc.domain.model.SearchResult.MediaType.VIDEO -> {


                                eq("post_type", "VIDEO")
                            }
                            null -> {

                                or {
                                    neq("post_image", "null")
                                    eq("post_type", "VIDEO")
                                    eq("post_type", "IMAGE")
                                }
                            }
                        }
                    }
                    limit(limit.toLong())
                    order(column = "timestamp", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }.decodeList<JsonObject>()

                val mappedResult = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        extractJsonValue(value)
                    }
                }

                android.util.Log.d(TAG, "Found ${mappedResult.size} media items matching query")
                Result.success(mappedResult)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Media search failed", e)
                Result.failure(e)
            }
        }
    }



    private fun sanitizeSearchQuery(query: String): String {
        return query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
            .trim()
            .take(100)
    }
}
