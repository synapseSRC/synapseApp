package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface AboutUpdateStatus {
    data object Idle : AboutUpdateStatus
    data object Checking : AboutUpdateStatus
    data object UpToDate : AboutUpdateStatus
    data object Unavailable : AboutUpdateStatus
    data class UpdateAvailable(val version: String, val releaseUrl: String) : AboutUpdateStatus
}

class AboutSupportViewModel(application: Application) : AndroidViewModel(application) {
    private val _appVersion = MutableStateFlow("")
    val appVersion: StateFlow<String> = _appVersion.asStateFlow()

    private val _buildNumber = MutableStateFlow("")
    val buildNumber: StateFlow<String> = _buildNumber.asStateFlow()

    private val _updateStatus = MutableStateFlow<AboutUpdateStatus>(AboutUpdateStatus.Idle)
    val updateStatus: StateFlow<AboutUpdateStatus> = _updateStatus.asStateFlow()

    init { loadAppInfo() }

    private fun loadAppInfo() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
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
            }.onSuccess { (version, build) ->
                _appVersion.value = version
                _buildNumber.value = build
            }.onFailure {
                val unknown = getApplication<Application>().getString(R.string.about_version_unknown)
                _appVersion.value = unknown
                _buildNumber.value = unknown
            }
        }
    }

    fun checkForUpdates() {
        if (_updateStatus.value == AboutUpdateStatus.Checking) return
        _updateStatus.value = AboutUpdateStatus.Checking
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { fetchLatestRelease() to currentInstalledVersion() } }
            _updateStatus.value = result.fold(
                onSuccess = { (latest, installed) ->
                    if (compareVersions(latest.version, installed) > 0) {
                        AboutUpdateStatus.UpdateAvailable(latest.version, latest.url)
                    } else {
                        AboutUpdateStatus.UpToDate
                    }
                },
                onFailure = { AboutUpdateStatus.Unavailable }
            )
        }
    }

    private fun currentInstalledVersion(): String = runCatching {
        val context = getApplication<Application>()
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
    }.getOrDefault("0.0.0")

    private fun fetchLatestRelease(): GitHubRelease {
        val connection = (URL(UPDATE_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = REQUEST_TIMEOUT_MILLIS
            readTimeout = REQUEST_TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Synapse-Android")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Release endpoint returned HTTP " + connection.responseCode)
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").trim()
            val page = json.optString("html_url").trim()
            if (tag.isBlank() || page.isBlank()) throw IOException("Release response is incomplete")
            return GitHubRelease(version = tag.removePrefix("v").removePrefix("V"), url = page)
        } finally {
            connection.disconnect()
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = left.substringBefore('-').substringBefore('+').split('.').map { it.toIntOrNull() ?: 0 }
        val rightParts = right.substringBefore('-').substringBefore('+').split('.').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            val comparison = (leftParts.getOrNull(index) ?: 0).compareTo(rightParts.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        return 0
    }

    fun getTermsOfServiceUrl(): String = "${APP_WEBSITE_URL}/reference/terms/"
    fun getPrivacyPolicyUrl(): String = "${APP_WEBSITE_URL}/reference/privacy/"

    private data class GitHubRelease(val version: String, val url: String)

    companion object {
        val APP_WEBSITE_URL: String = SynapseConfig.APP_WEBSITE_URL
        private val UPDATE_ENDPOINT: String = SynapseConfig.GITHUB_BUG_REPORT_URL.substringBefore("/issues").replace("https://github.com/", "https://api.github.com/repos/") + "/releases/latest"
        private const val REQUEST_TIMEOUT_MILLIS = 8_000
    }
}
