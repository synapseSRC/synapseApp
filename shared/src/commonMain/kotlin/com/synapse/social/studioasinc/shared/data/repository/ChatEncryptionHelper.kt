package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.crypto.models.PreKeyBundle
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.local.database.CachedMessageDao
import com.synapse.social.studioasinc.shared.util.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class ChatEncryptionHelper(
    private val signalProtocolManager: SignalProtocolManager?,
    private val dataSource: SupabaseChatDataSource,
    private val cachedMessageDao: CachedMessageDao?
) {

    // In-memory cache for decrypted content to avoid re-decryption failures
    // (especially for PreKeySignalMessages which consume the pre-key on first use)
    val decryptedMessageCache = mutableMapOf<String, String>()

    /**
     * Extracts content and mediaUrl from a decrypted JSON payload like {"content":"...","mediaUrl":"..."}.
     */
    private fun extractContent(jsonString: String): Pair<String, String?> {
        return try {
            val jsonPayload = Json.parseToJsonElement(jsonString).jsonObject
            val content = jsonPayload["content"]?.jsonPrimitive?.content ?: jsonString
            val mediaUrl = jsonPayload["mediaUrl"]?.jsonPrimitive?.content
            Pair(content, mediaUrl)
        } catch (_: Exception) {
            Pair(jsonString, null)
        }
    }

    suspend fun MessageDto.decryptIfNecessary(currentUserId: String): MessageDto {
        if (this.isEncrypted && this.encryptedContent != null && signalProtocolManager != null) {
            val messageId = this.id ?: return this

            // 1) Check in-memory cache first (fastest)
            decryptedMessageCache[messageId]?.let { cached ->
                Logger.d("E2EE_DECRYPT: Found message $messageId in memory cache", tag = "E2EE")
                val (content, mediaUrl) = extractContent(cached)
                return this.copy(content = content, mediaUrl = mediaUrl ?: this.mediaUrl)
            }

            // 1.1) Double-decryption guard: check if the DTO already has plaintext content
            val placeholders = setOf(
                "Message is encrypted",
                "🔒 Encrypted message",
                "🔒 You sent an encrypted message",
                "🔒 You sent an encrypted message (Copy)"
            )
            if (this.content !in placeholders && this.content.isNotBlank()) {
                // If it's already decrypted, just update the in-memory cache and return
                val (content, mediaUrl) = extractContent(this.content)
                decryptedMessageCache[messageId] = this.content
                return this.copy(content = content, mediaUrl = mediaUrl ?: this.mediaUrl)
            }

            // 2) Check local DB for previously-decrypted content (survives app restart)
            val dbCached = try {
                cachedMessageDao?.getMessages(chatId ?: "", 200)?.find { it.id == messageId }
            } catch (_: Exception) { null }
            if (dbCached != null && dbCached.content != "Message is encrypted"
                && dbCached.content != "🔒 You sent an encrypted message"
                && dbCached.content != "🔒 Encrypted message") {
                Logger.d("E2EE_DECRYPT: Found message $messageId in local DB cache", tag = "E2EE")
                decryptedMessageCache[messageId] = dbCached.content
                return this.copy(content = dbCached.content, mediaUrl = dbCached.mediaUrl ?: this.mediaUrl)
            }

            try {
                Logger.d("E2EE_DECRYPT: Attempting to decrypt message $messageId", tag = "E2EE")
                val jsonElement = Json.parseToJsonElement(this.encryptedContent).jsonObject
                val myPayloadElement = jsonElement[currentUserId]

                if (myPayloadElement != null) {
                    val senderId = this.senderId

                    // SENDER PATH — plaintext stored as JsonPrimitive
                    if (senderId == currentUserId) {
                        val plainText = try {
                            myPayloadElement.jsonPrimitive.content
                        } catch (_: Exception) { null }
                        if (plainText != null) {
                            Logger.d("E2EE_DECRYPT: Retrieved sender's plaintext copy for message $messageId", tag = "E2EE")
                            decryptedMessageCache[messageId] = plainText
                            val (content, mediaUrl) = extractContent(plainText)
                            // Persist to local DB so it survives restarts
                            try { cachedMessageDao?.updateContent(messageId, content) } catch (_: Exception) {}
                            return this.copy(content = content, mediaUrl = mediaUrl ?: this.mediaUrl)
                        }
                        Logger.d("E2EE_DECRYPT: Sender copy not plaintext, trying legacy decrypt", tag = "E2EE")
                    }

                    // RECIPIENT PATH — Signal Protocol decrypt
                    val myPayload = Json.decodeFromJsonElement(EncryptedMessage.serializer(), myPayloadElement)
                    try {
                        val decryptedBytes = signalProtocolManager.decryptMessage(
                            senderId = senderId,
                            message = myPayload
                        )
                        val decryptedContent = decryptedBytes.decodeToString()
                        Logger.d("E2EE_DECRYPT: Successfully decrypted message $messageId", tag = "E2EE")
                        decryptedMessageCache[messageId] = decryptedContent
                        val (content, mediaUrl) = extractContent(decryptedContent)
                        // Persist to local DB so we never need to re-decrypt
                        try { cachedMessageDao?.updateContent(messageId, content) } catch (_: Exception) {}
                        return this.copy(content = content, mediaUrl = mediaUrl ?: this.mediaUrl)
                    } catch (decryptError: Exception) {
                        Logger.e("E2EE_DECRYPT: Signal decryption failed for message $messageId. Deleting session and identity to force recovery.", tag = "E2EE", throwable = decryptError)
                        // Delete session and identity to force a fresh session establishment next time
                        signalProtocolManager.deleteSession(senderId)
                        signalProtocolManager.deleteIdentity(senderId)
                    }
                } else {
                    Logger.w("E2EE_DECRYPT: No payload found for current user in message $messageId. Available keys: ${jsonElement.keys}", tag = "E2EE")
                }
            } catch (e: Exception) {
                Logger.e("E2EE_DECRYPT: Critical failure for message $messageId: ${e.message}", tag = "E2EE", throwable = e)
            }
        }
        return this
    }

    suspend fun ensureSession(userId: String) {
        if (signalProtocolManager != null && !signalProtocolManager.hasSession(userId)) {
            Logger.d("E2EE_SESSION: No session exists for user $userId, establishing...", tag = "E2EE")
            val keyDto = dataSource.getUserPublicKey(userId)
            if (keyDto != null) {
                Logger.d("E2EE_SESSION: Found public key for user $userId, processing bundle", tag = "E2EE")
                val bundle = Json.decodeFromString<PreKeyBundle>(keyDto.publicKey)
                signalProtocolManager.processPreKeyBundle(userId, bundle)
                Logger.d("E2EE_SESSION: Session established successfully for user $userId", tag = "E2EE")
            } else {
                Logger.e("E2EE_SESSION: Public key not found for user $userId. They probably haven't enabled E2EE.", tag = "E2EE")
                throw Exception("Recipient hasn't enabled E2EE")
            }
        } else if (signalProtocolManager != null) {
            Logger.d("E2EE_SESSION: Session already exists for user $userId", tag = "E2EE")
        }
    }

    suspend fun initializeE2EE(currentUserId: String?): Result<Unit> {
        return try {
            if (signalProtocolManager == null) {
                val error = "SignalProtocolManager is null, E2EE not available"
                Logger.e("E2EE_INIT: $error", tag = "E2EE")
                return Result.failure(Exception(error))
            }

            if (currentUserId == null) {
                val error = "User not authenticated, cannot initialize E2EE"
                Logger.e("E2EE_INIT: $error", tag = "E2EE")
                return Result.failure(Exception(error))
            }

            Logger.d("E2EE_INIT: Starting E2EE initialization for user $currentUserId", tag = "E2EE")

            val hasLocalKeys = signalProtocolManager.hasIdentity()
            val remoteKeyDto = try { dataSource.getUserPublicKey(currentUserId) } catch (_: Exception) { null }
            val hasRemoteKeys = remoteKeyDto != null

            // Check if remote identity matches local identity
            var isIdentityMismatch = false
            if (hasLocalKeys && hasRemoteKeys && remoteKeyDto != null) {
                try {
                    val localIdentity = signalProtocolManager.getLocalIdentityKey()
                    val remoteBundle = Json.decodeFromString<PreKeyBundle>(remoteKeyDto.publicKey)
                    if (localIdentity != remoteBundle.identityKey) {
                        Logger.w("E2EE_INIT: Identity mismatch detected! Remote: ${remoteBundle.identityKey.take(10)}..., Local: ${localIdentity.take(10)}...", tag = "E2EE")
                        isIdentityMismatch = true
                    }
                } catch (e: Exception) {
                    Logger.e("E2EE_INIT: Failed to compare identities", tag = "E2EE", throwable = e)
                }
            }

            Logger.d("E2EE_INIT: hasLocalKeys=$hasLocalKeys, hasRemoteKeys=$hasRemoteKeys, mismatch=$isIdentityMismatch", tag = "E2EE")

            when {
                hasLocalKeys && hasRemoteKeys && !isIdentityMismatch -> {
                    // Both exist and match — fully initialized
                    val regId = signalProtocolManager.getLocalRegistrationId()
                    Logger.d("E2EE_INIT: E2EE already fully initialized (regId: $regId)", tag = "E2EE")

                    if (signalProtocolManager.checkKeyRotationNeeded()) {
                        Logger.w("E2EE_INIT: Key rotation recommended (keys older than 30 days)", tag = "E2EE")
                    }
                }
                hasLocalKeys && (!hasRemoteKeys || isIdentityMismatch) -> {
                    // Local keys exist but remote missing or mismatched — re-upload the bundle
                    if (isIdentityMismatch) {
                        Logger.w("E2EE_INIT: Identity mismatch — overwriting remote keys and clearing all sessions", tag = "E2EE")
                        // Clear all local sessions because they are tied to the old identity
                        signalProtocolManager.deleteAllSessions()
                    } else {
                        Logger.w("E2EE_INIT: Local keys exist but remote keys MISSING. Re-uploading...", tag = "E2EE")
                    }
                    val bundle = buildBundleFromLocalKeys()
                    val bundleStr = Json.encodeToString(bundle)
                    dataSource.uploadUserPublicKey(bundleStr)
                    Logger.d("E2EE_INIT: Successfully uploaded public key bundle", tag = "E2EE")
                }
                else -> {
                    // No local keys (or both missing) — full generation
                    if (!hasLocalKeys && hasRemoteKeys) {
                        Logger.w("E2EE_INIT: Remote keys exist but local keys MISSING. Regenerating everything...", tag = "E2EE")
                    } else {
                        Logger.d("E2EE_INIT: Fresh E2EE setup — no local or remote keys", tag = "E2EE")
                    }

                    val identity = signalProtocolManager?.generateIdentityAndKeys() ?: throw Exception("signalProtocolManager null")
                    Logger.d("E2EE_INIT: Generated identity with regId: ${identity.registrationId}", tag = "E2EE")

                    val preKeys = signalProtocolManager?.generateOneTimePreKeys(startId = 1, count = 100) ?: emptyList()
                    Logger.d("E2EE_INIT: Generated ${preKeys.size} one-time pre-keys", tag = "E2EE")

                    val bundle = PreKeyBundle(
                        registrationId = identity.registrationId,
                        deviceId = 1,
                        preKeyId = preKeys.firstOrNull()?.keyId,
                        preKeyPublic = preKeys.firstOrNull()?.publicKey,
                        signedPreKeyId = identity.signedPreKeyId,
                        signedPreKeyPublic = identity.signedPreKey,
                        signedPreKeySignature = identity.signedPreKeySignature,
                        identityKey = identity.identityKey
                    )

                    val bundleStr = Json.encodeToString(bundle)
                    Logger.d("E2EE_INIT: Uploading public key bundle to server", tag = "E2EE")
                    dataSource.uploadUserPublicKey(bundleStr)
                    Logger.d("E2EE_INIT: Successfully uploaded public key bundle", tag = "E2EE")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("E2EE_INIT: Initialization failed: ${e.message}", tag = "E2EE", throwable = e)
            Result.failure(e)
        }
    }

    /**
     * Builds a PreKeyBundle from the existing local keys.
     * Used when local keys exist but remote upload was missed.
     */
    private suspend fun buildBundleFromLocalKeys(): PreKeyBundle {
        val regId = signalProtocolManager?.getLocalRegistrationId() ?: throw Exception("signalProtocolManager null")
        val identityKey = signalProtocolManager?.getLocalIdentityKey() ?: throw Exception("signalProtocolManager null")

        // Generate fresh pre-keys to attach to the bundle
        val preKeys = signalProtocolManager?.generateOneTimePreKeys(startId = 1, count = 100) ?: emptyList()

        // We need a signed pre-key — regenerate keys to also get the signed pre-key info
        // Since the identity already exists, generateIdentityAndKeys() will re-create from the same pair
        val identity = signalProtocolManager?.generateIdentityAndKeys() ?: throw Exception("signalProtocolManager null")

        return PreKeyBundle(
            registrationId = regId,
            deviceId = 1,
            preKeyId = preKeys.firstOrNull()?.keyId,
            preKeyPublic = preKeys.firstOrNull()?.publicKey,
            signedPreKeyId = identity.signedPreKeyId,
            signedPreKeyPublic = identity.signedPreKey,
            signedPreKeySignature = identity.signedPreKeySignature,
            identityKey = identity.identityKey
        )
    }
}
