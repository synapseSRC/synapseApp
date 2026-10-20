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
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val dataSource = SupabaseChatDataSource(client)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(chatId: String): Result<ChatVerificationInfo> = execute { loadFresh(chatId) }

    override suspend fun setVerified(info: ChatVerificationInfo, verified: Boolean): Result<Unit> = execute {
        val current = loadFresh(info.chatId)
        ensureSameIdentity(info, current)
        val editor = preferences.edit()
        if (verified) editor.putString(preferenceKey(current), current.safetyNumber) else editor.remove(preferenceKey(current))
        if (!editor.commit()) throw ChatVerificationException(ChatVerificationFailureReason.STORAGE_FAILURE)
    }

    override suspend fun verifyScannedCode(info: ChatVerificationInfo, scannedCode: String): Result<Boolean> = execute {
        val current = loadFresh(info.chatId)
        if (current.participantId != info.participantId || current.safetyNumber != info.safetyNumber) return@execute false
        val parts = scannedCode.split('|', limit = 3)
        if (parts.size != 3 || parts[0] != QR_PREFIX || parts[1] != current.participantId) return@execute false
        val scannedKey = try { java.util.Base64.getDecoder().decode(parts[2]) }
        catch (_: IllegalArgumentException) { return@execute false }
        val expectedKey = try { java.util.Base64.getDecoder().decode(current.participantIdentityKey) }
        catch (_: IllegalArgumentException) { throw ChatVerificationException(ChatVerificationFailureReason.INVALID_IDENTITY_KEY) }
        if (!java.security.MessageDigest.isEqual(scannedKey, expectedKey)) return@execute false
        if (!preferences.edit().putString(preferenceKey(current), current.safetyNumber).commit()) {
            throw ChatVerificationException(ChatVerificationFailureReason.STORAGE_FAILURE)
        }
        true
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
        return ChatVerificationInfo(
            chatId = chatId,
            participantId = remoteId,
            participantIdentityKey = remoteIdentityKey,
            safetyNumber = safetyNumber,
            localQrPayload = "$QR_PREFIX|$localUserId|$localIdentityKey",
            secureSessionActive = signalProtocolManager.hasSession(remoteId),
            isVerified = preferences.getString("verified:$chatId:$remoteId", null) == safetyNumber
        )
    }

    private fun ensureSameIdentity(original: ChatVerificationInfo, current: ChatVerificationInfo) {
        if (original.participantId != current.participantId || original.safetyNumber != current.safetyNumber) {
            throw ChatVerificationException(ChatVerificationFailureReason.IDENTITY_CHANGED)
        }
    }

    private fun preferenceKey(info: ChatVerificationInfo): String = "verified:" + info.chatId + ":" + info.participantId

    private suspend fun <T> execute(block: suspend () -> T): Result<T> = try {
        Result.success(withContext(Dispatchers.IO) { block() })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    private companion object {
        const val PREFERENCES_NAME = "chat_security_verification"
        const val QR_PREFIX = "synapse-safety-v1"
    }
}
