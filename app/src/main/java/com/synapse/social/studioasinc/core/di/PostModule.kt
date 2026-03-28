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
import com.synapse.social.studioasinc.data.source.CommentLocalDataSource
import com.synapse.social.studioasinc.data.source.CommentRemoteDataSource
import com.synapse.social.studioasinc.shared.core.media.AndroidMediaCompressor
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.crypto.AndroidSignalProtocolManager
import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
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
import com.synapse.social.studioasinc.shared.data.repository.SupabaseNotificationRepository
import com.synapse.social.studioasinc.shared.data.repository.UserPreferencesRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.UserRepositoryImpl
import com.synapse.social.studioasinc.shared.data.source.remote.CloudinaryUploadService
import com.synapse.social.studioasinc.shared.data.source.remote.ImgBBUploadService
import com.synapse.social.studioasinc.shared.data.source.remote.R2UploadService
import com.synapse.social.studioasinc.shared.data.source.remote.SupabaseUploadService
import com.synapse.social.studioasinc.shared.domain.repository.AppRepository
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.BusinessRepository
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
import com.synapse.social.studioasinc.shared.domain.usecase.UploadMediaUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.ValidateProviderConfigUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.InitializeE2EUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowingUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.notification.GetNotificationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.notification.MarkNotificationAsReadUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.notification.SubscribeToNotificationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.DeletePostUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.TogglePostCommentsUseCase
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
object PostModule {

    @Provides
    @Singleton
    fun provideOfflineActionRepository(
        pendingActionDao: PendingActionDao
    ): OfflineActionRepository {
        return OfflineActionRepositoryImpl(pendingActionDao)
    }
    @Provides
    @Singleton
    fun providePostRepository(
        postDao: PostDao,
        client: SupabaseClientType,
        offlineActionRepository: OfflineActionRepository
    ): PostRepositoryImpl {
        return PostRepositoryImpl(postDao, client, offlineActionRepository)
    }
    @Provides
    @Singleton
    fun provideDomainPostRepository(postRepository: PostRepositoryImpl): com.synapse.social.studioasinc.domain.repository.PostRepository {
        return postRepository
    }
    @Provides
    @Singleton
    fun providePostActionsRepository(
        postRepository: PostRepositoryImpl
    ): PostActionsRepository {
        return postRepository
    }
    @Provides
    @Singleton
    fun providePostInteractionRepository(): com.synapse.social.studioasinc.domain.repository.PostInteractionRepository {
        return PostInteractionRepositoryImpl()
    }
    @Provides
    @Singleton
    fun provideProfileActionRepository(): ProfileActionRepositoryImpl {
        return ProfileActionRepositoryImpl()
    }
    @Provides
    @Singleton
    fun provideReactionRepository(client: SupabaseClientType): com.synapse.social.studioasinc.domain.repository.ReactionRepository {
        return ReactionRepositoryImpl(client)
    }
    @Provides
    @Singleton
    fun providePostDetailRepository(
        client: SupabaseClientType,
        reactionRepository: ReactionRepositoryImpl
    ): PostDetailRepositoryImpl {
        return PostDetailRepositoryImpl(client, reactionRepository as ReactionRepositoryImpl)
    }
    @Provides
    @Singleton
    fun provideCommentRemoteDataSource(client: SupabaseClientType): com.synapse.social.studioasinc.data.source.CommentRemoteDataSource {
        return CommentRemoteDataSource(client)
    }
    @Provides
    @Singleton
    fun provideCommentLocalDataSource(
        storageDatabase: StorageDatabase,
        commentDao: CommentDao
    ): com.synapse.social.studioasinc.data.source.CommentLocalDataSource {
        return CommentLocalDataSource(storageDatabase, commentDao)
    }
    @Provides
    @Singleton
    fun provideCommentRepository(
        localDataSource: com.synapse.social.studioasinc.data.source.CommentLocalDataSource,
        remoteDataSource: com.synapse.social.studioasinc.data.source.CommentRemoteDataSource,
        userRepository: com.synapse.social.studioasinc.data.repository.UserRepositoryImpl,
        reactionRepository: ReactionRepositoryImpl,
        @Named("ApplicationScope") externalScope: CoroutineScope
    ): CommentRepositoryImpl {
        return CommentRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            userRepository = userRepository,
            externalScope = externalScope,
            reactionRepository = reactionRepository as ReactionRepositoryImpl
        )
    }
    @Provides
    @Singleton
    fun providePollRepository(client: SupabaseClientType): com.synapse.social.studioasinc.domain.repository.PollRepository {
        return PollRepositoryImpl(client)
    }
    @Provides
    @Singleton
    fun provideConcretePollRepository(client: SupabaseClientType): com.synapse.social.studioasinc.data.repository.PollRepositoryImpl {
        return PollRepositoryImpl(client)
    }
    @Provides
    @Singleton
    fun provideBookmarkRepository(client: SupabaseClientType): BookmarkRepositoryImpl {
        return BookmarkRepositoryImpl(client)
    }
    @Provides
    @Singleton
    fun provideReshareRepository(client: SupabaseClientType): ReshareRepositoryImpl {
        return ReshareRepositoryImpl(client)
    }
    @Provides
    @Singleton
    fun provideReportRepository(client: SupabaseClientType): com.synapse.social.studioasinc.domain.repository.ReportRepository {
        return ReportRepositoryImpl(client)
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
    fun provideReelRepository(): ReelRepository {
        return ReelRepository()
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
    fun provideLinkPreviewRepository(
        database: StorageDatabase,
        httpClient: HttpClient
    ): LinkPreviewRepository {
        return LinkPreviewRepositoryImpl(database, httpClient)
    }
    @Provides
    @Singleton
    fun provideGetLinkMetadataUseCase(
        repository: LinkPreviewRepository
    ): GetLinkMetadataUseCase {
        return GetLinkMetadataUseCase(repository)
    }

}
