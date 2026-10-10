package com.synapse.social.studioasinc.shared.domain.usecase

import com.synapse.social.studioasinc.shared.domain.exception.NetworkUnavailableException
import com.synapse.social.studioasinc.shared.domain.model.AppUpdateInfo
import com.synapse.social.studioasinc.shared.domain.repository.AppRepository
import com.synapse.social.studioasinc.shared.domain.repository.PlatformInfoProvider

class CheckForUpdatesUseCase(
    private val appRepository: AppRepository,
    private val platformInfoProvider: PlatformInfoProvider
) {
    suspend operator fun invoke(): Result<AppUpdateInfo?> {
        if (!platformInfoProvider.isNetworkAvailable()) {
            return Result.failure(NetworkUnavailableException())
        }
        val currentVersionCode = platformInfoProvider.getAppVersionCode()
        return appRepository.getAppUpdateInfo().map { updateInfo ->
            if (updateInfo.versionCode.toInt() > currentVersionCode) {
                updateInfo
            } else {
                null
            }
        }
    }
}
