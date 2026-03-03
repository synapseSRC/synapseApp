package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.data.repository.AccountRepository
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _linkedAccounts = MutableStateFlow(LinkedAccountsState())
    val linkedAccounts: StateFlow<LinkedAccountsState> = _linkedAccounts.asStateFlow()

    private val _securityNotificationsEnabled = MutableStateFlow(true)
    val securityNotificationsEnabled: StateFlow<Boolean> = _securityNotificationsEnabled.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showChangeEmailDialog = MutableStateFlow(false)
    val showChangeEmailDialog: StateFlow<Boolean> = _showChangeEmailDialog.asStateFlow()

    private val _showChangePasswordDialog = MutableStateFlow(false)
    val showChangePasswordDialog: StateFlow<Boolean> = _showChangePasswordDialog.asStateFlow()

    private val _showDeleteAccountDialog = MutableStateFlow(false)
    val showDeleteAccountDialog: StateFlow<Boolean> = _showDeleteAccountDialog.asStateFlow()

    init {
        loadLinkedAccounts()
        loadSecurityNotificationsSettings()
    }

    private fun loadSecurityNotificationsSettings() {
        viewModelScope.launch {
            accountRepository.getSecurityNotificationsEnabled()
                .onSuccess { enabled ->
                    _securityNotificationsEnabled.value = enabled
                }
                .onFailure { e ->
                    android.util.Log.e("AccountSettingsViewModel", "Failed to load security notifications settings", e)
                }
        }
    }

    fun toggleSecurityNotifications(enabled: Boolean) {
        viewModelScope.launch {
            // Optimistic update
            _securityNotificationsEnabled.value = enabled

            accountRepository.setSecurityNotificationsEnabled(enabled)
                .onFailure { e ->
                    // Revert on failure
                    _securityNotificationsEnabled.value = !enabled
                    android.util.Log.e("AccountSettingsViewModel", "Failed to update security notifications", e)
                    _error.value = "Failed to update security notifications"
                }
        }
    }

    private fun loadLinkedAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            accountRepository.getLinkedIdentities()
                .onSuccess { providers ->
                    _linkedAccounts.value = LinkedAccountsState(
                        googleLinked = providers.contains("google"),
                        facebookLinked = providers.contains("facebook"),
                        appleLinked = providers.contains("apple")
                    )
                    android.util.Log.d("AccountSettingsViewModel", "Loaded ${providers.size} linked accounts")
                }
                .onFailure { e ->
                    android.util.Log.e("AccountSettingsViewModel", "Failed to load linked accounts", e)
                    _error.value = "Failed to load linked accounts: ${e.message}"
                }
            _isLoading.value = false
        }
    }

    fun connectSocialAccount(provider: SocialProvider) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            accountRepository.linkIdentity(provider)
                .onSuccess {
                    loadLinkedAccounts()
                }
                .onFailure { e ->
                    android.util.Log.e("AccountSettingsViewModel", "Failed to connect $provider", e)
                    _error.value = "Failed to connect ${provider.displayName}: ${e.message}"
                }

            _isLoading.value = false
        }
    }

    fun disconnectSocialAccount(provider: SocialProvider) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            accountRepository.unlinkIdentity(provider)
                .onSuccess {
                    loadLinkedAccounts()
                }
                .onFailure { e ->
                    android.util.Log.e("AccountSettingsViewModel", "Failed to disconnect $provider", e)
                    _error.value = "Failed to disconnect ${provider.displayName}: ${e.message}"
                }

            _isLoading.value = false
        }
    }

    fun showChangeEmailDialog() {
        _showChangeEmailDialog.value = true
    }

    fun dismissChangeEmailDialog() {
        _showChangeEmailDialog.value = false
        _error.value = null
    }

    fun changeEmail(newEmail: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            if (!isValidEmail(newEmail)) {
                _error.value = "Invalid email format"
                _isLoading.value = false
                return@launch
            }

            if (password.isBlank()) {
                _error.value = "Password is required"
                _isLoading.value = false
                return@launch
            }

            val currentEmail = authRepository.getCurrentUserEmail()
            if (currentEmail == null) {
                _error.value = "User email not found. Please sign in again."
                _isLoading.value = false
                return@launch
            }

            // Verify password by re-authenticating
            authRepository.signIn(currentEmail, password)
                .onSuccess {
                    android.util.Log.d("AccountSettingsViewModel", "Password verified, initiating email change")
                    accountRepository.changeEmail(newEmail)
                        .onSuccess {
                            _showChangeEmailDialog.value = false
                        }
                        .onFailure { e ->
                            android.util.Log.e("AccountSettingsViewModel", "Failed to change email", e)
                            _error.value = "Failed to change email: ${e.message}"
                        }
                }
                .onFailure { e ->
                    android.util.Log.e("AccountSettingsViewModel", "Failed to verify password", e)
                    if (e?.message?.contains("invalid", ignoreCase = true) == true ||
                        e?.message?.contains("credential", ignoreCase = true) == true) {
                        _error.value = "Incorrect password"
                    } else {
                        _error.value = "Failed to verify password: ${e?.message}"
                    }
                }

            _isLoading.value = false
        }
    }

    fun showChangePasswordDialog() {
        _showChangePasswordDialog.value = true
    }

    fun dismissChangePasswordDialog() {
        _showChangePasswordDialog.value = false
        _error.value = null
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            if (currentPassword.isBlank()) {
                _error.value = "Current password is required"
                _isLoading.value = false
                return@launch
            }

            if (newPassword.length < 8) {
                _error.value = "New password must be at least 8 characters"
                _isLoading.value = false
                return@launch
            }

            if (newPassword != confirmPassword) {
                _error.value = "Passwords do not match"
                _isLoading.value = false
                return@launch
            }

            val email = authRepository.getCurrentUserEmail()
            if (email == null) {
                _error.value = "User email not found. Please sign in again."
                _isLoading.value = false
                return@launch
            }

            // Verify current password
            authRepository.signIn(email, currentPassword)
                .onSuccess {
                    authRepository.updatePassword(newPassword)
                        .onSuccess {
                            android.util.Log.d("AccountSettingsViewModel", "Password changed successfully")
                            _showChangePasswordDialog.value = false
                        }
                        .onFailure { e ->
                            android.util.Log.e("AccountSettingsViewModel", "Failed to change password", e)
                            _error.value = "Failed to change password: ${e.message}"
                        }
                }
                .onFailure { e ->
                    android.util.Log.e("AccountSettingsViewModel", "Failed to verify password", e)
                    if (e.message?.contains("invalid", ignoreCase = true) == true ||
                        e.message?.contains("credential", ignoreCase = true) == true) {
                        _error.value = "Incorrect current password"
                    } else {
                        _error.value = "Failed to verify password: ${e?.message}"
                    }
                }

            _isLoading.value = false
        }
    }

    fun calculatePasswordStrength(password: String): Int {
        var strength = 0
        if (password.length >= 8) strength++
        if (password.length >= 12) strength++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) strength++
        if (password.any { it.isDigit() }) strength++
        if (password.any { !it.isLetterOrDigit() }) strength++
        return strength.coerceIn(0, 4)
    }

    fun showDeleteAccountDialog() {
        _showDeleteAccountDialog.value = true
    }

    fun dismissDeleteAccountDialog() {
        _showDeleteAccountDialog.value = false
        _error.value = null
    }

    fun deleteAccount(confirmationText: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            if (confirmationText != DELETE_ACCOUNT_CONFIRMATION) {
                _error.value = "Please type the exact confirmation phrase"
                _isLoading.value = false
                return@launch
            }

            android.util.Log.d("AccountSettingsViewModel", "Deleting account")

            accountRepository.deleteAccount()
                .onSuccess {
                    _showDeleteAccountDialog.value = false
                }
                .onFailure { e ->
                    android.util.Log.e("AccountSettingsViewModel", "Failed to delete account", e)
                    _error.value = "Failed to delete account: ${e.message}"
                }

            _isLoading.value = false
        }
    }

    private fun isValidEmail(email: String): Boolean {
        // Simple regex for email validation
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        return emailRegex.matches(email)
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        const val DELETE_ACCOUNT_CONFIRMATION = "DELETE MY ACCOUNT"
    }
}

data class LinkedAccountsState(
    val googleLinked: Boolean = false,
    val facebookLinked: Boolean = false,
    val appleLinked: Boolean = false
)
