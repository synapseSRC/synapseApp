package com.synapse.social.studioasinc.shared.data.datasource

import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChatMessageDataSourceTest {
    @Test
    fun testMarkMessagesAsDeliveredThrowsWhenUserIsNotAuthenticated() = runTest {
        val dummyClient = createSupabaseClient(
            supabaseUrl = "https://placeholder.supabase.co",
            supabaseKey = "placeholder-anon-key"
        ) {}
        val dataSource = ChatMessageDataSource(dummyClient)

        assertFailsWith<NotAuthenticatedException> {
            dataSource.markMessagesAsDelivered("chat_123")
        }
    }

    @Test
    fun filterOutUserDeletedMessagesHidesOnlyIdsDeletedByCurrentUser() {
        val messages = listOf(
            MessageDto(id = "hidden-message", content = "hidden content"),
            MessageDto(id = "visible-message", content = "visible content"),
            MessageDto(id = null, content = "message without id")
        )

        val visibleMessages = filterOutUserDeletedMessages(
            messages = messages,
            deletedMessageIds = setOf("hidden-message")
        )

        assertEquals(listOf("visible-message", null), visibleMessages.map { it.id })
    }
}
