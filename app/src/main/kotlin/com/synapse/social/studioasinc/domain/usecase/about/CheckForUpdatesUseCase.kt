package com.synapse.social.studioasinc.domain.usecase.about

import com.synapse.social.studioasinc.domain.model.AppUpdateCheckResult
import com.synapse.social.studioasinc.domain.model.release.ReleaseVersion
import com.synapse.social.studioasinc.domain.repository.AppUpdateRepository
import javax.inject.Inject

class CheckForUpdatesUseCase @Inject constructor(
    private val repository: AppUpdateRepository
) {
    suspend operator fun invoke(installedVersion: String): Result<AppUpdateCheckResult> =
        repository.getLatestRelease().map { release ->
            val latest = ReleaseVersion.of(release.version)
            val installed = ReleaseVersion.of(installedVersion)
            if (latest != null && installed != null && latest > installed) {
                AppUpdateCheckResult.UpdateAvailable(release)
            } else {
                AppUpdateCheckResult.UpToDate
            }
        }
}
