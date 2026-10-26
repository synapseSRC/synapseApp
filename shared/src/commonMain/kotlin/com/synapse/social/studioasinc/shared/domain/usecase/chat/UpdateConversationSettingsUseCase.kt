package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource

class UpdateConversationSettingsUseCase(
    private val dataSource: SupabaseChatDataSource
) {
    suspend operator fun invoke(
        chatId: String,
        isPinned: Boolean? = null,
        isMuted: Boolean? = null,
        isArchived: Boolean? = null,
        currentVersion: Int? = null
    ): Result<Unit> {
        return dataSource.updateChatSettings(chatId, isPinned, isMuted, isArchived, currentVersion)
    }
}
