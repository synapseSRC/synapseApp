package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedGroupField
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.crypto.models.PreKeyBundle
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.local.SecureStorage
import com.synapse.social.studioasinc.shared.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Encrypts / decrypts group metadata fields (name, description, avatar url) using the editor's
 * existing 1:1 Signal sessions, mirroring the message fan-out performed by [ChatEncryptionHelper].
 *
 * Each field is stored as an [EncryptedGroupField]: one [EncryptedMessage] envelope per recipient
 * uid plus the `editorId` so a recipient can decrypt with `decryptMessage(senderId = editorId, ...)`.
 *
 * Graceful degradation: when [signalProtocolManager] is null, or encryption / decryption fails, the
 * caller falls back to the legacy plaintext columns. Nothing here throws for a missing session.
 *
 * Replay safety: a PreKey envelope can only be decrypted once (its one-time pre-key is consumed on
 * receipt), so successfully decrypted values are persisted as plaintext through [secureStorage]
 * (at-rest encrypted on every platform) and served from that cache on subsequent reads.
 */
internal class GroupMetadataCipher(
    private val signalProtocolManager: SignalProtocolManager?,
    private val dataSource: SupabaseChatDataSource,
    private val secureStorage: SecureStorage? = null
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cryptoMutex = Mutex()
    private val cacheSerializer = MapSerializer(String.serializer(), CachedMetaField.serializer())

    /** True when end-to-end encryption is available for group metadata. */
    val isAvailable: Boolean get() = signalProtocolManager != null

    @Serializable
    private data class CachedMetaField(val fingerprint: String = "", val value: String = "")

    /**
     * Fan-out encrypts [plaintext] for every participant (plus the editor, for multi-device parity).
     * Returns null when [plaintext] is null, encryption is unavailable, or no envelope could be built.
     */
    suspend fun encryptField(
        plaintext: String?,
        participantIds: List<String>,
        currentUserId: String
    ): EncryptedGroupField? {
        if (plaintext == null) return null
        val manager = signalProtocolManager ?: return null
        val recipients = (participantIds + currentUserId).filter { it.isNotBlank() }.distinct()
        if (recipients.isEmpty()) return null

        val bytes = plaintext.encodeToByteArray()
        val envelopes = mutableMapOf<String, EncryptedMessage>()
        for (recipient in recipients) {
            try {
                cryptoMutex.withLock {
                    // Force a fresh PreKey (type 3) envelope for EVERY recipient, then drop the session
                    // again after encrypting. Two reasons:
                    //  1. A member who joined at creation has not run X3DH yet, so every metadata field
                    //     must be an independently-decryptable PreKey envelope (not an order-dependent
                    //     Whisper built on a session only the editor has).
                    //  2. Leaving a session behind here would make the creator's first chat message a
                    //     Whisper that the member cannot bootstrap — the "Couldn't decrypt message #1" bug.
                    //     Clearing it lets the message fan-out start a clean X3DH session instead.
                    manager.deleteSession(recipient)
                    ensureSession(recipient)
                    envelopes[recipient] = manager.encryptMessage(recipient, bytes)
                    manager.deleteSession(recipient)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w("E2EE_GROUP: Failed to encrypt metadata for $recipient: ${e.message}", tag = "E2EE")
            }
        }
        if (envelopes.isEmpty()) return null
        return EncryptedGroupField(editorId = currentUserId, envelopes = envelopes)
    }

    /**
     * Decrypts the envelope addressed to [currentUserId] using the recorded [EncryptedGroupField.editorId]
     * as the sender. Returns null when there is no envelope for this user or decryption fails.
     *
     * When [chatId] and [fieldKey] are provided (and a [secureStorage] is wired), results are cached as
     * plaintext keyed by the recipient envelope body, so repeat reads never re-attempt the one-shot
     * PreKey decrypt. A stale cached value is returned if a re-decrypt fails.
     */
    suspend fun decryptField(
        field: EncryptedGroupField?,
        currentUserId: String,
        chatId: String? = null,
        fieldKey: String? = null
    ): String? {
        if (field == null) return null
        val manager = signalProtocolManager ?: return null
        val envelope = field.envelopes[currentUserId] ?: return null

        val cacheable = secureStorage != null && chatId != null && fieldKey != null
        val stale = if (cacheable) readCache(chatId!!)[fieldKey!!] else null
        if (stale != null && stale.fingerprint == envelope.body && stale.value.isNotEmpty()) {
            return stale.value
        }

        val plaintext = try {
            cryptoMutex.withLock {
                try {
                    if (field.editorId != currentUserId && !manager.hasSession(field.editorId)) {
                        try {
                            ensureSession(field.editorId)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            Logger.w("E2EE_GROUP: No session with editor ${field.editorId}, attempting direct decrypt", tag = "E2EE")
                        }
                    }
                    decryptWithSessionReset(manager, field.editorId, envelope)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e("E2EE_GROUP: Failed to decrypt metadata from ${field.editorId}", tag = "E2EE", throwable = e)
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        }

        if (plaintext != null) {
            if (cacheable) writeCache(chatId!!, fieldKey!!, envelope.body, plaintext)
            return plaintext
        }
        return stale?.value?.takeIf { it.isNotEmpty() }
    }

    /**
     * Decrypts [envelope] from [editorId]; on failure of a PreKey-type envelope, drops the stored session
     * and retries. The existing session was often created by *us* when fan-out encrypting for the editor
     * (always the case for the editor's own self envelope) — libsignal cannot decrypt a PreKey message
     * using a send-side session, but decrypts it via the receiving path once no session exists.
     */
    private suspend fun decryptWithSessionReset(
        manager: SignalProtocolManager,
        editorId: String,
        envelope: EncryptedMessage
    ): String {
        return try {
            manager.decryptMessage(senderId = editorId, message = envelope).decodeToString()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Only PreKey envelopes (CiphertextMessage.PREKEY_TYPE) can be re-processed from scratch.
            if (envelope.type != PREKEY_TYPE) throw e
            manager.deleteSession(editorId)
            manager.decryptMessage(senderId = editorId, message = envelope).decodeToString()
        }
    }

    private fun cacheKeyFor(chatId: String): String = "e2ee_group_meta_$chatId"

    private fun readCache(chatId: String): Map<String, CachedMetaField> {
        val raw = secureStorage?.getString(cacheKeyFor(chatId)) ?: return emptyMap()
        return try {
            json.decodeFromString(cacheSerializer, raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun writeCache(chatId: String, fieldKey: String, fingerprint: String, value: String) {
        val storage = secureStorage ?: return
        try {
            val current = readCache(chatId).toMutableMap()
            current[fieldKey] = CachedMetaField(fingerprint = fingerprint, value = value)
            storage.save(cacheKeyFor(chatId), json.encodeToString(cacheSerializer, current))
        } catch (e: Exception) {
            Logger.w("E2EE_GROUP: Failed to persist metadata cache: ${e.message}", tag = "E2EE")
        }
    }

    /** Serializes an [EncryptedGroupField] to a JSON string suitable for a Postgres jsonb parameter. */
    fun serialize(field: EncryptedGroupField): String = json.encodeToString(EncryptedGroupField.serializer(), field)

    /** Parses a jsonb [JsonElement] (from a ChatDto) into an [EncryptedGroupField]. */
    fun parse(element: JsonElement?): EncryptedGroupField? {
        if (element == null) return null
        return try {
            json.decodeFromJsonElement(EncryptedGroupField.serializer(), element)
        } catch (e: Exception) {
            Logger.w("E2EE_GROUP: Failed to parse encrypted field element: ${e.message}", tag = "E2EE")
            null
        }
    }

    /** Parses a jsonb text column (from the inbox summary RPC) into an [EncryptedGroupField]. */
    fun parse(raw: String?): EncryptedGroupField? {
        if (raw.isNullOrBlank()) return null
        return try {
            json.decodeFromString(EncryptedGroupField.serializer(), raw)
        } catch (e: Exception) {
            Logger.w("E2EE_GROUP: Failed to parse encrypted field string: ${e.message}", tag = "E2EE")
            null
        }
    }

    private suspend fun ensureSession(userId: String) {
        val manager = signalProtocolManager ?: return
        if (!manager.hasSession(userId)) {
            val keyDto = dataSource.getUserPublicKey(userId)
            if (keyDto != null) {
                val bundle = json.decodeFromString<PreKeyBundle>(keyDto.publicKey)
                manager.processPreKeyBundle(userId, bundle)
            } else {
                throw Exception("Recipient hasn't enabled E2EE")
            }
        }
    }

    private companion object {
        /** libsignal `CiphertextMessage.PREKEY_TYPE` — an X3DH-based first message of a session. */
        const val PREKEY_TYPE = 3
    }
}
