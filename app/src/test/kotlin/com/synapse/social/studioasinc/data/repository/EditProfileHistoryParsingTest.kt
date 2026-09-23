package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.shared.domain.usecase.UploadMediaUseCase
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock

class EditProfileHistoryParsingTest {

    private val uploadMediaUseCase = mock(UploadMediaUseCase::class.java)
    private val repository = EditProfileRepositoryImpl(mock(android.content.Context::class.java), uploadMediaUseCase)

    @Test
    fun `parseProfileHistoryItem correctly parses schema compliant json`() {
        val json = buildJsonObject {
            put("id", "hist-123")
            put("user_id", "user-456")
            put("avatar", "https://example.com/avatar.jpg")
            put("created_at", "2025-01-01T12:00:00Z")
        }

        val item = repository.parseProfileHistoryItem(json)

        assertNotNull(item)
        assertEquals("hist-123", item?.id)
        assertEquals("user-456", item?.userId)
        assertEquals("https://example.com/avatar.jpg", item?.imageUrl)
        assertEquals("2025-01-01T12:00:00Z", item?.createdAt)
    }

    @Test
    fun `parseProfileHistoryItem returns null when avatar or id is missing`() {
        val jsonWithoutAvatar = buildJsonObject {
            put("id", "hist-123")
            put("user_id", "user-456")
        }

        val item = repository.parseProfileHistoryItem(jsonWithoutAvatar)
        assertNull(item)
    }

    @Test
    fun `parseCoverHistoryItem correctly parses schema compliant json`() {
        val json = buildJsonObject {
            put("id", "cover-123")
            put("user_id", "user-456")
            put("cover_image_url", "https://example.com/cover.jpg")
            put("created_at", "2025-01-02T12:00:00Z")
        }

        val item = repository.parseCoverHistoryItem(json)

        assertNotNull(item)
        assertEquals("cover-123", item?.id)
        assertEquals("user-456", item?.userId)
        assertEquals("https://example.com/cover.jpg", item?.imageUrl)
        assertEquals("2025-01-02T12:00:00Z", item?.createdAt)
    }

    @Test
    fun `parseCoverHistoryItem returns null when cover_image_url is missing`() {
        val jsonWithoutCover = buildJsonObject {
            put("id", "cover-123")
            put("user_id", "user-456")
        }

        val item = repository.parseCoverHistoryItem(jsonWithoutCover)
        assertNull(item)
    }
}
