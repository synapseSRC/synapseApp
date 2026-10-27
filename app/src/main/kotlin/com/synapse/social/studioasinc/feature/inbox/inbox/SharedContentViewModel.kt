package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentType
import com.synapse.social.studioasinc.shared.domain.model.chat.FileItem
import com.synapse.social.studioasinc.shared.domain.model.chat.LinkItem
import com.synapse.social.studioasinc.shared.domain.model.chat.MediaItem
import com.synapse.social.studioasinc.shared.domain.model.chat.SharedContent
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetSharedContentSummaryUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetSharedContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharedContentUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentTab: Int = 0,
    val mediaItems: List<MediaItem> = emptyList(),
    val linkItems: List<LinkItem> = emptyList(),
    val fileItems: List<FileItem> = emptyList(),
    val mediaCount: Int = 0,
    val linkCount: Int = 0,
    val fileCount: Int = 0,
    val hasMoreMedia: Boolean = false,
    val hasMoreLinks: Boolean = false,
    val hasMoreFiles: Boolean = false,
    val lastMediaTimestamp: String? = null,
    val lastMediaId: String? = null,
    val lastLinkTimestamp: String? = null,
    val lastLinkId: String? = null,
    val lastFileTimestamp: String? = null,
    val lastFileId: String? = null,
    @StringRes val errorMessageRes: Int? = null,
    val selectedMediaItem: MediaItem? = null,
    val selectedLinkItem: LinkItem? = null,
    val selectedFileItem: FileItem? = null
)

@HiltViewModel
class SharedContentViewModel @Inject constructor(
    private val getSharedContentUseCase: GetSharedContentUseCase,
    private val getSharedContentSummaryUseCase: GetSharedContentSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedContentUiState())
    val uiState: StateFlow<SharedContentUiState> = _uiState.asStateFlow()

    private var currentChatId: String? = null

    fun loadContent(chatId: String, initialTab: Int = 0) {
        if (currentChatId == chatId && _uiState.value.mediaItems.isNotEmpty()) {
            // Already loaded for this chat, just switch tab if needed
            if (initialTab != _uiState.value.currentTab) {
                _uiState.update { it.copy(currentTab = initialTab) }
            }
            return
        }

        currentChatId = chatId
        _uiState.update {
            it.copy(
                isLoading = true,
                isRefreshing = false,
                isLoadingMore = false,
                currentTab = initialTab,
                mediaItems = emptyList(),
                linkItems = emptyList(),
                fileItems = emptyList(),
                mediaCount = 0,
                linkCount = 0,
                fileCount = 0,
                hasMoreMedia = false,
                hasMoreLinks = false,
                hasMoreFiles = false,
                lastMediaTimestamp = null,
                lastMediaId = null,
                lastLinkTimestamp = null,
                lastLinkId = null,
                lastFileTimestamp = null,
                lastFileId = null,
                errorMessageRes = null,
                selectedMediaItem = null,
                selectedLinkItem = null,
                selectedFileItem = null
            )
        }

        viewModelScope.launch {
            // Load summary first for counts
            getSharedContentSummaryUseCase(chatId)
                .onSuccess { summary ->
                    _uiState.update {
                        it.copy(
                            mediaCount = summary.mediaCount,
                            linkCount = summary.linkCount,
                            fileCount = summary.fileCount
                        )
                    }
                }
                .onFailure {
                    // Non-fatal, continue with content load
                }

            // Load all content types
            loadContentForType(chatId, ContentType.MEDIA)
            loadContentForType(chatId, ContentType.LINKS)
            loadContentForType(chatId, ContentType.FILES)

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refreshContent() {
        val chatId = currentChatId ?: return
        _uiState.update {
            it.copy(
                isRefreshing = true,
                errorMessageRes = null,
                lastMediaTimestamp = null,
                lastMediaId = null,
                lastLinkTimestamp = null,
                lastLinkId = null,
                lastFileTimestamp = null,
                lastFileId = null
            )
        }

        viewModelScope.launch {
            getSharedContentSummaryUseCase(chatId)
                .onSuccess { summary ->
                    _uiState.update {
                        it.copy(
                            mediaCount = summary.mediaCount,
                            linkCount = summary.linkCount,
                            fileCount = summary.fileCount
                        )
                    }
                }

            loadContentForType(chatId, ContentType.MEDIA)
            loadContentForType(chatId, ContentType.LINKS)
            loadContentForType(chatId, ContentType.FILES)

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun loadMoreContent() {
        val chatId = currentChatId ?: return
        val currentTab = _uiState.value.currentTab

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            val contentType = when (currentTab) {
                0 -> ContentType.MEDIA
                1 -> ContentType.LINKS
                2 -> ContentType.FILES
                else -> ContentType.MEDIA
            }

            val beforeTimestamp = when (contentType) {
                ContentType.MEDIA -> _uiState.value.lastMediaTimestamp
                ContentType.LINKS -> _uiState.value.lastLinkTimestamp
                ContentType.FILES -> _uiState.value.lastFileTimestamp
            }

            val beforeId = when (contentType) {
                ContentType.MEDIA -> _uiState.value.lastMediaId
                ContentType.LINKS -> _uiState.value.lastLinkId
                ContentType.FILES -> _uiState.value.lastFileId
            }

            getSharedContentUseCase(
                chatId = chatId,
                contentType = contentType,
                limit = 25,
                beforeTimestamp = beforeTimestamp,
                beforeId = beforeId
            ).onSuccess { content ->
                _uiState.update { state ->
                    when (contentType) {
                        ContentType.MEDIA -> state.copy(
                            mediaItems = state.mediaItems + content.mediaItems,
                            hasMoreMedia = content.hasMoreMedia,
                            lastMediaTimestamp = content.mediaItems.lastOrNull()?.timestamp,
                            lastMediaId = content.mediaItems.lastOrNull()?.messageId
                        )
                        ContentType.LINKS -> state.copy(
                            linkItems = state.linkItems + content.linkItems,
                            hasMoreLinks = content.hasMoreLinks,
                            lastLinkTimestamp = content.linkItems.lastOrNull()?.timestamp,
                            lastLinkId = content.linkItems.lastOrNull()?.messageId
                        )
                        ContentType.FILES -> state.copy(
                            fileItems = state.fileItems + content.fileItems,
                            hasMoreFiles = content.hasMoreFiles,
                            lastFileTimestamp = content.fileItems.lastOrNull()?.timestamp,
                            lastFileId = content.fileItems.lastOrNull()?.messageId
                        )
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_load_shared_content) }
            }

            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    fun switchTab(tabIndex: Int) {
        _uiState.update { it.copy(currentTab = tabIndex) }
    }

    fun onMediaItemClicked(item: MediaItem) {
        _uiState.update { it.copy(selectedMediaItem = item) }
    }

    fun onLinkItemClicked(item: LinkItem) {
        _uiState.update { it.copy(selectedLinkItem = item) }
    }

    fun onFileItemClicked(item: FileItem) {
        _uiState.update { it.copy(selectedFileItem = item) }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedMediaItem = null,
                selectedLinkItem = null,
                selectedFileItem = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessageRes = null) }
    }

    private suspend fun loadContentForType(chatId: String, contentType: ContentType) {
        getSharedContentUseCase(
            chatId = chatId,
            contentType = contentType,
            limit = 50
        ).onSuccess { content ->
            _uiState.update { state ->
                when (contentType) {
                    ContentType.MEDIA -> state.copy(
                        mediaItems = content.mediaItems,
                        hasMoreMedia = content.hasMoreMedia,
                        lastMediaTimestamp = content.mediaItems.lastOrNull()?.timestamp,
                        lastMediaId = content.mediaItems.lastOrNull()?.messageId
                    )
                    ContentType.LINKS -> state.copy(
                        linkItems = content.linkItems,
                        hasMoreLinks = content.hasMoreLinks,
                        lastLinkTimestamp = content.linkItems.lastOrNull()?.timestamp,
                        lastLinkId = content.linkItems.lastOrNull()?.messageId
                    )
                    ContentType.FILES -> state.copy(
                        fileItems = content.fileItems,
                        hasMoreFiles = content.hasMoreFiles,
                        lastFileTimestamp = content.fileItems.lastOrNull()?.timestamp,
                        lastFileId = content.fileItems.lastOrNull()?.messageId
                    )
                }
            }
        }.onFailure {
            _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_load_shared_content) }
        }
    }
}
