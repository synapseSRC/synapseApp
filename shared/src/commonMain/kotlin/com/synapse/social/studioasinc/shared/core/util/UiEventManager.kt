package com.synapse.social.studioasinc.shared.core.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class UiEvent {
    data class Message(val text: String) : UiEvent()
    data class Error(val text: String) : UiEvent()
    data class Success(val text: String) : UiEvent()
}

object UiEventManager {
    private val _events = MutableSharedFlow<UiEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    suspend fun emit(event: UiEvent) {
        _events.emit(event)
    }
}
