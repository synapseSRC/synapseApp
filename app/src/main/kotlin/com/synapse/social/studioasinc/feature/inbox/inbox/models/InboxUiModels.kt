package com.synapse.social.studioasinc.feature.inbox.inbox.models

import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message

enum class InboxFilter { ALL, UNREAD, FAVORITES, GROUPS }
enum class InboxSearchCategory { UNREAD, PHOTOS, LINKS, DOCS }

data class InboxSearchResult(
    val conversation: Conversation,
    val message: Message
)

data class InboxSearchState(
    val query: String = "",
    val category: InboxSearchCategory = InboxSearchCategory.UNREAD,
    val isLoading: Boolean = false,
    val messages: List<InboxSearchResult> = emptyList(),
    val people: List<User> = emptyList(),
    val failedConversationCount: Int = 0,
    val contactsSearchFailed: Boolean = false
)

enum class EmptyStateType {
    CHATS,
    CALLS,
    CONTACTS,
    SEARCH_NO_RESULTS,
    ARCHIVED,
    ERROR
}
