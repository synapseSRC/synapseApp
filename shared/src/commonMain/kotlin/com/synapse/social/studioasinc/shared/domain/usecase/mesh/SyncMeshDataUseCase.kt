package com.synapse.social.studioasinc.shared.domain.usecase.mesh

import com.synapse.social.studioasinc.shared.domain.repository.MeshRepository
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.util.Logger

class SyncMeshDataUseCase(
    private val meshRepository: MeshRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val pendingMessages = meshRepository.getPendingSyncMessages()
        if (pendingMessages.isEmpty()) return Result.success(Unit)

        pendingMessages.forEach { meshMsg ->
            try {
                // If it's a chat message, try sending it via Supabase
                if (meshMsg.chatId != null) {
                    val result = chatRepository.sendMessage(
                        chatId = meshMsg.chatId,
                        content = meshMsg.content,
                        messageType = meshMsg.type
                    )
                    if (result.isSuccess) {
                        meshRepository.markAsSynced(meshMsg.id)
                    }
                }
            } catch (e: Exception) {
                Logger.e("Failed to sync mesh message ${meshMsg.id}", throwable = e)
            }
        }

        return Result.success(Unit)
    }
}
