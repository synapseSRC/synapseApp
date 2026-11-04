package com.synapse.social.studioasinc.shared.data.crypto.models

import kotlinx.serialization.Serializable

/**
 * End-to-end encrypted container for a single group metadata field (name, description or avatar url).
 *
 * The plaintext value is fan-out encrypted with the editor's existing 1:1 Signal sessions, one
 * [EncryptedMessage] envelope per recipient uid. [editorId] records who produced the envelopes so a
 * recipient can decrypt using `decryptMessage(senderId = editorId, ...)`.
 *
 * Stored server-side in the `chats.enc_name` / `enc_description` / `enc_avatar_url` jsonb columns.
 */
@Serializable
data class EncryptedGroupField(
    val editorId: String,
    val envelopes: Map<String, EncryptedMessage>
)
