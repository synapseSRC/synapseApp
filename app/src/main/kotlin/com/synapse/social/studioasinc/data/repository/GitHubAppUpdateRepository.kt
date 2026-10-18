package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.AppReleaseInfo
import com.synapse.social.studioasinc.domain.repository.AppUpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.inject.Inject

class GitHubAppUpdateRepository @Inject constructor() : AppUpdateRepository {
    override suspend fun getLatestRelease(): Result<AppReleaseInfo> = try {
        Result.success(withContext(Dispatchers.IO) { fetchLatestRelease() })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun fetchLatestRelease(): AppReleaseInfo {
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
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name").trim()
            val page = json.optString("html_url").trim()
            if (tag.isBlank() || page.isBlank()) throw IOException("Release response is incomplete")
            val releaseUri = URI(page)
            val releaseHost = releaseUri.host ?: throw IOException("Unexpected release URL host")
            val releasePath = releaseUri.path ?: throw IOException("Unexpected release URL path")
            if (releaseUri.scheme != "https" ||
                !releaseHost.equals("github.com", ignoreCase = true) ||
                (releaseUri.port != -1 && releaseUri.port != 443) ||
                releaseUri.rawUserInfo != null ||
                !releasePath.startsWith(RELEASE_PATH_PREFIX)
            ) throw IOException("Unexpected release URL host")
            return AppReleaseInfo(
                version = tag.removePrefix("v").removePrefix("V"),
                releaseUrl = page
            )
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val UPDATE_ENDPOINT = "https://api.github.com/repos/synapseSRC/synapseApp/releases/latest"
        private const val RELEASE_PATH_PREFIX = "/synapseSRC/synapseApp/releases/"
        private const val REQUEST_TIMEOUT_MILLIS = 8_000
    }
}
