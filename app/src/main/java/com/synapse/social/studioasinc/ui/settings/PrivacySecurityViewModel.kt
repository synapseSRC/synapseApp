package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.biometric.BiometricManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.core.util.BiometricChecker
import com.synapse.social.studioasinc.core.util.BiometricCheckerImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch



class PrivacySecurityViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val biometricChecker: BiometricChecker = BiometricCheckerImpl()

    private val settingsRepository = SettingsRepositoryImpl.getInstance(application)





    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings: StateFlow<PrivacySettings> = _privacySettings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()


    private val _show2FASetupDialog = MutableStateFlow(false)
    val show2FASetupDialog: StateFlow<Boolean> = _show2FASetupDialog.asStateFlow()

    private val _twoFactorSetupState = MutableStateFlow(TwoFactorSetupState())
    val twoFactorSetupState: StateFlow<TwoFactorSetupState> = _twoFactorSetupState.asStateFlow()

    init {
        loadPrivacySettings()
    }







    private fun loadPrivacySettings() {
        viewModelScope.launch {
            try {
                settingsRepository.privacySettings.collect { settings ->
                    _privacySettings.value = settings
                }
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to load privacy settings", e)
                _error.value = "Failed to load privacy settings"
            }
        }
    }



    fun setProfileVisibility(visibility: ProfileVisibility) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setProfileVisibility(visibility)
                android.util.Log.d("PrivacySecurityViewModel", "Profile visibility set to: $visibility")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to set profile visibility", e)
                _error.value = "Failed to update profile visibility"
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun setContentVisibility(visibility: ContentVisibility) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setContentVisibility(visibility)
                android.util.Log.d("PrivacySecurityViewModel", "Content visibility set to: $visibility")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to set content visibility", e)
                _error.value = "Failed to update content visibility"
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun setGroupPrivacy(privacy: GroupPrivacy) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setGroupPrivacy(privacy)
                android.util.Log.d("PrivacySecurityViewModel", "Group privacy set to: $privacy")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to set group privacy", e)
                _error.value = "Failed to update group privacy"
            } finally {
                _isLoading.value = false
            }
        }
    }







    fun setTwoFactorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (enabled) {

                    _show2FASetupDialog.value = true
                    _twoFactorSetupState.value = TwoFactorSetupState()
                } else {

                    settingsRepository.setTwoFactorEnabled(false)
                    android.util.Log.d("PrivacySecurityViewModel", "Two-factor authentication disabled")
                }
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to toggle 2FA", e)
                _error.value = "Failed to update two-factor authentication"
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun generate2FASecret() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserEmail = com.synapse.social.studioasinc.core.auth.AuthHelper.getCurrentUserEmail()

                if (currentUserEmail != null) {

                    val secret = generateBase32Secret()
                    val qrCodeUrl = "otpauth://totp/Synapse:${currentUserEmail}?secret=$secret&issuer=Synapse"

                    _twoFactorSetupState.value = _twoFactorSetupState.value.copy(
                        secret = secret,
                        qrCodeUrl = qrCodeUrl,
                        step = TwoFactorSetupStep.SCAN_QR
                    )
                } else {
                    _error.value = "User email not found"
                }
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to generate 2FA secret", e)
                _error.value = "Failed to generate 2FA secret"
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun verify2FACode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (code.length != 6 || !code.all { it.isDigit() }) {
                    _error.value = "Please enter a valid 6-digit code"
                    return@launch
                }



                settingsRepository.setTwoFactorEnabled(true)

                _twoFactorSetupState.value = _twoFactorSetupState.value.copy(
                    step = TwoFactorSetupStep.COMPLETED
                )

                android.util.Log.d("PrivacySecurityViewModel", "Two-factor authentication enabled successfully")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to verify 2FA code", e)
                _error.value = "Failed to verify code"
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun dismiss2FASetupDialog() {
        _show2FASetupDialog.value = false
        _twoFactorSetupState.value = TwoFactorSetupState()
    }



    private fun generateBase32Secret(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        return (1..32).map { chars.random() }.joinToString("")
    }



    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (enabled) {
                    val canAuthenticate = biometricChecker.canAuthenticate(getApplication())

                    when (canAuthenticate) {
                        BiometricManager.BIOMETRIC_SUCCESS -> {

                        }
                        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                            _error.value = "No biometric features available on this device."
                            _isLoading.value = false
                            return@launch
                        }
                        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                            _error.value = "Biometric features are currently unavailable."
                            _isLoading.value = false
                            return@launch
                        }
                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                            _error.value = "No biometric credentials enrolled on this device."
                            _isLoading.value = false
                            return@launch
                        }
                        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                            _error.value = "Security update required for biometric features."
                            _isLoading.value = false
                            return@launch
                        }
                        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                            _error.value = "Biometric features are unsupported."
                            _isLoading.value = false
                            return@launch
                        }
                        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                            _error.value = "Biometric status unknown."
                            _isLoading.value = false
                            return@launch
                        }
                    }
                }

                settingsRepository.setBiometricLockEnabled(enabled)
                android.util.Log.d("PrivacySecurityViewModel", "Biometric lock ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to toggle biometric lock", e)
                _error.value = "Failed to update biometric lock"
            } finally {
                _isLoading.value = false
            }
        }
    }







    fun navigateToBlockedUsers() {
        android.util.Log.d("PrivacySecurityViewModel", "Navigate to blocked users")

    }



    fun navigateToMutedUsers() {
        android.util.Log.d("PrivacySecurityViewModel", "Navigate to muted users")

    }



    fun navigateToActiveSessions() {
        android.util.Log.d("PrivacySecurityViewModel", "Navigate to active sessions")

    }







    fun setReadReceiptsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setReadReceiptsEnabled(enabled)
                android.util.Log.d("PrivacySecurityViewModel", "Read receipts ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to toggle read receipts", e)
                _error.value = "Failed to update read receipts"
            } finally {
                _isLoading.value = false
            }
        }
    }







    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // TODO: Implement app lock functionality
                //  - Add biometric authentication on app launch
                //  - Add PIN/Pattern fallback option
                //  - Store lock preference securely
                //  - Handle lock timeout settings
                settingsRepository.setAppLockEnabled(enabled)
                android.util.Log.d("PrivacySecurityViewModel", "App lock ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to toggle app lock", e)
                _error.value = "Failed to update app lock"
            } finally {
                _isLoading.value = false
            }
        }
    }







    fun setChatLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // TODO: Implement chat lock functionality
                //  - Add per-chat lock capability
                //  - Require biometric/PIN to open locked chats
                //  - Add lock icon indicator in chat list
                //  - Store locked chat IDs securely
                //  - Handle lock timeout for individual chats
                settingsRepository.setChatLockEnabled(enabled)
                android.util.Log.d("PrivacySecurityViewModel", "Chat lock ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to toggle chat lock", e)
                _error.value = "Failed to update chat lock"
            } finally {
                _isLoading.value = false
            }
        }
    }







    fun clearError() {
        _error.value = null
    }
}



data class TwoFactorSetupState(
    val step: TwoFactorSetupStep = TwoFactorSetupStep.INITIAL,
    val secret: String = "",
    val qrCodeUrl: String = ""
)



enum class TwoFactorSetupStep {
    INITIAL,
    SCAN_QR,
    VERIFY_CODE,
    COMPLETED
}
