package com.synapse.social.studioasinc.shared.data.database

import app.cash.sqldelight.ColumnAdapter
import com.synapse.social.studioasinc.shared.domain.model.MediaItem
import com.synapse.social.studioasinc.shared.domain.model.PollOption
import com.synapse.social.studioasinc.shared.domain.model.PostMetadata
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val mediaItemListAdapter = object : ColumnAdapter<List<MediaItem>, String> {
    override fun decode(databaseValue: String): List<MediaItem> {
        return if (databaseValue.isBlank()) emptyList() else try {
            Json.decodeFromString<List<MediaItem>>(databaseValue)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun encode(value: List<MediaItem>): String {
        return Json.encodeToString(value)
    }
}

val pollOptionListAdapter = object : ColumnAdapter<List<PollOption>, String> {
    override fun decode(databaseValue: String): List<PollOption> {
        return if (databaseValue.isBlank()) emptyList() else try {
            Json.decodeFromString<List<PollOption>>(databaseValue)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun encode(value: List<PollOption>): String {
        return Json.encodeToString(value)
    }
}

val reactionMapAdapter = object : ColumnAdapter<Map<ReactionType, Int>, String> {
    override fun decode(databaseValue: String): Map<ReactionType, Int> {
        return if (databaseValue.isBlank()) emptyMap() else try {
            Json.decodeFromString<Map<ReactionType, Int>>(databaseValue)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override fun encode(value: Map<ReactionType, Int>): String {
        return Json.encodeToString(value)
    }
}

val reactionTypeAdapter = object : ColumnAdapter<ReactionType, String> {
    override fun decode(databaseValue: String): ReactionType {
        return try {
            ReactionType.valueOf(databaseValue)
        } catch (e: Exception) {
            ReactionType.LIKE
        }
    }

    override fun encode(value: ReactionType): String {
        return value.name
    }
}

val postMetadataAdapter = object : ColumnAdapter<PostMetadata, String> {
    override fun decode(databaseValue: String): PostMetadata {
        return if (databaseValue.isBlank()) PostMetadata() else try {
            Json.decodeFromString<PostMetadata>(databaseValue)
        } catch (e: Exception) {
            PostMetadata()
        }
    }

    override fun encode(value: PostMetadata): String {
        return Json.encodeToString(value)
    }
}

val intAdapter = object : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long): Int = databaseValue.toInt()
    override fun encode(value: Int): Long = value.toLong()
}

val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long): Boolean = databaseValue == 1L
    override fun encode(value: Boolean): Long = if (value) 1L else 0L
}

val stringMapAdapter = object : ColumnAdapter<Map<String, String>, String> {
    override fun decode(databaseValue: String): Map<String, String> {
        return if (databaseValue.isBlank()) emptyMap() else try {
            Json.decodeFromString<Map<String, String>>(databaseValue)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override fun encode(value: Map<String, String>): String {
        return Json.encodeToString(value)
    }
}
