package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.data.repository.*
import com.synapse.social.studioasinc.data.source.CommentLocalDataSource
import com.synapse.social.studioasinc.data.source.CommentRemoteDataSource
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseBlockDataSource
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseUserDataSource
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.local.SecureStorage
import com.synapse.social.studioasinc.shared.data.local.database.CachedConversationDao
import com.synapse.social.studioasinc.shared.data.local.database.CachedMessageDao
import com.synapse.social.studioasinc.shared.data.local.database.CommentDao
import com.synapse.social.studioasinc.shared.data.local.database.PendingActionDao
import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.data.local.database.StoryDao
import com.synapse.social.studioasinc.shared.data.local.database.UserDao
import com.synapse.social.studioasinc.shared.data.repository.*
import com.synapse.social.studioasinc.shared.data.sync.SyncManager
import com.synapse.social.studioasinc.shared.domain.repository.*
import com.synapse.social.studioasinc.shared.domain.usecase.UploadMediaUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import io.ktor.client.HttpClient
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
object BackendModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClientType {
        return SupabaseClient.client
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        client: SupabaseClientType,
        signalProtocolManager: SignalProtocolManager,
        mediaUploadRepository: MediaUploadRepository,
        presenceRepository: PresenceRepository,
        offlineActionRepository: OfflineActionRepository,
        cachedMessageDao: CachedMessageDao,
        cachedConversationDao: CachedConversationDao
    ): ChatRepository {
        return SupabaseChatRepository(
            SupabaseChatDataSource(client),
            client,
            signalProtocolManager,
            mediaUploadRepository,
            presenceRepository,
            offlineActionRepository,
            cachedMessageDao,
            cachedConversationDao
        )
    }

    @Provides
    @Singleton
    fun provideSharedAuthRepository(
        client: SupabaseClientType,
        cachedMessageDao: CachedMessageDao,
        cachedConversationDao: CachedConversationDao,
        postDao: PostDao,
        storyDao: StoryDao,
        offlineActionRepository: OfflineActionRepository,
        syncManager: SyncManager
    ): AuthRepository {
        return SupabaseAuthRepository(
            client = client,
            cachedMessageDao = cachedMessageDao,
            cachedConversationDao = cachedConversationDao,
            postDao = postDao,
            storyDao = storyDao,
            offlineActionRepository = offlineActionRepository,
            syncManager = syncManager
        )
    }

    @Provides
    @Singleton
    fun providePresenceRepository(): PresenceRepository = SupabasePresenceRepository()

    @Provides
    @Singleton
    fun provideSearchRepository(): ISearchRepository = SearchRepositoryImpl()

    @Provides
    @Singleton
    fun provideAccountRepository(client: SupabaseClientType): AccountRepository = AccountRepository(client)

    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao, client: SupabaseClientType): com.synapse.social.studioasinc.data.repository.UserRepositoryImpl {
        return com.synapse.social.studioasinc.data.repository.UserRepositoryImpl(userDao, client)
    }

    @Provides
    @Singleton
    fun provideSharedUserRepository(storageDatabase: StorageDatabase, client: SupabaseClientType): com.synapse.social.studioasinc.shared.domain.repository.UserRepository {
        val userDataSource = SupabaseUserDataSource(client)
        return com.synapse.social.studioasinc.shared.data.repository.UserRepositoryImpl(storageDatabase, userDataSource)
    }

    @Provides
    @Singleton
    fun provideUsernameRepository(): UsernameRepositoryImpl = UsernameRepositoryImpl()

    @Provides
    @Singleton
    fun provideProfileRepository(client: SupabaseClientType, commentRepository: CommentRepositoryImpl): com.synapse.social.studioasinc.data.repository.ProfileRepository {
        return ProfileRepositoryImpl(client, commentRepository)
    }

    @Provides
    @Singleton
    fun provideFollowRepository(client: SupabaseClientType): FollowRepository = FollowRepositoryImpl(client)

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(client: io.github.jan.supabase.SupabaseClient): UserPreferencesRepository = UserPreferencesRepositoryImpl(client)

    @Provides
    @Singleton
    fun provideOfflineActionRepository(pendingActionDao: PendingActionDao): OfflineActionRepository = OfflineActionRepositoryImpl(pendingActionDao)

    @Provides
    @Singleton
    fun providePostRepository(postDao: PostDao, client: SupabaseClientType, offlineActionRepository: OfflineActionRepository): PostRepositoryImpl {
        return PostRepositoryImpl(postDao, client, offlineActionRepository)
    }

    @Provides
    @Singleton
    fun provideDomainPostRepository(postRepository: PostRepositoryImpl): com.synapse.social.studioasinc.domain.repository.PostRepository = postRepository

    @Provides
    @Singleton
    fun providePostActionsRepository(postRepository: PostRepositoryImpl): PostActionsRepository = postRepository

    @Provides
    @Singleton
    fun providePostInteractionRepository(): com.synapse.social.studioasinc.domain.repository.PostInteractionRepository = PostInteractionRepositoryImpl()

    @Provides
    @Singleton
    fun provideProfileActionRepository(): ProfileActionRepositoryImpl = ProfileActionRepositoryImpl()

    @Provides
    @Singleton
    fun provideReactionRepository(client: SupabaseClientType): com.synapse.social.studioasinc.domain.repository.ReactionRepository = ReactionRepositoryImpl(client)

    @Provides
    @Singleton
    fun providePostDetailRepository(client: SupabaseClientType, reactionRepository: ReactionRepositoryImpl): PostDetailRepositoryImpl = PostDetailRepositoryImpl(client, reactionRepository)

    @Provides
    @Singleton
    fun provideCommentRemoteDataSource(client: SupabaseClientType): CommentRemoteDataSource = CommentRemoteDataSource(client)

    @Provides
    @Singleton
    fun provideCommentLocalDataSource(storageDatabase: StorageDatabase, commentDao: CommentDao): CommentLocalDataSource = CommentLocalDataSource(storageDatabase, commentDao)

    @Provides
    @Singleton
    fun provideCommentRepository(
        localDataSource: CommentLocalDataSource,
        remoteDataSource: CommentRemoteDataSource,
        userRepository: com.synapse.social.studioasinc.data.repository.UserRepositoryImpl,
        reactionRepository: ReactionRepositoryImpl,
        @Named("ApplicationScope") externalScope: CoroutineScope
    ): CommentRepositoryImpl {
        return CommentRepositoryImpl(localDataSource, remoteDataSource, userRepository, externalScope, reactionRepository)
    }

    @Provides
    @Singleton
    fun providePollRepository(client: SupabaseClientType): com.synapse.social.studioasinc.domain.repository.PollRepository = PollRepositoryImpl(client)

    @Provides
    @Singleton
    fun provideConcretePollRepository(client: SupabaseClientType): PollRepositoryImpl = PollRepositoryImpl(client)

    @Provides
    @Singleton
    fun provideBookmarkRepository(client: SupabaseClientType): BookmarkRepositoryImpl = BookmarkRepositoryImpl(client)

    @Provides
    @Singleton
    fun provideReportRepository(client: SupabaseClientType): com.synapse.social.studioasinc.domain.repository.ReportRepository = ReportRepositoryImpl(client)

    @Provides
    @Singleton
    fun provideStoryRepository(@ApplicationContext context: android.content.Context, uploadMediaUseCase: UploadMediaUseCase): com.synapse.social.studioasinc.data.repository.StoryRepository { return com.synapse.social.studioasinc.data.repository.StoryRepositoryImpl(context, uploadMediaUseCase) }

    @Provides
    @Singleton
    fun provideReelRepository(): ReelRepository = ReelRepository()

@Provides
    @Singleton
    fun provideLinkPreviewRepository(database: StorageDatabase, httpClient: HttpClient): LinkPreviewRepository = LinkPreviewRepositoryImpl(database, httpClient)

    @Provides
    @Singleton
    fun provideScheduledPostRepository(client: SupabaseClientType): com.synapse.social.studioasinc.domain.repository.ScheduledPostRepository = ScheduledPostRepositoryImpl(client)

    @Provides
    @Singleton
    fun provideAppRepository(httpClient: HttpClient): AppRepository = AppRepositoryImpl(httpClient)

    @Provides
    @Singleton
    fun provideBusinessRepository(client: SupabaseClientType): BusinessRepository = BusinessRepositoryImpl(client)

    @Provides
    @Singleton
    fun provideStorageRepository(db: StorageDatabase, secureStorage: SecureStorage): StorageRepository = StorageRepositoryImpl(db, secureStorage)

    @Provides
    @Singleton
fun provideMediaUploadRepository(
        fileUploader: FileUploader,
        imgBBUploadService: com.synapse.social.studioasinc.shared.data.source.remote.ImgBBUploadService,
        cloudinaryUploadService: com.synapse.social.studioasinc.shared.data.source.remote.CloudinaryUploadService,
        supabaseUploadService: com.synapse.social.studioasinc.shared.data.source.remote.SupabaseUploadService,
        r2UploadService: com.synapse.social.studioasinc.shared.data.source.remote.R2UploadService
    ): MediaUploadRepository = MediaUploadRepositoryImpl(fileUploader, imgBBUploadService, cloudinaryUploadService, supabaseUploadService, r2UploadService)

    @Provides
    @Singleton
    fun provideNotificationRepository(client: SupabaseClientType, @Named("ApplicationScope") externalScope: CoroutineScope): NotificationRepository = SupabaseNotificationRepository(client, externalScope)

    @Provides
    @Singleton
    fun provideBlockingRepository(dataSource: SupabaseBlockDataSource): BlockRepository = BlockRepositoryImpl(dataSource)

    @Provides
    @Singleton
    fun provideChatMessagingRepository(client: SupabaseClientType): ChatMessagingRepositoryImpl = ChatMessagingRepositoryImpl(client)

    @Provides
    @Singleton
    fun provideChatRepositoryApp(signalRepository: com.synapse.social.studioasinc.shared.data.repository.SignalRepository, signalProtocolManager: SignalProtocolManager, client: SupabaseClientType): ChatRepositoryImpl = ChatRepositoryImpl(client, signalRepository, signalProtocolManager)

@Provides
    @Singleton
    fun provideChatVerificationRepository(
        @ApplicationContext context: android.content.Context,
        client: SupabaseClientType,
        signalProtocolManager: SignalProtocolManager,
        chatRepository: com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
    ): com.synapse.social.studioasinc.domain.repository.ChatVerificationRepository =
        com.synapse.social.studioasinc.data.repository.ChatVerificationRepositoryImpl(context, client, signalProtocolManager, chatRepository)

    @Provides
    @Singleton
    fun provideDraftRepository(application: android.app.Application): DraftRepositoryImpl = DraftRepositoryImpl(application)

    @Provides
    @Singleton
    fun provideEditProfileRepository(
        @ApplicationContext context: android.content.Context,
        supabaseStorageService: com.synapse.social.studioasinc.data.remote.services.SupabaseStorageService
    ): EditProfileRepositoryImpl = EditProfileRepositoryImpl(context, supabaseStorageService)

    @Provides
    @Singleton
    fun provideFeedbackRepository(): FeedbackRepositoryImpl = FeedbackRepositoryImpl()

    @Provides
    @Singleton
    fun provideLocationRepository(): LocationRepositoryImpl = LocationRepositoryImpl()

    @Provides
    @Singleton
    fun provideSettingsRepositoryImpl(@ApplicationContext context: android.content.Context): SettingsRepositoryImpl = SettingsRepositoryImpl.getInstance(context)
}













