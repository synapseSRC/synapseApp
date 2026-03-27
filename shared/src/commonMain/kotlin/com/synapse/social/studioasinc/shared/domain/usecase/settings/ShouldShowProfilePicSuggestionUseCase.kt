package com.synapse.social.studioasinc.shared.domain.usecase.settings

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.UserRepository
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map


@OptIn(ExperimentalCoroutinesApi::class)
class ShouldShowProfilePicSuggestionUseCase constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> {
        return authRepository.sessionStatus.flatMapLatest { status ->
            if (status == AuthSessionStatus.AUTHENTICATED) {
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    settingsRepository.hideProfilePicSuggestion.map { hidePreference ->
                        if (hidePreference) {
                            false
                        } else {
                            val userResult = userRepository.getUserProfile(userId)
                            val user = userResult.getOrNull()
                            user?.avatar.isNullOrBlank()
                        }
                    }
                } else {
                    flowOf(false)
                }
            } else {
                flowOf(false)
            }
        }
    }
}
