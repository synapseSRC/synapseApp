package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.core.util.UploadProgressManager
import com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality
import com.synapse.social.studioasinc.shared.domain.repository.FileUploader
import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import com.synapse.social.studioasinc.shared.domain.usecase.ai.GenerateSmartRepliesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.*
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.ReportUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserActiveStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserPresenceDetailUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.UpdateCurrentChatUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.GetStorageConfigUseCase
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceUploadService

/**
 * Grouped dependencies for messaging concerns.
 */
data class ChatMessagingDependencies(
    val sendMessageUseCase: SendMessageUseCase,
    val editMessageUseCase: EditMessageUseCase,
    val deleteMessageUseCase: DeleteMessageUseCase,
    val deleteMessageForMeUseCase: DeleteMessageForMeUseCase,
    val bulkDeleteMessagesUseCase: BulkDeleteMessagesUseCase,
    val bulkDeleteMessagesForMeUseCase: BulkDeleteMessagesForMeUseCase,
    val populateMessageReactionsUseCase: PopulateMessageReactionsUseCase,
    val currentUserIdUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetCurrentUserIdUseCase,
    val reconcileRealtimeMessageUseCase: ReconcileRealtimeMessageUseCase,
    val chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository,
    val authRepository: com.synapse.social.studioasinc.shared.domain.repository.AuthRepository,
)

/**
 * Grouped dependencies for realtime subscription concerns.
 */
data class ChatSubscriptionDependencies(
    val subscribeToMessagesUseCase: SubscribeToMessagesUseCase,
    val subscribeToTypingStatusUseCase: SubscribeToTypingStatusUseCase,
    val subscribeToMessageReactionsUseCase: SubscribeToMessageReactionsUseCase,
    val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
    val markMessagesAsDeliveredUseCase: MarkMessagesAsDeliveredUseCase,
    val getMessagesUseCase: GetMessagesUseCase,
)

/**
 * Grouped dependencies for AI features.
 */
data class ChatAiDependencies(
    val generateSmartRepliesUseCase: GenerateSmartRepliesUseCase,
    val summarizeChatUseCase: com.synapse.social.studioasinc.domain.usecase.ai.SummarizeChatUseCase,
    val summarizeMessageUseCase: com.synapse.social.studioasinc.domain.usecase.ai.SummarizeMessageUseCase,
    val getUserProfileUseCase: GetUserProfileUseCase,
)

/**
 * Grouped dependencies for media upload concerns.
 */
data class ChatMediaDependencies(
    val uploadMediaUseCase: UploadMediaUseCase,
    val fileUploader: FileUploader,
    val uploadProgressManager: UploadProgressManager,
    val voiceUploadService: VoiceUploadService,
    val mediaCompressor: MediaCompressor?,
    val getStorageConfigUseCase: GetStorageConfigUseCase,
)

/**
 * Grouped dependencies for settings concerns.
 */
data class ChatSettingsDependencies(
    val setDisappearingModeUseCase: SetDisappearingModeUseCase,
    val getDisappearingModeUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetDisappearingModeUseCase,
    val getChatSettingsUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetChatSettingsUseCase,
    val getConversationMuteStatusUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationMuteStatusUseCase,
    val setConversationMuteStatusUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.SetConversationMuteStatusUseCase,
    val chatLockManager: com.synapse.social.studioasinc.core.util.ChatLockManager,
    val deleteExpiredMessagesUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteExpiredMessagesUseCase,
)

/**
 * Consolidated dependency configuration for ChatViewModel.
 * Groups related dependencies into configuration objects to reduce constructor parameter count.
 */
data class ChatDependencies(
    val messaging: ChatMessagingDependencies,
    val subscription: ChatSubscriptionDependencies,
    val ai: ChatAiDependencies,
    val media: ChatMediaDependencies,
    val settings: ChatSettingsDependencies,
    val initializeE2EUseCase: InitializeE2EUseCase,
    val getOrCreateChatUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetOrCreateChatUseCase,
    val getChatInfoUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetChatInfoUseCase,
    val getGroupMembersUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetGroupMembersUseCase,
    val getMessageByIdUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessageByIdUseCase,
    val toggleMessageReactionUseCase: ToggleMessageReactionUseCase,
    val observeUserActiveStatusUseCase: ObserveUserActiveStatusUseCase,
    val observeUserPresenceDetailUseCase: ObserveUserPresenceDetailUseCase,
    val updateCurrentChatUseCase: UpdateCurrentChatUseCase,
    val voteInPollUseCase: VoteInPollUseCase,
    val markViewOnceConsumedUseCase: MarkViewOnceConsumedUseCase,
    val searchMessagesUseCase: SearchMessagesUseCase,
    val exportChatUseCase: ExportChatUseCase,
    val reportChatUseCase: ReportChatUseCase,
    val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    val blockUserUseCase: BlockUserUseCase,
    val reportUserUseCase: ReportUserUseCase,
)