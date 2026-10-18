package com.synapse.social.studioasinc.domain.usecase.about

import com.synapse.social.studioasinc.domain.model.AppUpdateCheckResult
import com.synapse.social.studioasinc.domain.repository.AppUpdateRepository
import javax.inject.Inject

class CheckForUpdatesUseCase @Inject constructor(
    private val repository: AppUpdateRepository
) {
    suspend operator fun invoke(installedVersion: String): Result<AppUpdateCheckResult> =
        repository.getLatestRelease().map { release ->
            if (compareVersions(release.version, installedVersion) > 0) {
                AppUpdateCheckResult.UpdateAvailable(release)
            } else {
                AppUpdateCheckResult.UpToDate
            }
        }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = numericParts(left)
        val rightParts = numericParts(right)
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            val comparison = (leftParts.getOrNull(index) ?: 0).compareTo(rightParts.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun numericParts(version: String): List<Int> = version
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('-')
        .substringBefore('+')
        .split('.')
        .map { it.toIntOrNull() ?: 0 }
}
