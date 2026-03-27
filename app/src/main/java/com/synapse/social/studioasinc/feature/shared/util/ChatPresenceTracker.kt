package com.synapse.social.studioasinc.feature.shared.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Tracks when user enters/exits a chat screen to update presence.
 * This prevents notifications from being sent when user is actively viewing the chat.
 * 
 * Usage in ChatScreen:
 * ```
 * ChatPresenceTracker(
 *     chatId = chatId,
 *     updateCurrentChat = updateCurrentChatUseCase
 * )
 * ```
 */
@Composable
fun ChatPresenceTracker(
    chatId: String,
    updateCurrentChat: suspend (String?) -> Result<Unit>
) {
    val scope = rememberCoroutineScope()
    
    DisposableEffect(chatId) {
        scope.launch {
            updateCurrentChat(chatId)
        }
        
        onDispose {
            scope.launch {
                updateCurrentChat(null)
            }
        }
    }
}
