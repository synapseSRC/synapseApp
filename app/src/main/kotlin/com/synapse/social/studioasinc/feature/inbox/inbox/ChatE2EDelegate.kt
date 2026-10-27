package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessageByIdUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.ReconcileRealtimeMessageUseCase
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.mergeMonotonic
import com.synapse.social.studioasinc.shared.domain.util.EncryptedPayloadDetector
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class ChatE2EDelegate(
    private val getMessageByIdUseCase: GetMessageByIdUseCase,
    private val reconcileRealtimeMessageUseCase: ReconcileRealtimeMessageUseCase,
    private val messagingDelegate: ChatMessagingDelegate,
    private val viewModelScope: CoroutineScope,
    private val currentUserIdProvider: () -> String?,
    private val currentChatIdProvider: () -> String?,
    private val isE2EEReady: StateFlow<Boolean>
) {

    private val activeRetryJobs = ConcurrentHashMap<Job, Unit>()

    companion object {
        fun isUndecryptable(message: Message): Boolean =
            EncryptedPayloadDetector.isUndecryptable(message.content, message.encryptionFailureReason)
    }

    fun retryDecryptionWhenReady(message: Message) {
        val chatId = currentChatIdProvider() ?: message.chatId
        val job = viewModelScope.launch {
            val ready = isE2EEReady.value ||
                withTimeoutOrNull(10.seconds) { isE2EEReady.first { it } } == true
            if (!ready) return@launch
            if (currentChatIdProvider()?.equals(chatId, ignoreCase = true) != true) return@launch

            getMessageByIdUseCase(message.id).onSuccess { refreshed ->
                if (refreshed == null || isUndecryptable(refreshed)) return@onSuccess
                if (currentChatIdProvider()?.equals(chatId, ignoreCase = true) != true) return@onSuccess
                messagingDelegate._messages.update { current ->
                    val existing = current.firstOrNull { it.id == message.id }
                    val toWrite = refreshed.copy(clientId = existing?.clientId ?: refreshed.clientId)
                    val merged = existing?.mergeMonotonic(toWrite) ?: toWrite
                    with(messagingDelegate) { current.replaceById(message.id, merged) }
                }
                val decrypted = messagingDelegate._messages.value.firstOrNull { it.id == message.id }
                    ?: return@onSuccess
                try {
                    reconcileRealtimeMessageUseCase(chatId, decrypted)
                } catch (e: Exception) {
                    Napier.w("Cache reconciliation failed for decrypted message ${message.id}: ${e.message}", tag = "ChatViewModel")
                }
            }.onFailure { error ->
                Napier.w("Decryption retry failed for message ${message.id}: ${error.message}", tag = "ChatViewModel")
            }
        }
        activeRetryJobs[job] = Unit
        job.invokeOnCompletion { activeRetryJobs.remove(job) }
    }

    fun retryAllUndecryptableMessages(chatId: String) {
        val job = viewModelScope.launch {
            val ready = isE2EEReady.value ||
                withTimeoutOrNull(15.seconds) { isE2EEReady.first { it } } == true

            if (!ready) {
                Napier.w("E2EE not ready after 15s on resume, skipping decryption retry", tag = "ChatViewModel")
                return@launch
            }

            if (currentChatIdProvider()?.equals(chatId, ignoreCase = true) != true) return@launch

            val undecryptableMessages = messagingDelegate.messages.value
                .filter { isUndecryptable(it) && it.senderId != currentUserIdProvider() }
                .takeLast(10)

            if (undecryptableMessages.isEmpty()) return@launch

            Napier.d("Retrying decryption for ${undecryptableMessages.size} messages on resume", tag = "ChatViewModel")

            undecryptableMessages.forEach { message ->
                getMessageByIdUseCase(message.id).onSuccess { refreshed ->
                    if (refreshed == null || isUndecryptable(refreshed)) return@onSuccess
                    if (currentChatIdProvider()?.equals(chatId, ignoreCase = true) != true) return@onSuccess

                    messagingDelegate._messages.update { current ->
                        val existing = current.firstOrNull { it.id == message.id }
                        val toWrite = refreshed.copy(clientId = existing?.clientId ?: refreshed.clientId)
                        val merged = existing?.mergeMonotonic(toWrite) ?: toWrite
                        with(messagingDelegate) { current.replaceById(message.id, merged) }
                    }

                    val decrypted = messagingDelegate._messages.value.firstOrNull { it.id == message.id }
                        ?: return@onSuccess
                    try {
                        reconcileRealtimeMessageUseCase(chatId, decrypted)
                    } catch (e: Exception) {
                        Napier.w("Cache reconciliation failed for decrypted message ${message.id}: ${e.message}", tag = "ChatViewModel")
                    }
                }.onFailure { error ->
                    Napier.w("Decryption retry failed for message ${message.id}: ${error.message}", tag = "ChatViewModel")
                }
            }
        }
        activeRetryJobs[job] = Unit
        job.invokeOnCompletion { activeRetryJobs.remove(job) }
    }

    fun cleanup() {
        activeRetryJobs.keys.forEach { it.cancel() }
        activeRetryJobs.clear()
    }
}
