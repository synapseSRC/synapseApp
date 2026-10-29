package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignOutViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private var isSigningOut = false

    fun signOut(onSignedOut: () -> Unit) {
        if (isSigningOut) return
        isSigningOut = true
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }
}
