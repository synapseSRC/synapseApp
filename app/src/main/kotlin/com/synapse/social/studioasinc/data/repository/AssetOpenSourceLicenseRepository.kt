package com.synapse.social.studioasinc.data.repository

import android.content.Context
import com.synapse.social.studioasinc.core.util.json
import com.synapse.social.studioasinc.data.model.OpenSourceLicensesAssetDto
import com.synapse.social.studioasinc.domain.model.OpenSourceLicense
import com.synapse.social.studioasinc.domain.repository.OpenSourceLicenseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

/**
 * Reads the bundled third-party licence notices from app assets, so the list is available
 * offline and never depends on a backend.
 */
class AssetOpenSourceLicenseRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : OpenSourceLicenseRepository {

    override suspend fun getLicenses(): Result<List<OpenSourceLicense>> = try {
        Result.success(withContext(Dispatchers.IO) { readLicenses() })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun readLicenses(): List<OpenSourceLicense> {
        val text = try {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        } catch (error: Exception) {
            throw IOException("Could not read the $ASSET_NAME asset", error)
        }
        val asset = try {
            json.decodeFromString<OpenSourceLicensesAssetDto>(text)
        } catch (error: SerializationException) {
            throw IOException("Licence asset is not valid JSON", error)
        }
        val licenses = asset.libraries
            .map { OpenSourceLicenseMapper.toDomain(it) }
            .filter { license ->
                license.name.isNotBlank() &&
                    license.author.isNotBlank() &&
                    license.licenseId.isNotBlank() &&
                    isWebUrl(license.repositoryUrl)
            }
            .distinctBy { it.name.lowercase() }
        // A truncated or emptied asset must surface as a failure state, not a silently blank screen.
        if (licenses.isEmpty()) throw IOException("Licence asset contains no usable entries")
        return licenses
    }

    private fun isWebUrl(value: String): Boolean = try {
        val uri = URI(value)
        (uri.scheme == "https" || uri.scheme == "http") && uri.host != null && uri.rawUserInfo == null
    } catch (error: IllegalArgumentException) {
        false
    }

    companion object {
        private const val ASSET_NAME = "about_open_source_licenses.json"
    }
}
