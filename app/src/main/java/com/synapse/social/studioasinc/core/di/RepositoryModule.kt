package com.synapse.social.studioasinc.core.di

import android.content.Context
import android.content.SharedPreferences
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.repository.*
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.SupabaseAuthRepository
import com.synapse.social.studioasinc.shared.data.repository.UserPreferencesRepository
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.local.database.UserDao
import com.synapse.social.studioasinc.shared.data.local.database.CommentDao
import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import com.synapse.social.studioasinc.shared.domain.repository.PostActionsRepository
import com.synapse.social.studioasinc.shared.domain.usecase.post.DeletePostUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.TogglePostCommentsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import com.synapse.social.studioasinc.shared.data.repository.MediaUploadRepositoryImpl
import com.synapse.social.studioasinc.shared.data.FileUploader
import com.synapse.social.studioasinc.shared.data.source.remote.ImgBBUploadService
import com.synapse.social.studioasinc.shared.data.source.remote.CloudinaryUploadService
import com.synapse.social.studioasinc.shared.data.source.remote.SupabaseUploadService
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import com.synapse.social.studioasinc.shared.data.source.remote.R2UploadService
import com.synapse.social.studioasinc.shared.domain.usecase.ValidateProviderConfigUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.GetStorageConfigUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.UpdateStorageProviderUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.UploadMediaUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.notification.GetNotificationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.notification.MarkNotificationAsReadUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.notification.SubscribeToNotificationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetCurrentUserAvatarUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.SearchUsersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.UpdateProfileUseCase
import com.synapse.social.studioasinc.data.local.AppSettingsManager
import com.synapse.social.studioasinc.shared.data.local.AndroidSecureStorage
import com.synapse.social.studioasinc.shared.data.local.SecureStorage
import com.synapse.social.studioasinc.shared.data.repository.ReelRepository
import com.synapse.social.studioasinc.shared.data.repository.SupabaseNotificationRepository
import com.synapse.social.studioasinc.shared.domain.repository.NotificationRepository
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.data.repository.UserRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.FollowRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.StorageRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.AccountRepository
import com.synapse.social.studioasinc.shared.domain.repository.FollowRepository
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowingUseCase
import com.synapse.social.studioasinc.shared.domain.repository.BusinessRepository
import com.synapse.social.studioasinc.shared.data.repository.BusinessRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.repository.AppRepository
import com.synapse.social.studioasinc.shared.data.repository.AppRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.usecase.CheckForUpdatesUseCase
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
import com.synapse.social.studioasinc.shared.data.repository.PasskeyRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.repository.PlatformInfoProvider
import com.synapse.social.studioasinc.data.local.AndroidPlatformInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import com.synapse.social.studioasinc.shared.core.media.AndroidMediaCompressor
import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.AndroidSignalProtocolManager
import com.synapse.social.studioasinc.shared.domain.usecase.chat.InitializeE2EUseCase

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    @Named("ApplicationScope")
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideSharedAuthRepository(
        client: SupabaseClientType
    ): AuthRepository {
        return SupabaseAuthRepository(client)
    }

    @Provides
    @Singleton
    fun provideAccountRepository(
        client: SupabaseClientType
    ): AccountRepository {
        return AccountRepository(client)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepositoryImpl.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao, client: SupabaseClientType): UserRepository {
        return UserRepository(userDao, client)
    }

    @Provides
    @Singleton
    fun provideSharedUserRepository(
        storageDatabase: StorageDatabase,
        client: SupabaseClientType
    ): com.synapse.social.studioasinc.shared.domain.repository.UserRepository {
        return UserRepositoryImpl(storageDatabase, client)
    }

    @Provides
    @Singleton
    fun providePostRepository(
        postDao: PostDao,
        client: SupabaseClientType
    ): PostRepository {
        return PostRepository(postDao, client)
    }

    @Provides
    @Singleton
    fun providePostActionsRepository(
        postRepository: PostRepository
    ): PostActionsRepository {
        return postRepository
    }

    @Provides
    @Singleton
    fun provideUsernameRepository(): UsernameRepository {
        return UsernameRepository()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("synapse_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        client: SupabaseClientType,
        commentRepository: CommentRepository
    ): ProfileRepository {
        return ProfileRepositoryImpl(client, commentRepository)
    }

    @Provides
    @Singleton
    fun providePostInteractionRepository(): PostInteractionRepository {
        return PostInteractionRepository()
    }

    @Provides
    @Singleton
    fun provideProfileActionRepository(): ProfileActionRepository {
        return ProfileActionRepository()
    }

    @Provides
    @Singleton
    fun provideReactionRepository(client: SupabaseClientType): ReactionRepository {
        return ReactionRepository(client)
    }

    @Provides
    @Singleton
    fun providePostDetailRepository(
        client: SupabaseClientType,
        reactionRepository: ReactionRepository
    ): PostDetailRepository {
        return PostDetailRepository(client, reactionRepository)
    }

    @Provides
    @Singleton
    fun provideCommentRepository(
        storageDatabase: StorageDatabase,
        client: SupabaseClientType,
        commentDao: CommentDao,
        userRepository: UserRepository,
        reactionRepository: ReactionRepository,
        @Named("ApplicationScope") externalScope: CoroutineScope
    ): CommentRepository {
        return CommentRepository(
            storageDatabase = storageDatabase,
            client = client,
            commentDao = commentDao,
            userRepository = userRepository,
            externalScope = externalScope,
            reactionRepository = reactionRepository
        )
    }

    @Provides
    @Singleton
    fun providePollRepository(client: SupabaseClientType): PollRepository {
        return PollRepository(client)
    }

    @Provides
    @Singleton
    fun provideBookmarkRepository(client: SupabaseClientType): BookmarkRepository {
        return BookmarkRepository(client)
    }

    @Provides
    @Singleton
    fun provideReshareRepository(client: SupabaseClientType): ReshareRepository {
        return ReshareRepository(client)
    }

    @Provides
    @Singleton
    fun provideReportRepository(client: SupabaseClientType): ReportRepository {
        return ReportRepository(client)
    }

    @Provides
    @Singleton
    fun provideStoryRepository(
        @ApplicationContext context: Context,
        uploadMediaUseCase: UploadMediaUseCase
    ): StoryRepository {
        return StoryRepositoryImpl(context, uploadMediaUseCase)
    }

    @Provides
    @Singleton
    fun provideAppSettingsManager(@ApplicationContext context: Context): AppSettingsManager {
        return AppSettingsManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideReelRepository(): ReelRepository {
        return ReelRepository()
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        client: SupabaseClientType,
        @Named("ApplicationScope") externalScope: CoroutineScope
    ): NotificationRepository {
        return SupabaseNotificationRepository(client, externalScope)
    }

    @Provides
    @Singleton
    fun provideSecureStorage(@ApplicationContext context: Context): SecureStorage {
        return AndroidSecureStorage(context)
    }

    @Provides
    @Singleton
    fun provideStorageRepository(
        db: StorageDatabase,
        secureStorage: SecureStorage
    ): StorageRepository {
        return StorageRepositoryImpl(db, secureStorage)
    }

    @Provides
    @Singleton
    fun provideKtorHttpClient(): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
        }
    }

    @Provides
    @Singleton
    fun provideFileUploader(@ApplicationContext context: Context): FileUploader {
        return FileUploader(context)
    }

    @Provides
    @Singleton
    fun provideImgBBUploadService(httpClient: HttpClient): ImgBBUploadService {
        return ImgBBUploadService(httpClient)
    }

    @Provides
    @Singleton
    fun provideCloudinaryUploadService(httpClient: HttpClient): CloudinaryUploadService {
        return CloudinaryUploadService(httpClient)
    }

    @Provides
    @Singleton
    fun provideSupabaseUploadService(supabaseClient: SupabaseClientType): SupabaseUploadService {
        return SupabaseUploadService(supabaseClient)
    }

    @Provides
    @Singleton
    fun provideR2UploadService(httpClient: HttpClient): R2UploadService {
        return R2UploadService(httpClient)
    }

    @Provides
    @Singleton
    fun provideValidateProviderConfigUseCase(): ValidateProviderConfigUseCase {
        return ValidateProviderConfigUseCase()
    }

    @Provides
    @Singleton
    fun provideGetStorageConfigUseCase(
        repository: StorageRepository
    ): GetStorageConfigUseCase {
        return GetStorageConfigUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateStorageProviderUseCase(
        repository: StorageRepository
    ): UpdateStorageProviderUseCase {
        return UpdateStorageProviderUseCase(repository)
    }


    @Provides
    @Singleton
    fun provideMediaCompressor(@ApplicationContext context: Context): MediaCompressor {
        return AndroidMediaCompressor(context)
    }

    @Provides
    @Singleton
    fun provideMediaUploadRepository(
        fileUploader: FileUploader,
        imgBBUploadService: ImgBBUploadService,
        cloudinaryUploadService: CloudinaryUploadService,
        supabaseUploadService: SupabaseUploadService,
        r2UploadService: R2UploadService
    ): MediaUploadRepository {
        return MediaUploadRepositoryImpl(
            fileUploader,
            imgBBUploadService,
            cloudinaryUploadService,
            supabaseUploadService,
            r2UploadService
        )
    }

    @Provides
    @Singleton
    fun provideUploadMediaUseCase(
        repository: StorageRepository,
        mediaUploadRepository: MediaUploadRepository,
        mediaCompressor: MediaCompressor
    ): UploadMediaUseCase {
        return UploadMediaUseCase(
            repository,
            mediaUploadRepository,
            mediaCompressor
        )
    }

    @Provides
    @Singleton
    fun provideGetNotificationsUseCase(
        notificationRepository: NotificationRepository,
        authRepository: AuthRepository
    ): GetNotificationsUseCase {
        return GetNotificationsUseCase(notificationRepository, authRepository)
    }

    @Provides
    @Singleton
    fun provideMarkNotificationAsReadUseCase(
        notificationRepository: NotificationRepository,
        authRepository: AuthRepository
    ): MarkNotificationAsReadUseCase {
        return MarkNotificationAsReadUseCase(notificationRepository, authRepository)
    }

    @Provides
    @Singleton
    fun provideSubscribeToNotificationsUseCase(
        notificationRepository: NotificationRepository,
        authRepository: AuthRepository
    ): SubscribeToNotificationsUseCase {
        return SubscribeToNotificationsUseCase(notificationRepository, authRepository)
    }

    @Provides
    @Singleton
    fun provideGetUserProfileUseCase(
        userRepository: com.synapse.social.studioasinc.shared.domain.repository.UserRepository
    ): GetUserProfileUseCase {
        return GetUserProfileUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideGetCurrentUserAvatarUseCase(
        userRepository: com.synapse.social.studioasinc.shared.domain.repository.UserRepository
    ): GetCurrentUserAvatarUseCase {
        return GetCurrentUserAvatarUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideSearchUsersUseCase(
        userRepository: com.synapse.social.studioasinc.shared.domain.repository.UserRepository
    ): SearchUsersUseCase {
        return SearchUsersUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideUpdateProfileUseCase(
        userRepository: com.synapse.social.studioasinc.shared.domain.repository.UserRepository
    ): UpdateProfileUseCase {
        return UpdateProfileUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideFollowRepository(
        client: SupabaseClientType
    ): FollowRepository {
        return FollowRepositoryImpl(client)
    }

    @Provides
    @Singleton
    fun provideGetFollowersUseCase(
        followRepository: FollowRepository
    ): GetFollowersUseCase {
        return GetFollowersUseCase(followRepository)
    }

    @Provides
    @Singleton
    fun provideGetFollowingUseCase(
        followRepository: FollowRepository
    ): GetFollowingUseCase {
        return GetFollowingUseCase(followRepository)
    }

    @Provides
    @Singleton
    fun provideDeletePostUseCase(
        repository: PostActionsRepository
    ): DeletePostUseCase {
        return DeletePostUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideTogglePostCommentsUseCase(
        repository: PostActionsRepository
    ): TogglePostCommentsUseCase {
        return TogglePostCommentsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideBusinessRepository(
        client: SupabaseClientType
    ): BusinessRepository {
        return BusinessRepositoryImpl(client)
    }

    @Provides
    @Singleton
    fun provideAppRepository(
        httpClient: HttpClient
    ): AppRepository {
        return AppRepositoryImpl(httpClient)
    }

    @Provides
    @Singleton
    fun providePlatformInfoProvider(
        @ApplicationContext context: Context
    ): PlatformInfoProvider {
        return AndroidPlatformInfoProvider(context)
    }

    @Provides
    @Singleton
    fun provideCheckForUpdatesUseCase(
        appRepository: AppRepository,
        platformInfoProvider: PlatformInfoProvider
    ): CheckForUpdatesUseCase {
        return CheckForUpdatesUseCase(appRepository, platformInfoProvider)
    }

    @Provides
    @Singleton
    fun providePasskeyRepository(
        client: SupabaseClientType
    ): PasskeyRepository {
        return PasskeyRepositoryImpl(client)
    }

    @Provides
    @Singleton
    fun provideSignalProtocolManager(
        @ApplicationContext context: Context
    ): SignalProtocolManager {
        return AndroidSignalProtocolManager(context)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        client: SupabaseClientType,
        signalProtocolManager: SignalProtocolManager
    ): com.synapse.social.studioasinc.shared.domain.repository.ChatRepository {
        return com.synapse.social.studioasinc.shared.data.repository.SupabaseChatRepository(
            com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource(client),
            client,
            signalProtocolManager
        )
    }

    @Provides
    @Singleton
    fun provideGetConversationsUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationsUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationsUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideGetMessagesUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessagesUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessagesUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideSendMessageUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideSubscribeToMessagesUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToMessagesUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToMessagesUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideMarkMessagesAsReadUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsReadUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsReadUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideSubscribeToInboxUpdatesUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToInboxUpdatesUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToInboxUpdatesUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideBroadcastTypingStatusUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.BroadcastTypingStatusUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.BroadcastTypingStatusUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideSubscribeToTypingStatusUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToTypingStatusUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToTypingStatusUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideEditMessageUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.EditMessageUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.EditMessageUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideDeleteMessageUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideUploadMediaUseCaseChat(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository,
        storageRepository: com.synapse.social.studioasinc.shared.domain.repository.StorageRepository,
        mediaUploadRepository: MediaUploadRepository,
        fileUploader: com.synapse.social.studioasinc.shared.data.FileUploader
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.UploadMediaUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.UploadMediaUseCase(chatRepository, storageRepository, mediaUploadRepository, fileUploader)
    }

    @Provides
    @Singleton
    fun provideInitializeE2EUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): InitializeE2EUseCase {
        return InitializeE2EUseCase(chatRepository)
    }
    @Provides
    @Singleton
    fun provideDeleteMessageForMeUseCase(
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageForMeUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageForMeUseCase(chatRepository)
    }
}
