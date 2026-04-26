package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.data.repository.UserRepositoryImpl
import com.synapse.social.studioasinc.shared.data.repository.UserPreferencesRepositoryImpl
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.UpdateProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.UnblockUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.GetBlockedUsersUseCase
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.BlockedUser
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object IOSDependencies : KoinComponent {
    // Inject lazily so it doesn't crash if accessed before startKoin
    private val database: com.synapse.social.studioasinc.shared.data.database.StorageDatabase by inject()

    fun getUserRepository() = UserRepositoryImpl(database, com.synapse.social.studioasinc.shared.data.datasource.SupabaseUserDataSource(SupabaseClient.client))
    fun getUserPreferencesRepository() = UserPreferencesRepositoryImpl(SupabaseClient.client)

    // Inject StorageRepository from Koin
    fun getAuthRepository(): AuthRepository = getKoin().get()
    fun getStorageRepository(): com.synapse.social.studioasinc.shared.domain.repository.StorageRepository = getKoin().get()

    // Create use cases on demand
    private fun getProfileUseCase() = GetUserProfileUseCase(getUserRepository())
    private fun getUpdateProfileUseCase() = UpdateProfileUseCase(getUserRepository())

    // iOS-specific wrappers that unwrap kotlin.Result for Swift async throws bridging
    @Throws(Exception::class)
    suspend fun fetchUserProfile(uid: String): User? {
        val result = getProfileUseCase()(uid)
        return result.getOrElse { throw it }
    }

    @Throws(Exception::class)
    suspend fun saveUserProfile(uid: String, updates: Map<String, Any?>): Boolean {
        val result = getUpdateProfileUseCase()(uid, updates)
        return result.getOrElse { throw it }
    }
}
