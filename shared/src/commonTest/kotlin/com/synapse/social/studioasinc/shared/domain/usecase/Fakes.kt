package com.synapse.social.studioasinc.shared.domain.usecase

import com.synapse.social.studioasinc.shared.domain.model.AppUpdateInfo
import com.synapse.social.studioasinc.shared.domain.repository.AppRepository
import com.synapse.social.studioasinc.shared.domain.repository.PlatformInfoProvider

class FakeAppRepository : AppRepository {
    var updateInfoResult: Result<AppUpdateInfo> = Result.success(AppUpdateInfo())

    override suspend fun getAppUpdateInfo(): Result<AppUpdateInfo> {
        return updateInfoResult
    }
}

class FakePlatformInfoProvider : PlatformInfoProvider {
    var networkAvailableFlag: Boolean = true
    var currentAppVersionCode: Int = 1

    override fun getAppVersionCode(): Int {
        return currentAppVersionCode
    }

    override fun isNetworkAvailable(): Boolean {
        return networkAvailableFlag
    }
}
