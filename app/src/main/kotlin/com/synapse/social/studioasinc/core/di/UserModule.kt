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
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseUserDataSource
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
object UserModule {

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
    fun provideUserRepository(userDao: UserDao, client: SupabaseClientType): com.synapse.social.studioasinc.data.repository.UserRepositoryImpl {
        return UserRepositoryImpl(userDao, client)
    }
    @Provides
    @Singleton
    fun provideSharedUserRepository(
        storageDatabase: StorageDatabase,
        client: SupabaseClientType
    ): com.synapse.social.studioasinc.shared.domain.repository.UserRepository {
        val userDataSource = SupabaseUserDataSource(client)
        return UserRepositoryImpl(storageDatabase, userDataSource)
    }
    @Provides
    @Singleton
    fun provideUsernameRepository(): UsernameRepositoryImpl {
        return UsernameRepositoryImpl()
    }
    @Provides
    @Singleton
    fun provideProfileRepository(
        client: SupabaseClientType,
        commentRepository: CommentRepositoryImpl
    ): ProfileRepository {
        return ProfileRepositoryImpl(client, commentRepository)
    }
    @Provides
    @Singleton
    fun provideDomainProfileRepository(
        profileRepository: ProfileRepository
    ): com.synapse.social.studioasinc.domain.repository.ProfileRepository {
        return DomainProfileRepositoryAdapter(profileRepository)
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
    fun provideUserPreferencesRepository(
        client: io.github.jan.supabase.SupabaseClient
    ): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(client)
    }

}
