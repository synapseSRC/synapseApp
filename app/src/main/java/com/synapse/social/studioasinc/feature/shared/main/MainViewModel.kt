package com.synapse.social.studioasinc.feature.shared.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.ui.navigation.AppDestination
import com.synapse.social.studioasinc.domain.usecase.update.GetUpdateStateUseCase
import com.synapse.social.studioasinc.domain.usecase.update.UpdateState
import com.synapse.social.studioasinc.shared.domain.model.auth.AuthSessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val getUpdateStateUseCase: GetUpdateStateUseCase
) : ViewModel() {

    private val _updateState = MutableLiveData<UpdateState>()
    val updateState: LiveData<UpdateState> = _updateState

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _isCheckingAuth = MutableStateFlow(true)
    val isCheckingAuth = _isCheckingAuth.asStateFlow()

    private val _startDestination = MutableStateFlow<Any>(AppDestination.Auth)
    val startDestination = _startDestination.asStateFlow()

    init {
        checkUserAuthentication()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = getUpdateStateUseCase()
        }
    }

    fun checkUserAuthentication() {
        viewModelScope.launch {
            try {

                // Wait for session to be fully loaded
                authRepository.sessionStatus.first { it == AuthSessionStatus.AUTHENTICATED || it == AuthSessionStatus.NOT_AUTHENTICATED }
                val isAuthenticated = authRepository.restoreSession()

                if (isAuthenticated) {
                    val userId = authRepository.getCurrentUserId()
                    val userEmail = authRepository.getCurrentUserEmail()

                    if (userId != null && !userEmail.isNullOrBlank()) {
                         userRepository.getUserById(userId)
                            .onSuccess { user ->
                                if (user != null) {
                                    if (!user.banned) {
                                        _authState.value = AuthState.Authenticated
                                        _startDestination.value = AppDestination.Home
                                    } else {
                                        _authState.value = AuthState.Banned


                                        _startDestination.value = AppDestination.Auth
                                    }
                                } else {

                                    _authState.value = AuthState.Authenticated
                                    _startDestination.value = AppDestination.Home
                                }
                            }
                            .onFailure {
                                _authState.value = AuthState.Authenticated
                                _startDestination.value = AppDestination.Home
                            }
                    } else {
                         _authState.value = AuthState.Unauthenticated
                         _startDestination.value = AppDestination.Auth
                    }
                } else {
                    _authState.value = AuthState.Unauthenticated
                    _startDestination.value = AppDestination.Auth
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Database error: ${e.message}")
                _startDestination.value = AppDestination.Auth
            } finally {
                _isCheckingAuth.value = false
            }
        }
    }
}

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Banned : AuthState()
    data class Error(val message: String) : AuthState()
}
