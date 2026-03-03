package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.domain.model.AppUpdateInfo
import com.synapse.social.studioasinc.shared.domain.repository.AppRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class AppRepositoryImpl(
    private val httpClient: HttpClient
) : AppRepository {
    override suspend fun getAppUpdateInfo(): Result<AppUpdateInfo> {
        return try {
            val response: AppUpdateInfo = httpClient.get("https://pastebin.com/raw/sQuaciVv").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
