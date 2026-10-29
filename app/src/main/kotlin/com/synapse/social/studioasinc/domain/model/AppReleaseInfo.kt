package com.synapse.social.studioasinc.domain.model

data class AppReleaseInfo(val version: String, val releaseUrl: String)

sealed interface AppUpdateCheckResult {
    data object UpToDate : AppUpdateCheckResult
    data class UpdateAvailable(val release: AppReleaseInfo) : AppUpdateCheckResult
}
