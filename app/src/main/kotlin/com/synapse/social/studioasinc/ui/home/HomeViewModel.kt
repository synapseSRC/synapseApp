package com.synapse.social.studioasinc.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetCurrentUserAvatarUseCase
import com.synapse.social.studioasinc.ui.settings.NavigationPlacement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUserAvatarUseCase: GetCurrentUserAvatarUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val navigationPlacement: StateFlow<NavigationPlacement> = settingsRepository.navigationPlacement
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.synapse.social.studioasinc.data.local.database.settings.SettingsConstants.DEFAULT_NAVIGATION_PLACEMENT
        )

    private val _userAvatarUrl = MutableStateFlow<String?>(null)
    val userAvatarUrl: StateFlow<String?> = _userAvatarUrl.asStateFlow()

    init {
        loadUserAvatar()
    }

    private fun loadUserAvatar() {
        viewModelScope.launch {
            getCurrentUserAvatarUseCase().onSuccess { avatar ->
                _userAvatarUrl.value = avatar
            }
        }
    }
}
