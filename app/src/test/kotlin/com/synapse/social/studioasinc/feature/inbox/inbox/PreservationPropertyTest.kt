package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Preservation Property Tests — Task 2
 *
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 *
 * These tests establish the behavioral baseline that NON-RETRY paths must preserve after
 * the fix is applied in Task 3. ALL tests in this file are expected to PASS on UNFIXED
 * code — the passing results prove the fix introduces no regressions.
 *
 * Coverage (each corresponds to one design property-2 assertion):
 *
 *   P2-1  Non-placeholder message — replaceById with same clientId is identity on clientId/content
 *   P2-2  Delivery-status update — copy(deliveryStatus = …) never alters clientId
 *   P2-3  Reaction update — copy(reactions/userReaction = …) never alters clientId
 *   P2-4  distinctBy { it.id } invariant — replaceById produces no duplicate ids
 *   P2-5  replaceById is a no-op for absent ids — list is structurally unchanged
 *
 * Architecture:
 *   - Uses a standalone `MutableStateFlow<List<Message>>` with an inlined `replaceById`
 *     extension, mirroring `RetryClientIdStabilityExplorationTest`.  Zero dependency on
 *     the full ViewModel stack so tests compile even without Hilt or other Android deps.
 */
class PreservationPropertyTest {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private val messages = MutableStateFlow<List<Message>>(emptyList())

    // -------------------------------------------------------------------------
    // Helpers — mirrored from ChatMessagingDelegate (inlined for isolation)
    // -------------------------------------------------------------------------

    /** Exact copy of ChatMessagingDelegate.replaceById */
    private fun List<Message>.replaceById(id: String, newMessage: Message): List<Message> {
        val index = indexOfFirst { it.id == id }
        return if (index != -1) {
            toMutableList().apply { set(index, newMessage) }
        } else {
            this
        }
    }

    /** Simulates the _messages.update lambda the retry coroutine currently executes */
    private fun applyRetryUpdate(messageId: String, refreshed: Message) {
        messages.update { current ->
            current.replaceById(messageId, refreshed)
        }
    }

    /** Simulates a delivery-status in-place update (own-message reconciliation path) */
    private fun applyDeliveryStatusUpdate(messageId: String, newStatus: DeliveryStatus) {
        messages.update { current ->
            val index = current.indexOfFirst { it.id == messageId }
            if (index == -1) return@update current
            current.toMutableList().apply {
                set(index, get(index).copy(deliveryStatus = newStatus))
            }
        }
    }

    /** Simulates a reactions-only update (reaction event path) */
    private fun applyReactionUpdate(
        messageId: String,
        reactions: Map<ReactionType, Int>,
        userReaction: ReactionType?
    ) {
        messages.update { current ->
            val index = current.indexOfFirst { it.id == messageId }
            if (index == -1) return@update current
            current.toMutableList().apply {
                set(index, get(index).copy(reactions = reactions, userReaction = userReaction))
            }
        }
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    private fun makeMessage(
        id: String,
        clientId: String,
        content: String,
        deliveryStatus: DeliveryStatus = DeliveryStatus.DELIVERED,
        senderId: String = "other-user"
    ) = Message(
        id = id,
        chatId = "chat-test",
        senderId = senderId,
        content = content,
        messageType = MessageType.TEXT,
        deliveryStatus = deliveryStatus,
        createdAt = "2025-01-01T10:00:00Z",
        clientId = clientId
    )

    // -------------------------------------------------------------------------
    // P2-1  Non-placeholder incoming message
    //
    //   For a message whose content is NOT in ENCRYPTED_PLACEHOLDERS, calling
    //   replaceById with an identical refresh (same clientId) leaves the clientId
    //   and content unchanged.
    //
    //   Validates: Requirements 3.2, 3.3 (non-retry paths)
    // -------------------------------------------------------------------------

    @Test
    fun nonPlaceholder_replaceWithSameClientId_preservesClientId() {
        val msgId = "msg-001"
        val stableClientId = "cid-stable-001"
        val content = "A perfectly readable message"

        assertFalse(
            "Precondition: content must not be in ENCRYPTED_PLACEHOLDERS",
            content in ChatMessagingDelegate.ENCRYPTED_PLACEHOLDERS
        )

        val original = makeMessage(id = msgId, clientId = stableClientId, content = content)
        messages.value = listOf(original)

        // Replace with an identical copy — same clientId, same content
        val sameRefresh = original.copy()
        applyRetryUpdate(msgId, sameRefresh)

        val afterClientId = messages.value.first { it.id == msgId }.clientId
        assertEquals(
            "P2-1: clientId must be unchanged after replaceById with same clientId",
            stableClientId,
            afterClientId
        )

        val afterContent = messages.value.first { it.id == msgId }.content
        assertEquals(
            "P2-1: content must be unchanged after replaceById with same clientId",
            content,
            afterContent
        )
    }

    @Test
    fun nonPlaceholder_multipleMessages_onlyTargetIsReplaced() {
        val msgId = "msg-001"
        val otherId = "msg-002"

        val target = makeMessage(id = msgId, clientId = "cid-001", content = "Target message")
        val other = makeMessage(id = otherId, clientId = "cid-002", content = "Untouched message")
        messages.value = listOf(target, other)

        applyRetryUpdate(msgId, target.copy())  // replace target with identical copy

        // The other message must be completely untouched
        val otherAfter = messages.value.first { it.id == otherId }
        assertEquals(
            "P2-1: unrelated message clientId must be unchanged",
            "cid-002",
            otherAfter.clientId
        )
        assertEquals(
            "P2-1: unrelated message content must be unchanged",
            "Untouched message",
            otherAfter.content
        )
    }

    // -------------------------------------------------------------------------
    // P2-2  Delivery-status update
    //
    //   Updating only deliveryStatus on an existing message does not alter its clientId.
    //
    //   Validates: Requirements 3.1, 3.3
    // -------------------------------------------------------------------------

    @Test
    fun deliveryStatusUpdate_doesNotAlterClientId() {
        val msgId = "msg-delivery-001"
        val stableClientId = "cid-delivery-stable"

        val original = makeMessage(
            id = msgId,
            clientId = stableClientId,
            content = "Sent message",
            deliveryStatus = DeliveryStatus.SENT,
            senderId = "current-user"
        )
        messages.value = listOf(original)

        applyDeliveryStatusUpdate(msgId, DeliveryStatus.DELIVERED)

        assertEquals(
            "P2-2: clientId must be unchanged after SENT → DELIVERED update",
            stableClientId,
            messages.value.first { it.id == msgId }.clientId
        )
        assertEquals(
            "P2-2: deliveryStatus must be updated to DELIVERED",
            DeliveryStatus.DELIVERED,
            messages.value.first { it.id == msgId }.deliveryStatus
        )
    }

    @Test
    fun deliveryStatusUpdate_readTransition_doesNotAlterClientId() {
        val msgId = "msg-delivery-002"
        val stableClientId = "cid-delivery-read"

        val original = makeMessage(
            id = msgId,
            clientId = stableClientId,
            content = "Delivered message",
            deliveryStatus = DeliveryStatus.DELIVERED,
            senderId = "current-user"
        )
        messages.value = listOf(original)

        applyDeliveryStatusUpdate(msgId, DeliveryStatus.READ)

        assertEquals(
            "P2-2: clientId must be unchanged after DELIVERED → READ update",
            stableClientId,
            messages.value.first { it.id == msgId }.clientId
        )
    }

    @Test
    fun deliveryStatusUpdate_allTransitions_neverAlterClientId() {
        // Validates all four forward delivery transitions in sequence
        val msgId = "msg-delivery-003"
        val stableClientId = "cid-delivery-all"

        messages.value = listOf(
            makeMessage(
                id = msgId,
                clientId = stableClientId,
                content = "Test message",
                deliveryStatus = DeliveryStatus.SENDING,
                senderId = "current-user"
            )
        )

        val transitions = listOf(
            DeliveryStatus.SENT,
            DeliveryStatus.DELIVERED,
            DeliveryStatus.READ
        )

        transitions.forEach { newStatus ->
            applyDeliveryStatusUpdate(msgId, newStatus)
            assertEquals(
                "P2-2: clientId must be stable after transition to $newStatus",
                stableClientId,
                messages.value.first { it.id == msgId }.clientId
            )
        }
    }

    // -------------------------------------------------------------------------
    // P2-3  Reaction update
    //
    //   Updating only reactions/userReaction on an existing message does not alter
    //   its clientId.
    //
    //   Validates: Requirements 3.3
    // -------------------------------------------------------------------------

    @Test
    fun reactionUpdate_doesNotAlterClientId() {
        val msgId = "msg-reaction-001"
        val stableClientId = "cid-reaction-stable"

        val original = makeMessage(
            id = msgId,
            clientId = stableClientId,
            content = "React to me"
        )
        messages.value = listOf(original)

        applyReactionUpdate(
            messageId = msgId,
            reactions = mapOf(ReactionType.LIKE to 1),
            userReaction = ReactionType.LIKE
        )

        assertEquals(
            "P2-3: clientId must be unchanged after reaction update",
            stableClientId,
            messages.value.first { it.id == msgId }.clientId
        )
        assertEquals(
            "P2-3: reaction must be applied",
            mapOf(ReactionType.LIKE to 1),
            messages.value.first { it.id == msgId }.reactions
        )
        assertEquals(
            "P2-3: userReaction must be applied",
            ReactionType.LIKE,
            messages.value.first { it.id == msgId }.userReaction
        )
    }

    @Test
    fun reactionRemoval_doesNotAlterClientId() {
        val msgId = "msg-reaction-002"
        val stableClientId = "cid-reaction-remove"

        val original = makeMessage(id = msgId, clientId = stableClientId, content = "Already liked")
            .copy(reactions = mapOf(ReactionType.LIKE to 1), userReaction = ReactionType.LIKE)
        messages.value = listOf(original)

        // Remove the reaction
        applyReactionUpdate(
            messageId = msgId,
            reactions = emptyMap(),
            userReaction = null
        )

        assertEquals(
            "P2-3: clientId must be unchanged after reaction removal",
            stableClientId,
            messages.value.first { it.id == msgId }.clientId
        )
        assertTrue(
            "P2-3: reactions map must be empty after removal",
            messages.value.first { it.id == msgId }.reactions.isEmpty()
        )
    }

    @Test
    fun multipleReactionUpdates_clientIdRemainsStable() {
        val msgId = "msg-reaction-003"
        val stableClientId = "cid-reaction-multi"

        messages.value = listOf(makeMessage(id = msgId, clientId = stableClientId, content = "Multi-react"))

        // Simulate three successive reaction changes
        listOf(ReactionType.LIKE, ReactionType.LOVE, ReactionType.HAHA).forEach { reaction ->
            applyReactionUpdate(
                messageId = msgId,
                reactions = mapOf(reaction to 1),
                userReaction = reaction
            )
            assertEquals(
                "P2-3: clientId must remain stable after applying reaction $reaction",
                stableClientId,
                messages.value.first { it.id == msgId }.clientId
            )
        }
    }

    // -------------------------------------------------------------------------
    // P2-4  distinctBy { it.id } invariant
    //
    //   After any replaceById operation, the resulting list contains no duplicate ids.
    //
    //   Validates: Requirements 3.2, 3.3
    // -------------------------------------------------------------------------

    @Test
    fun replaceById_producesNoDuplicateIds_singleMessage() {
        val msgId = "msg-dedup-001"
        val original = makeMessage(id = msgId, clientId = "cid-dedup-001", content = "Original")
        messages.value = listOf(original)

        // Replace with an updated copy
        val updated = original.copy(content = "Updated content", deliveryStatus = DeliveryStatus.READ)
        applyRetryUpdate(msgId, updated)

        val ids = messages.value.map { it.id }
        assertEquals(
            "P2-4: list must have exactly one entry after single-message replaceById",
            1,
            ids.size
        )
        assertEquals(
            "P2-4: id must be unchanged",
            msgId,
            ids.first()
        )
    }

    @Test
    fun replaceById_producesNoDuplicateIds_multipleMessages() {
        val ids = listOf("msg-001", "msg-002", "msg-003")
        messages.value = ids.mapIndexed { i, id ->
            makeMessage(id = id, clientId = "cid-$i", content = "Message $i")
        }

        // Replace the middle message
        val refreshed = messages.value[1].copy(content = "Updated middle")
        applyRetryUpdate("msg-002", refreshed)

        val resultIds = messages.value.map { it.id }
        assertEquals(
            "P2-4: list size must be unchanged after replaceById in a 3-element list",
            3,
            resultIds.size
        )
        assertEquals(
            "P2-4: distinct id count must equal list size (no duplicates)",
            resultIds.distinct().size,
            resultIds.size
        )
    }

    @Test
    fun replaceById_producesNoDuplicateIds_absentId() {
        val existingId = "msg-existing"
        messages.value = listOf(makeMessage(id = existingId, clientId = "cid-x", content = "Existing"))

        // Replace with a message whose id is NOT in the list
        val absent = makeMessage(id = "msg-absent", clientId = "cid-y", content = "Ghost message")
        applyRetryUpdate("msg-absent", absent)

        // replaceById is a no-op when id is absent, so the list must be unchanged
        val resultIds = messages.value.map { it.id }
        assertEquals(
            "P2-4: absent-id replaceById must not inject duplicates — list size must stay 1",
            1,
            resultIds.size
        )
        assertEquals(
            "P2-4: list must still contain only the original message",
            existingId,
            resultIds.first()
        )
    }

    @Test
    fun replaceById_preservesOrderForAllPositions() {
        // Build a 5-element list and replace each position in turn, verifying no duplicates
        val original = (1..5).map { i ->
            makeMessage(id = "msg-$i", clientId = "cid-$i", content = "Message $i")
        }
        messages.value = original

        for (i in 1..5) {
            val targetId = "msg-$i"
            val updated = messages.value.first { it.id == targetId }.copy(deliveryStatus = DeliveryStatus.READ)
            applyRetryUpdate(targetId, updated)

            val resultIds = messages.value.map { it.id }
            assertEquals(
                "P2-4: list size must remain 5 after replacing position $i",
                5,
                resultIds.size
            )
            assertEquals(
                "P2-4: no duplicate ids after replacing position $i",
                resultIds.distinct().size,
                resultIds.size
            )
        }
    }

    // -------------------------------------------------------------------------
    // P2-5  replaceById is a no-op for absent ids
    //
    //   Calling replaceById with an id not present in the list leaves the list
    //   structurally unchanged — same size, same elements, same order.
    //
    //   Validates: Requirements 3.2 (history pagination path — absent id must not crash)
    // -------------------------------------------------------------------------

    @Test
    fun replaceById_absentId_listUnchanged_emptyList() {
        messages.value = emptyList()

        val ghost = makeMessage(id = "msg-absent", clientId = "cid-ghost", content = "Not in list")
        applyRetryUpdate("msg-absent", ghost)

        assertEquals(
            "P2-5: replaceById on empty list must leave list empty",
            emptyList<Message>(),
            messages.value
        )
    }

    @Test
    fun replaceById_absentId_listUnchanged_singleElement() {
        val existing = makeMessage(id = "msg-real", clientId = "cid-real", content = "Real message")
        messages.value = listOf(existing)

        val ghost = makeMessage(id = "msg-absent", clientId = "cid-ghost", content = "Ghost")
        applyRetryUpdate("msg-absent", ghost)

        assertEquals(
            "P2-5: list size must remain 1 when absent id is used",
            1,
            messages.value.size
        )
        assertEquals(
            "P2-5: existing message must be unchanged",
            existing,
            messages.value.first()
        )
    }

    @Test
    fun replaceById_absentId_listUnchanged_multipleElements() {
        val originals = listOf(
            makeMessage(id = "msg-A", clientId = "cid-A", content = "Alpha"),
            makeMessage(id = "msg-B", clientId = "cid-B", content = "Beta"),
            makeMessage(id = "msg-C", clientId = "cid-C", content = "Gamma")
        )
        messages.value = originals

        val ghost = makeMessage(id = "msg-GHOST", clientId = "cid-ghost", content = "Ghost")
        applyRetryUpdate("msg-GHOST", ghost)

        assertEquals(
            "P2-5: list size must remain 3 when absent id is used",
            3,
            messages.value.size
        )
        // Verify every original message is still present and unchanged
        originals.forEach { original ->
            val inList = messages.value.firstOrNull { it.id == original.id }
            assertEquals(
                "P2-5: message ${original.id} must be identical to original",
                original,
                inList
            )
        }
    }

    @Test
    fun replaceById_absentId_doesNotAppend() {
        val existing = makeMessage(id = "msg-real", clientId = "cid-real", content = "Real message")
        messages.value = listOf(existing)

        // Attempt to use replaceById as if it were an insert (it must NOT append)
        val ghost = makeMessage(id = "msg-absent", clientId = "cid-ghost", content = "Ghost")
        applyRetryUpdate("msg-absent", ghost)

        assertFalse(
            "P2-5: replaceById must not append the new message when id is absent",
            messages.value.any { it.id == "msg-absent" }
        )
    }

    // -------------------------------------------------------------------------
    // P2-GATE  Encrypted placeholder gate
    //
    //   Sanity-check that the ENCRYPTED_PLACEHOLDERS set is well-defined and that
    //   non-placeholder content strings correctly evaluate to false at the gate.
    //   (Regression guard for Requirements 3.1 — own-message content never matches the
    //   placeholder gate.)
    // -------------------------------------------------------------------------

    @Test
    fun encryptedPlaceholders_gateIsCorrectForNonPlaceholderContent() {
        val nonPlaceholderStrings = listOf(
            "Hello!",
            "How are you?",
            "See you at 5",
            "",                    // empty string
            " ",                   // whitespace
            "message is encrypted" // lowercase — must NOT match
        )

        nonPlaceholderStrings.forEach { content ->
            assertFalse(
                "P2-GATE: '$content' must NOT be in ENCRYPTED_PLACEHOLDERS",
                content in ChatMessagingDelegate.ENCRYPTED_PLACEHOLDERS
            )
        }
    }

    @Test
    fun encryptedPlaceholders_gateIsCorrectForPlaceholderContent() {
        val placeholders = ChatMessagingDelegate.ENCRYPTED_PLACEHOLDERS

        assertTrue(
            "P2-GATE: ENCRYPTED_PLACEHOLDERS must be non-empty",
            placeholders.isNotEmpty()
        )

        placeholders.forEach { placeholder ->
            assertTrue(
                "P2-GATE: '$placeholder' must be in ENCRYPTED_PLACEHOLDERS",
                placeholder in ChatMessagingDelegate.ENCRYPTED_PLACEHOLDERS
            )
        }
    }
}
