package com.synapse.social.studioasinc.domain.repository

import com.synapse.social.studioasinc.domain.model.AppReleaseInfo

interface AppUpdateRepository {
    suspend fun getLatestRelease(): Result<AppReleaseInfo>
}
