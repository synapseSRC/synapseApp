package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.database.StoryEntity as DbStory
import com.synapse.social.studioasinc.shared.domain.model.Story
import com.synapse.social.studioasinc.shared.domain.model.StoryMediaType
import com.synapse.social.studioasinc.shared.domain.model.StoryPrivacy
import kotlinx.coroutines.withContext

class SqlDelightStoryDao(
    private val db: StorageDatabase
) : StoryDao {

    override suspend fun insert(story: Story): Unit = withContext(AppDispatchers.IO) {
        db.storyQueries.insertStory(toDb(story))
    }

    override suspend fun insertAll(stories: List<Story>) = withContext(AppDispatchers.IO) {
        db.transaction {
            stories.forEach { story ->
                db.storyQueries.insertStory(toDb(story))
            }
        }
    }

    override suspend fun getAll(): List<Story> = withContext(AppDispatchers.IO) {
        db.storyQueries.selectAll().executeAsList().map { toDomain(it) }
    }

    override suspend fun deleteById(id: String): Unit = withContext(AppDispatchers.IO) {
        db.storyQueries.deleteById(id)
    }

    override suspend fun deleteAll(): Unit = withContext(AppDispatchers.IO) {
        db.storyQueries.deleteAll()
    }

    private fun toDb(story: Story): DbStory {
        return DbStory(
            id = story.id ?: "",
            userId = story.userId,
            mediaUrl = story.mediaUrl,
            mediaType = story.mediaType?.name,
            content = story.content,
            duration = story.duration?.toLong(),
            durationHours = story.durationHours?.toLong(),
            privacy = story.privacy?.name,
            viewCount = story.viewCount?.toLong(),
            isActive = story.isActive,
            thumbnailUrl = story.thumbnailUrl,
            mediaWidth = story.mediaWidth?.toLong(),
            mediaHeight = story.mediaHeight?.toLong(),
            mediaDurationSeconds = story.mediaDurationSeconds?.toLong(),
            fileSizeBytes = story.fileSizeBytes,
            reactionsCount = story.reactionsCount?.toLong(),
            repliesCount = story.repliesCount?.toLong(),
            isReported = story.isReported,
            moderationStatus = story.moderationStatus,
            createdAt = story.createdAt,
            expiresAt = story.expiresAt
        )
    }

    private fun toDomain(db: DbStory): Story {
        return Story(
            id = db.id,
            userId = db.userId,
            mediaUrl = db.mediaUrl,
            mediaType = db.mediaType?.let { type ->
                StoryMediaType.entries.find { it.name.equals(type, ignoreCase = true) }
            },
            content = db.content,
            duration = db.duration?.toInt(),
            durationHours = db.durationHours?.toInt(),
            privacy = db.privacy?.let { p ->
                StoryPrivacy.entries.find { it.name.equals(p, ignoreCase = true) }
            },
            viewCount = db.viewCount?.toInt(),
            isActive = db.isActive,
            thumbnailUrl = db.thumbnailUrl,
            mediaWidth = db.mediaWidth?.toInt(),
            mediaHeight = db.mediaHeight?.toInt(),
            mediaDurationSeconds = db.mediaDurationSeconds?.toInt(),
            fileSizeBytes = db.fileSizeBytes,
            reactionsCount = db.reactionsCount?.toInt(),
            repliesCount = db.repliesCount?.toInt(),
            isReported = db.isReported,
            moderationStatus = db.moderationStatus,
            createdAt = db.createdAt,
            expiresAt = db.expiresAt
        )
    }
}
