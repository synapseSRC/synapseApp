package com.synapse.social.studioasinc.feature.inbox.inbox.models

import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset

/**
 * Consolidated UI state for ChatViewModel.
 * This replaces the previous 30+ individual StateFlow properties with a single state container,
 * making state management predictable and reducing boilerplate UI code.
 */
data class ChatUiState(
    // Message state
    val messages: List<Message> = emptyList(),
    val searchResults: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val isSearching: Boolean = false,
    val editingMessage: Message? = null,
    val replyingToMessage: Message? = null,
    val selectedMessageIds: Set<String> = emptySet(),
    val disappearingMode: DisappearingMode = DisappearingMode.OFF,
    val error: String? = null,
    val toastMessage: String? = null,
    val isE2EEReady: Boolean = false,
    val isScreenVisible: Boolean = false,
    val currentUserId: String? = null,
    val resolvedChatId: String? = null,
    val participantProfile: User? = null,
    val isGroupChat: Boolean = false,
    val onlyAdminsCanMessage: Boolean = false,
    val currentUserIsAdmin: Boolean = false,
    val isParticipantActive: Boolean = false,
    val isMuted: Boolean = false,
    val isMuteUpdating: Boolean = false,
    val canSendMessage: Boolean = true,
    val chatWallpaperType: WallpaperType = WallpaperType.DEFAULT,
    val chatWallpaperValue: String? = null,
    val chatWallpaperBlur: Float = 0f,
    val chatFontScale: Float = 1.0f,
    val chatThemePreset: ChatThemePreset = ChatThemePreset.DEFAULT,
    val chatMessageCornerRadius: Int = 16,
    val chatMaxMessageChunkSize: Int = 500,
    val messageSuggestionEnabled: Boolean = false,
    val chatAvatarDisabled: Boolean = false,
    val typingIndicatorsEnabled: Boolean = true,
    val isSummarizingMessage: Boolean = false,
)