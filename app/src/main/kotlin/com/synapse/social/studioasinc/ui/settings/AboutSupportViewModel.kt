package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.AppUpdateCheckResult
import com.synapse.social.studioasinc.domain.usecase.about.CheckForUpdatesUseCase
import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface AboutUpdateStatus {
    data object Idle : AboutUpdateStatus
    data object Checking : AboutUpdateStatus
    data object UpToDate : AboutUpdateStatus
    data object Unavailable : AboutUpdateStatus
    data class UpdateAvailable(val version: String, val releaseUrl: String) : AboutUpdateStatus
}

@HiltViewModel
class AboutSupportViewModel @Inject constructor(
    application: Application,
    private val checkForUpdatesUseCase: CheckForUpdatesUseCase
) : AndroidViewModel(application) {
    private val _appVersion = MutableStateFlow("")
    val appVersion: StateFlow<String> = _appVersion.asStateFlow()

    private val _buildNumber = MutableStateFlow("")
    val buildNumber: StateFlow<String> = _buildNumber.asStateFlow()

    /** Short commit hash stamped into [BuildConfig] at build time; blank when git was unavailable. */
    private val _commitHash = MutableStateFlow(BuildConfig.GIT_COMMIT_HASH.trim())
    val commitHash: StateFlow<String> = _commitHash.asStateFlow()

    private val _updateStatus = MutableStateFlow<AboutUpdateStatus>(AboutUpdateStatus.Idle)
    val updateStatus: StateFlow<AboutUpdateStatus> = _updateStatus.asStateFlow()

    init { loadAppInfo() }

    private fun loadAppInfo() {
        viewModelScope.launch {
            try {
                val (version, build) = readAppInfo()
                _appVersion.value = version
                _buildNumber.value = build
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val unknown = getApplication<Application>().getString(R.string.about_version_unknown)
                _appVersion.value = unknown
                _buildNumber.value = unknown
            }
        }
    }

    private suspend fun readAppInfo(): Pair<String, String> = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val version = info.versionName ?: context.getString(R.string.about_version_unknown)
        val build = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toString()
        }
        version to build
    }

    fun checkForUpdates() {
        if (_updateStatus.value == AboutUpdateStatus.Checking) return
        _updateStatus.value = AboutUpdateStatus.Checking
        viewModelScope.launch {
            try {
                val installedVersion = _appVersion.value.ifBlank {
                    readAppInfo().also { (version, build) ->
                        _appVersion.value = version
                        _buildNumber.value = build
                    }.first
                }
                _updateStatus.value = checkForUpdatesUseCase(installedVersion).fold(
                    onSuccess = { result ->
                        when (result) {
                            AppUpdateCheckResult.UpToDate -> AboutUpdateStatus.UpToDate
                            is AppUpdateCheckResult.UpdateAvailable -> AboutUpdateStatus.UpdateAvailable(
                                version = result.release.version,
                                releaseUrl = result.release.releaseUrl
                            )
                        }
                    },
                    onFailure = { AboutUpdateStatus.Unavailable }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _updateStatus.value = AboutUpdateStatus.Unavailable
            }
        }
    }

    fun getTermsOfServiceUrl(): String = "$APP_WEBSITE_URL/reference/terms/"
    fun getPrivacyPolicyUrl(): String = "$APP_WEBSITE_URL/reference/privacy/"

    companion object {
        val APP_WEBSITE_URL: String = SynapseConfig.APP_WEBSITE_URL
    }
}
