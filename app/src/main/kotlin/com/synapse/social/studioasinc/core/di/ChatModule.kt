package com.synapse.social.studioasinc.core.di

import android.content.Context
import android.content.SharedPreferences
import com.synapse.social.studioasinc.data.local.AndroidPlatformInfoProvider
import com.synapse.social.studioasinc.data.local.AppSettingsManager
import com.synapse.social.studioasinc.data.repository.*
import com.synapse.social.studioasinc.data.repository.DomainProfileRepositoryAdapter
import com.synapse.social.studioasinc.data.repository.DomainSettingsRepositoryAdapter
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.shared.core.media.AndroidMediaCompressor
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.crypto.AndroidSignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.local.AndroidSecureStorage
import com.synapse.social.studioasinc.shared.data.local.SecureStorage
import com.synapse.social.studioasinc.shared.data.local.database.CachedConversationDao
import com.synapse.social.studioasinc.shared.data.local.database.CachedMessageDao
import com.synapse.social.studioasinc.shared.data.local.database.CommentDao
import com.synapse.social.studioasinc.shared.data.local.database.PendingActionDao
import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.data.local.database.UserDao
import com.synapse.social.studioasinc.shared.data.repository.AccountRepository
import com.synapse.social.studioasinc.shared.data.repository.AppRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.BusinessRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.FollowRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.LinkPreviewRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.MediaUploadRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.OfflineActionRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.PasskeyRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.ReelRepository
import com.synapse.social.studioasinc.shared.data.repository.StorageRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.SupabaseAuthRepository
import com.synapse.social.studioasinc.shared.data.repository.SupabaseChatRepository
import com.synapse.social.studioasinc.shared.data.repository.SupabaseNotificationRepository
import com.synapse.social.studioasinc.shared.data.repository.UserPreferencesRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.UserRepositoryImpl
import com.synapse.social.studioasinc.shared.data.source.remote.CloudinaryUploadService
import com.synapse.social.studioasinc.shared.data.source.remote.ImgBBUploadService
import com.synapse.social.studioasinc.shared.data.source.remote.R2UploadService
import com.synapse.social.studioasinc.shared.data.source.remote.SupabaseUploadService
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceUploadService
import com.synapse.social.studioasinc.shared.domain.repository.AppRepository
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.BusinessRepository
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.FileUploader
import com.synapse.social.studioasinc.shared.domain.repository.FollowRepository
import com.synapse.social.studioasinc.shared.domain.repository.LinkPreviewRepository
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import com.synapse.social.studioasinc.shared.domain.repository.NotificationRepository
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
import com.synapse.social.studioasinc.shared.domain.repository.PlatformInfoProvider
import com.synapse.social.studioasinc.shared.domain.repository.PostActionsRepository
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import com.synapse.social.studioasinc.shared.domain.repository.UserPreferencesRepository
import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import com.synapse.social.studioasinc.shared.domain.usecase.CheckForUpdatesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.GetStorageConfigUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.UpdateStorageProviderUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.ValidateProviderConfigUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.BroadcastTypingStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.BulkDeleteMessagesForMeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.BulkDeleteMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.ClearChatHistoryUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageForMeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.EditMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetCommonGroupsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationMuteStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetDisappearingModeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessageByIdUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetSharedContentSummaryUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.InitializeE2EUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsDeliveredUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsReadUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.ResetE2EUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SetConversationMuteStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SetDisappearingModeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToInboxUpdatesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToTypingStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.UploadMediaUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.ToggleMessageReactionUseCase
    import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessageReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.PopulateMessageReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.ReconcileRealtimeMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToMessageReactionsUseCase
    import com.synapse.social.studioasinc.shared.domain.usecase.chat.VoteInPollUseCase
    import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkViewOnceConsumedUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowingUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.notification.GetNotificationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.notification.MarkNotificationAsReadUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.notification.SubscribeToNotificationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.DeletePostUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.TogglePostCommentsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.ObserveChatSettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.SyncChatSettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetCurrentUserAvatarUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.SearchUsersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.UpdateProfileUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json


@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideUploadMediaUseCase(
        chatRepository: ChatRepository,
        storageRepository: StorageRepository,
        mediaUploadRepository: MediaUploadRepository,
        fileUploader: FileUploader
    ): UploadMediaUseCase = UploadMediaUseCase(chatRepository, storageRepository, mediaUploadRepository, fileUploader)

    @Provides
    @Singleton
    fun provideGetDisappearingModeUseCase(chatRepository: ChatRepository): GetDisappearingModeUseCase = GetDisappearingModeUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideSetDisappearingModeUseCase(chatRepository: ChatRepository): SetDisappearingModeUseCase = SetDisappearingModeUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideClearChatHistoryUseCase(chatRepository: ChatRepository): ClearChatHistoryUseCase = ClearChatHistoryUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideGetSharedContentSummaryUseCase(chatRepository: ChatRepository): GetSharedContentSummaryUseCase = GetSharedContentSummaryUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideGetConversationMuteStatusUseCase(chatRepository: ChatRepository): GetConversationMuteStatusUseCase = GetConversationMuteStatusUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideSetConversationMuteStatusUseCase(chatRepository: ChatRepository): SetConversationMuteStatusUseCase = SetConversationMuteStatusUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideGetCommonGroupsUseCase(chatRepository: ChatRepository): GetCommonGroupsUseCase = GetCommonGroupsUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideGetMessagesUseCase(chatRepository: ChatRepository): GetMessagesUseCase = GetMessagesUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideSendMessageUseCase(chatRepository: ChatRepository, signalProtocolManager: SignalProtocolManager): SendMessageUseCase = SendMessageUseCase(chatRepository, signalProtocolManager)

    @Provides
    @Singleton
    fun provideSubscribeToMessagesUseCase(chatRepository: ChatRepository): SubscribeToMessagesUseCase = SubscribeToMessagesUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideMarkMessagesAsReadUseCase(chatRepository: ChatRepository): MarkMessagesAsReadUseCase = MarkMessagesAsReadUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideMarkMessagesAsDeliveredUseCase(chatRepository: ChatRepository): MarkMessagesAsDeliveredUseCase = MarkMessagesAsDeliveredUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideBroadcastTypingStatusUseCase(chatRepository: ChatRepository): BroadcastTypingStatusUseCase = BroadcastTypingStatusUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideSubscribeToTypingStatusUseCase(chatRepository: ChatRepository): SubscribeToTypingStatusUseCase = SubscribeToTypingStatusUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideEditMessageUseCase(chatRepository: ChatRepository): EditMessageUseCase = EditMessageUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideDeleteMessageUseCase(chatRepository: ChatRepository): DeleteMessageUseCase = DeleteMessageUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideDeleteMessageForMeUseCase(chatRepository: ChatRepository): DeleteMessageForMeUseCase = DeleteMessageForMeUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideBulkDeleteMessagesForMeUseCase(chatRepository: ChatRepository): BulkDeleteMessagesForMeUseCase = BulkDeleteMessagesForMeUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideInitializeE2EUseCase(chatRepository: ChatRepository): InitializeE2EUseCase = InitializeE2EUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideGetMessageByIdUseCase(chatRepository: ChatRepository): GetMessageByIdUseCase = GetMessageByIdUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideToggleMessageReactionUseCase(chatRepository: ChatRepository): ToggleMessageReactionUseCase = ToggleMessageReactionUseCase(chatRepository)

    @Provides
    @Singleton
    fun providePopulateMessageReactionsUseCase(chatRepository: ChatRepository): PopulateMessageReactionsUseCase = PopulateMessageReactionsUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideSubscribeToMessageReactionsUseCase(chatRepository: ChatRepository): SubscribeToMessageReactionsUseCase = SubscribeToMessageReactionsUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideVoteInPollUseCase(chatRepository: ChatRepository): VoteInPollUseCase = VoteInPollUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideMarkViewOnceConsumedUseCase(chatRepository: ChatRepository): MarkViewOnceConsumedUseCase = MarkViewOnceConsumedUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideReconcileRealtimeMessageUseCase(chatRepository: ChatRepository): ReconcileRealtimeMessageUseCase = ReconcileRealtimeMessageUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideResetE2EUseCase(chatRepository: ChatRepository): ResetE2EUseCase = ResetE2EUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideGetConversationsUseCase(chatRepository: ChatRepository): GetConversationsUseCase = GetConversationsUseCase(chatRepository)

    @Provides
    @Singleton
    fun provideSubscribeToInboxUpdatesUseCase(chatRepository: ChatRepository): SubscribeToInboxUpdatesUseCase = SubscribeToInboxUpdatesUseCase(chatRepository)
}
