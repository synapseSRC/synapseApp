package com.synapse.social.studioasinc.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.repository.UserRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetCurrentUserAvatarUseCase

class HomeViewModelFactory(
    private val database: StorageDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val repository = UserRepositoryImpl(database, com.synapse.social.studioasinc.shared.data.datasource.SupabaseUserDataSource(SupabaseClient.client))
            val useCase = GetCurrentUserAvatarUseCase(repository)
            return HomeViewModel(useCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
