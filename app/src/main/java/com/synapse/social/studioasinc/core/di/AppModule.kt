package com.synapse.social.studioasinc.core.di

import android.content.Context
import android.content.SharedPreferences
import com.synapse.social.studioasinc.data.local.AndroidPasskeyManager
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
object AppModule {

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
    fun providePasskeyManager(
        passkeyCredentialManager: com.synapse.social.studioasinc.ui.settings.PasskeyCredentialManager,
        @ApplicationContext context: android.content.Context
    ): com.synapse.social.studioasinc.domain.repository.PasskeyManager {
        return AndroidPasskeyManager(passkeyCredentialManager, context)
    }
    @Provides
    @Singleton
    fun provideMediaUploadHandler(
        impl: com.synapse.social.studioasinc.feature.createpost.createpost.handlers.MediaUploadHandler
    ): com.synapse.social.studioasinc.domain.repository.MediaUploadHandler = impl
    @Provides
    @Singleton
    fun provideReelSubmissionHandler(
        impl: com.synapse.social.studioasinc.feature.createpost.createpost.handlers.ReelSubmissionHandler
    ): com.synapse.social.studioasinc.domain.repository.ReelSubmissionHandler = impl

}
