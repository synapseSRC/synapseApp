package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.AppUpdateInfo

interface AppRepository {
    suspend fun getAppUpdateInfo(): Result<AppUpdateInfo>
}
