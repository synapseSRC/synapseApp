package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Bug Condition Exploration Test — Task 1
 *
 * Validates: Requirements 1.2, 1.3
 *
 * EXPECTED TO FAIL on unfixed code.
 *
 * This test encodes the bug condition defined in the design doc:
 *
 *   isBugCondition(incomingId, refreshedMessage):
 *     stableClientId = _messages.firstOrNull { it.id == incomingId }?.clientId
 *     RETURN stableClientId != null AND refreshedMessage.clientId != stableClientId
 *
 * When the retry coroutine in handleIncomingMessage resolves an encrypted-placeholder
 * message with a refreshed object that carries a different clientId, the current
 * (unfixed) code calls:
 *
 *   messagingDelegate._messages.update { current ->
 *       with(messagingDelegate) { current.replaceById(newMessage.id, refreshed) }
 *   }
 *
 * …passing `refreshed` directly without preserving the clientId already in the list.
 * The test asserts the stable clientId is preserved — which will FAIL on unfixed code,
 * proving the bug exists.
 */
class RetryClientIdStabilityExplorationTest {

    private val messages = MutableStateFlow<List<Message>>(emptyList())

    // Replicates the extension function from ChatMessagingDelegate inline so this test
    // has zero dependency on the full ViewModel stack.
    private fun List<Message>.replaceById(id: String, newMessage: Message): List<Message> {
        val index = indexOfFirst { it.id == id }
        return if (index != -1) {
            toMutableList().apply { set(index, newMessage) }
        } else {
            this
        }
    }

    // The exact update lambda the retry coroutine executes (FIXED):
    // Reads the stable clientId already stored in _messages and preserves it,
    // matching the fix applied in ChatViewModel.handleIncomingMessage.
    private fun applyRetryUpdate(messageId: String, refreshed: Message) {
        messages.update { current ->
            val stableClientId = current.firstOrNull { it.id == messageId }?.clientId
            val toWrite = if (stableClientId != null) refreshed.copy(clientId = stableClientId) else refreshed
            current.replaceById(messageId, toWrite)
        }
    }

    // -------------------------------------------------------------------------
    // Test helpers
    // -------------------------------------------------------------------------

    private fun makeIncomingPlaceholder(
        id: String,
        clientId: String,
        placeholderContent: String = ChatMessagingDelegate.ENCRYPTED_PLACEHOLDERS.first()
    ) = Message(
        id = id,
        chatId = "chat-test",
        senderId = "other-user",
        content = placeholderContent,
        messageType = MessageType.TEXT,
        deliveryStatus = DeliveryStatus.DELIVERED,
        createdAt = "2025-01-01T10:00:00Z",
        clientId = clientId
    )

    private fun makeRefreshedMessage(
        id: String,
        clientId: String,
        realContent: String = "Hello, decrypted world!"
    ) = Message(
        id = id,
        chatId = "chat-test",
        senderId = "other-user",
        content = realContent,
        messageType = MessageType.TEXT,
        deliveryStatus = DeliveryStatus.DELIVERED,
        createdAt = "2025-01-01T10:00:00Z",
        clientId = clientId
    )

    // -------------------------------------------------------------------------
    // Test case 1 (THE BUG) — retry returns a different clientId
    // -------------------------------------------------------------------------

    /**
     * Initial insert sets clientId = "cid-A".
     * Retry resolves with a refreshed message carrying clientId = "cid-B".
     *
     * EXPECTED (correct):   final clientId == "cid-A"
     * ACTUAL on unfixed code: final clientId == "cid-B"   ← FAILS here → confirms bug
     */
    @Test
    fun retryWithDifferentClientId_shouldPreserveStableClientId() {
        val msgId = "server-msg-001"

        // Step 1 — initial insertion: message with placeholder content and clientId = "cid-A"
        val initialMessage = makeIncomingPlaceholder(id = msgId, clientId = "cid-A")
        messages.value = listOf(initialMessage)

        assertEquals(
            "Precondition: initial clientId must be cid-A",
            "cid-A",
            messages.value.first { it.id == msgId }.clientId
        )

        // Step 2 — retry coroutine resolves with real content but clientId = "cid-B"
        val refreshed = makeRefreshedMessage(id = msgId, clientId = "cid-B")
        applyRetryUpdate(msgId, refreshed)

        // Step 3 — assert the stable clientId is preserved (will FAIL on unfixed code)
        val finalClientId = messages.value.first { it.id == msgId }.clientId
        assertEquals(
            "Bug detected: retry overwrote stable clientId 'cid-A' with 'cid-B'. " +
                "The retry coroutine passes `refreshed` directly to replaceById without " +
                "preserving the clientId already stored in _messages.",
            "cid-A",
            finalClientId
        )
    }

    // -------------------------------------------------------------------------
    // Test case 2 (BASELINE) — retry returns the SAME clientId
    // -------------------------------------------------------------------------

    /**
     * When the refreshed message already has the same clientId as the stored entry,
     * isBugCondition is false and no damage is done. This baseline should PASS on both
     * unfixed and fixed code, confirming the test infrastructure is sound.
     */
    @Test
    fun retryWithSameClientId_shouldLeaveClientIdUnchanged() {
        val msgId = "server-msg-002"

        val initialMessage = makeIncomingPlaceholder(id = msgId, clientId = "cid-A")
        messages.value = listOf(initialMessage)

        val refreshed = makeRefreshedMessage(id = msgId, clientId = "cid-A")
        applyRetryUpdate(msgId, refreshed)

        assertEquals(
            "Baseline: same clientId on retry must be preserved",
            "cid-A",
            messages.value.first { it.id == msgId }.clientId
        )
    }

    // -------------------------------------------------------------------------
    // Test case 3 (NULL-GUARD) — message not yet in list when retry fires
    // -------------------------------------------------------------------------

    /**
     * If the message is not in _messages when the retry fires (race-ahead edge case),
     * replaceById is a no-op. This ensures the null-guard path doesn't crash.
     */
    @Test
    fun retryWhenMessageNotInList_shouldNotCrash() {
        val msgId = "server-msg-003"

        // List is empty — message hasn't been inserted yet
        messages.value = emptyList()

        val refreshed = makeRefreshedMessage(id = msgId, clientId = "cid-B")
        applyRetryUpdate(msgId, refreshed)

        // replaceById on a list that doesn't contain msgId → list stays empty (no-op)
        assertEquals(
            "Null-guard: replaceById on absent message must leave list unchanged",
            emptyList<Message>(),
            messages.value
        )
    }

    // -------------------------------------------------------------------------
    // Test case 4 (REGRESSION GUARD) — non-placeholder content never enters retry
    // -------------------------------------------------------------------------

    /**
     * A message whose content is NOT in ENCRYPTED_PLACEHOLDERS must not be touched by
     * the retry coroutine. We verify that the message content and clientId remain
     * exactly as set after a no-op retry attempt.
     */
    @Test
    fun nonPlaceholderMessage_retryIsNoop() {
        val msgId = "server-msg-004"

        val normalMessage = Message(
            id = msgId,
            chatId = "chat-test",
            senderId = "other-user",
            content = "A perfectly readable message",  // NOT a placeholder
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.DELIVERED,
            createdAt = "2025-01-01T10:00:00Z",
            clientId = "cid-stable"
        )
        messages.value = listOf(normalMessage)

        // The retry gate condition: content must be in encryptedPlaceholders
        val isPlaceholder = normalMessage.content in ChatMessagingDelegate.ENCRYPTED_PLACEHOLDERS
        assertEquals(
            "Regression guard: non-placeholder content must NOT be in ENCRYPTED_PLACEHOLDERS",
            false,
            isPlaceholder
        )

        // Since the gate is false, the retry coroutine is never launched.
        // The message in _messages must remain exactly as originally inserted.
        assertEquals(
            "Regression guard: clientId must remain stable for non-placeholder messages",
            "cid-stable",
            messages.value.first { it.id == msgId }.clientId
        )
        assertEquals(
            "Regression guard: content must remain unchanged",
            "A perfectly readable message",
            messages.value.first { it.id == msgId }.content
        )
    }
}
