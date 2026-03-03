package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch



class AboutSupportViewModel(
    application: Application
) : AndroidViewModel(application) {





    private val _appVersion = MutableStateFlow("")
    val appVersion: StateFlow<String> = _appVersion.asStateFlow()

    private val _buildNumber = MutableStateFlow("")
    val buildNumber: StateFlow<String> = _buildNumber.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadAppInfo()
    }







    private fun loadAppInfo() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                )

                _appVersion.value = packageInfo.versionName ?: "Unknown"


                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toString()
                }

                _buildNumber.value = versionCode

                android.util.Log.d(
                    "AboutSupportViewModel",
                    "App version: ${_appVersion.value}, Build: ${_buildNumber.value}"
                )
            } catch (e: PackageManager.NameNotFoundException) {
                android.util.Log.e("AboutSupportViewModel", "Failed to load app info", e)
                _appVersion.value = "Unknown"
                _buildNumber.value = "Unknown"
            }
        }
    }







    fun getTermsOfServiceUrl(): String {
        return URL_TERMS_OF_SERVICE
    }



    fun getPrivacyPolicyUrl(): String {
        return URL_PRIVACY_POLICY
    }



    fun navigateToHelpCenter() {
        android.util.Log.d("AboutSupportViewModel", "Navigate to Help Center")

    }



    fun getHelpCenterUrl(): String {
        return URL_HELP_CENTER
    }



    fun navigateToLicenses() {
        android.util.Log.d("AboutSupportViewModel", "Navigate to Open Source Licenses (placeholder)")

    }







    fun clearError() {
        _error.value = null
    }



    fun clearMessage() {
        _message.value = null
    }



    fun getFullVersionString(): String {
        return "${_appVersion.value} (${_buildNumber.value})"
    }

    companion object {
        private const val URL_TERMS_OF_SERVICE = "https://synapsesocial.vercel.app/reference/terms/"
        private const val URL_PRIVACY_POLICY = "https://synapsesocial.vercel.app/reference/privacy/"
        private const val URL_HELP_CENTER = "https://synapsesocial.vercel.app/"
    }

}
