package com.synapse.social.studioasinc.data.local.database

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import com.synapse.social.studioasinc.shared.domain.model.PendingAction
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.domain.model.ReactionType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import android.util.Log

class SyncWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
        val postRepository = entryPoint.postRepository()
        val offlineActionRepository = entryPoint.offlineActionRepository()
        val chatRepository = entryPoint.chatRepository()

        // 1. Process offline actions queue
        val pendingActions = offlineActionRepository.getPendingActions()
        Log.d(TAG, "Processing ${pendingActions.size} pending actions")

        for (action in pendingActions) {
            val success = try {
                when (action.actionType) {
                    PendingAction.ActionType.LIKE -> {
                        val payload = Json.parseToJsonElement(action.payload ?: "{}").jsonObject
                        val reactionType = ReactionType.valueOf(payload["reactionType"]?.jsonPrimitive?.content ?: "LIKE")
                        val oldReaction = payload["oldReaction"]?.jsonPrimitive?.content?.let { ReactionType.valueOf(it) }

                        postRepository.toggleReaction(
                            postId = action.targetId,
                            userId = "", // Not used in implementation
                            reactionType = reactionType,
                            oldReaction = oldReaction,
                            skipCheck = true
                        ).isSuccess
                    }
                    PendingAction.ActionType.SEND_MESSAGE -> {
                        val payload = Json.parseToJsonElement(action.payload ?: "{}").jsonObject
                        chatRepository.sendMessage(
                            chatId = action.targetId,
                            content = payload["content"]?.jsonPrimitive?.content ?: "",
                            mediaUrl = payload["mediaUrl"]?.jsonPrimitive?.content,
                            messageType = payload["messageType"]?.jsonPrimitive?.content ?: "text",
                            expiresAt = payload["expiresAt"]?.jsonPrimitive?.content,
                            replyToId = payload["replyToId"]?.jsonPrimitive?.content
                        ).isSuccess
                    }
                    else -> true // Unsupported for now
                }
            } catch (e: Exception) {
                false
            }

            if (success) {
                offlineActionRepository.removeAction(action.id)
            } else {
                val newRetryCount = action.retryCount + 1
                if (newRetryCount >= 5) {
                    offlineActionRepository.removeAction(action.id) // Give up after 5 retries
                } else {
                    offlineActionRepository.updateAction(action.id, newRetryCount, System.currentTimeMillis())
                }
            }
        }

        // 2. Original sync logic (refreshing posts)
        return try {
            postRepository.refreshPosts(0, 20)
            Result.success()
        } catch (e: Exception) {
            if (pendingActions.isNotEmpty()) Result.retry() else Result.failure()
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun postRepository(): PostRepository
    fun offlineActionRepository(): OfflineActionRepository
    fun chatRepository(): ChatRepository
}
