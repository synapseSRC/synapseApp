package com.synapse.social.studioasinc.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.synapse.social.studioasinc.data.crypto.SignalSafetyNumberGenerator
import com.synapse.social.studioasinc.domain.model.ChatVerificationException
import com.synapse.social.studioasinc.domain.model.ChatVerificationFailureReason
import com.synapse.social.studioasinc.domain.model.ChatVerificationInfo
import com.synapse.social.studioasinc.domain.repository.ChatVerificationRepository
import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.models.PreKeyBundle
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ChatVerificationRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val client: SupabaseClientType,
    private val signalProtocolManager: SignalProtocolManager,
    private val chatRepository: ChatRepository
) : ChatVerificationRepository {

    /**
     * Legacy plaintext verification store. It is read once so a verification made before the state
     * moved into the encrypted Signal store is not lost, and every entry is removed as it is read;
     * nothing is ever written here again.
     */
    private val legacyPreferences: SharedPreferences =
        context.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val dataSource = SupabaseChatDataSource(client)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(chatId: String): Result<ChatVerificationInfo> = execute { loadFresh(chatId) }

    override suspend fun setVerified(info: ChatVerificationInfo, verified: Boolean): Result<Unit> = execute {
        val current = loadFresh(info.chatId)
        ensureSameIdentity(info, current)
        if (!verified) {
            signalProtocolManager.clearVerifiedSafetyNumber(current.participantId)
            return@execute
        }
        adoptPendingIdentityChangeOrThrow(current)
        signalProtocolManager.setVerifiedSafetyNumber(current.participantId, current.safetyNumber)
    }

    override suspend fun verifyScannedCode(info: ChatVerificationInfo, scannedCode: String): Result<Boolean> = execute {
        val current = loadFresh(info.chatId)
        // The snapshot the user is looking at no longer describes this conversation: the peer, or
        // the peer's key, moved between opening the sheet and scanning. Say so instead of returning
        // a plain "code does not match", which would hide a real identity change from the caller.
        if (current.participantId != info.participantId || current.safetyNumber != info.safetyNumber) {
            throw ChatVerificationException(ChatVerificationFailureReason.IDENTITY_CHANGED)
        }
        val parts = scannedCode.split('|', limit = 3)
        if (parts.size != 3 || parts[0] != QR_PREFIX || parts[1] != current.participantId) return@execute false
        val scannedKey = try { java.util.Base64.getDecoder().decode(parts[2]) }
        catch (_: IllegalArgumentException) { return@execute false }
        val expectedKey = try { java.util.Base64.getDecoder().decode(current.participantIdentityKey) }
        catch (_: IllegalArgumentException) { throw ChatVerificationException(ChatVerificationFailureReason.INVALID_IDENTITY_KEY) }
        if (!MessageDigest.isEqual(scannedKey, expectedKey)) return@execute false
        adoptPendingIdentityChangeOrThrow(current)
        signalProtocolManager.setVerifiedSafetyNumber(current.participantId, current.safetyNumber)
        true
    }

    /**
     * The comparison the user just completed is against the peer's current key, so a key that
     * changed since the last verification is adopted now: stored key advanced, previous
     * verification dropped and stale session deleted. Nothing happens when no change is pending.
     */
    private fun adoptPendingIdentityChangeOrThrow(info: ChatVerificationInfo) {
        if (signalProtocolManager.pendingIdentityChange(info.participantId) == null) return
        if (!signalProtocolManager.acceptIdentityChange(info.participantId)) {
            throw ChatVerificationException(ChatVerificationFailureReason.INVALID_IDENTITY_KEY)
        }
    }

    private suspend fun loadFresh(chatId: String): ChatVerificationInfo {
        if (chatId.isBlank() || chatId == "new") throw ChatVerificationException(ChatVerificationFailureReason.GENERAL)
        val localUserId = chatRepository.getCurrentUserId()
            ?: throw ChatVerificationException(ChatVerificationFailureReason.NOT_AUTHENTICATED)
        val participants = chatRepository.getParticipantIds(chatId).getOrElse { throw it }.distinct()
        val peers = participants.filterNot { it == localUserId }
        if (peers.size != 1) throw ChatVerificationException(ChatVerificationFailureReason.NOT_ONE_TO_ONE_CHAT)
        if (!signalProtocolManager.hasIdentity()) throw ChatVerificationException(ChatVerificationFailureReason.LOCAL_IDENTITY_MISSING)
        val localIdentityKey = signalProtocolManager.getLocalIdentityKey()
        val remoteId = peers.single()
        val remoteDto = dataSource.getUserPublicKey(remoteId)
            ?: throw ChatVerificationException(ChatVerificationFailureReason.PARTICIPANT_IDENTITY_MISSING)
        val remoteIdentityKey = try { json.decodeFromString<PreKeyBundle>(remoteDto.publicKey).identityKey }
        catch (_: Exception) { throw ChatVerificationException(ChatVerificationFailureReason.INVALID_IDENTITY_KEY) }
        val safetyNumber = try {
            SignalSafetyNumberGenerator.generate(localUserId, localIdentityKey, remoteId, remoteIdentityKey)
        } catch (_: Exception) { throw ChatVerificationException(ChatVerificationFailureReason.INVALID_IDENTITY_KEY) }
        val pendingChange = signalProtocolManager.pendingIdentityChange(remoteId)
        val verifiedSafetyNumber = signalProtocolManager.verifiedSafetyNumber(remoteId)
            ?: migrateLegacyVerification(chatId, remoteId, safetyNumber)
        return ChatVerificationInfo(
            chatId = chatId,
            participantId = remoteId,
            participantIdentityKey = remoteIdentityKey,
            safetyNumber = safetyNumber,
            localQrPayload = "$QR_PREFIX|$localUserId|$localIdentityKey",
            secureSessionActive = signalProtocolManager.hasSession(remoteId),
            // A pending change invalidates the verification even when the numbers happen to match,
            // because the user has not accepted the new key yet.
            isVerified = pendingChange == null && verifiedSafetyNumber == safetyNumber,
            identityChanged = pendingChange != null
        )
    }

    /**
     * Moves a verification that is still sitting in the plaintext store into the encrypted Signal
     * store, but only when it describes the keys in use right now. The legacy entry is removed
     * either way: it is no longer authoritative once the keys moved.
     */
    private fun migrateLegacyVerification(chatId: String, participantId: String, safetyNumber: String): String? {
        val legacyKey = "verified:$chatId:$participantId"
        val legacyValue = legacyPreferences.getString(legacyKey, null) ?: return null
        if (legacyValue != safetyNumber) {
            // Stale: it describes keys that are no longer in use, so it is dropped rather than migrated.
            legacyPreferences.edit().remove(legacyKey).commit()
            return null
        }
        // Store the encrypted entry first and commit it; only then drop the legacy entry. If the
        // encrypted write fails, the legacy entry is still on disk and the next load retries, so the
        // user cannot lose their verification.
        signalProtocolManager.setVerifiedSafetyNumber(participantId, safetyNumber)
        legacyPreferences.edit().remove(legacyKey).commit()
        return safetyNumber
    }

    private fun ensureSameIdentity(original: ChatVerificationInfo, current: ChatVerificationInfo) {
        if (original.participantId != current.participantId || original.safetyNumber != current.safetyNumber) {
            throw ChatVerificationException(ChatVerificationFailureReason.IDENTITY_CHANGED)
        }
    }

    private suspend fun <T> execute(block: suspend () -> T): Result<T> = try {
        Result.success(withContext(Dispatchers.IO) { block() })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    private companion object {
        const val LEGACY_PREFERENCES_NAME = "chat_security_verification"
        const val QR_PREFIX = "synapse-safety-v1"
    }
}
