package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.model.MessageDto
import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.repository.SignalRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepositoryImpl(
    private val client: SupabaseClient,
    private val signalRepository: SignalRepository,
    private val signalProtocolManager: SignalProtocolManager
) {

    suspend fun sendMessage(recipientId: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            val senderId = currentUser.id

            // Check if session exists
            if (!signalProtocolManager.hasSession(recipientId)) {
                // Fetch PreKeyBundle
                val bundle = signalRepository.fetchPreKeyBundle(recipientId)
                    ?: return@withContext Result.failure(Exception("Recipient keys not found"))

                // Process PreKeyBundle
                signalProtocolManager.processPreKeyBundle(recipientId, bundle)
            }

            // Encrypt message
            val encryptedMessage = signalProtocolManager.encryptMessage(recipientId, content.toByteArray())

            // Create MessageDto
            val messageDto = MessageDto(
                senderId = senderId,
                recipientId = recipientId,
                content = encryptedMessage.body,
                type = encryptedMessage.type,
                registrationId = encryptedMessage.registrationId,
                deviceId = 1, // Assuming default device ID 1
                createdAt = java.time.Instant.now().toString()
            )

            // Insert into Supabase
            client.postgrest.from("messages").insert(messageDto)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun decryptMessage(senderId: String, encryptedContent: String, type: Int, registrationId: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val encryptedMessage = EncryptedMessage(
                type = type,
                body = encryptedContent,
                registrationId = registrationId
            )
            val decryptedBytes = signalProtocolManager.decryptMessage(senderId, encryptedMessage)
            Result.success(String(decryptedBytes))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
