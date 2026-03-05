package com.synapse.social.studioasinc.shared.data.source.remote

import com.synapse.social.studioasinc.shared.data.PlatformUtils
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.UploadError
import com.synapse.social.studioasinc.shared.util.TimeProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(InternalAPI::class)
class CloudinaryUploadService(private val client: HttpClient) : UploadService {
    override suspend fun upload(
        fileProvider: suspend (Long) -> ByteReadChannel,
        fileSize: Long,
        fileName: String,
        config: StorageConfig,
        bucketName: String?,
        onProgress: (Float) -> Unit
    ): String {
        val cloudName = config.cloudinaryCloudName
        val apiKey = config.cloudinaryApiKey
        val apiSecret = config.cloudinaryApiSecret

        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw Exception("Cloudinary configuration is incomplete")
        }

        val resourceType = when {
            fileName.endsWith(".mp4", ignoreCase = true) ||
            fileName.endsWith(".mov", ignoreCase = true) ||
            fileName.endsWith(".avi", ignoreCase = true) ||
            fileName.endsWith(".mkv", ignoreCase = true) -> "video"
            else -> "auto"
        }

        val timestamp = (TimeProvider.nowMillis() / 1000).toString()
        val toSign = "timestamp=$timestamp$apiSecret"
        val signature = PlatformUtils.sha1(toSign)

        val fileChannel = fileProvider(0)

        return try {
            val response: JsonObject = client.submitFormWithBinaryData(
                url = "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload",
                formData = formData {
                    append("file", fileChannel)
                    append("api_key", apiKey)
                    append("timestamp", timestamp)
                    append("signature", signature)
                }
            ).body()

            onProgress(1.0f)

            response["secure_url"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content
                ?: throw Exception(extractCloudinaryError(response))
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.toString()
            println("CloudinaryUploadService error: $errorMsg")
            throw Exception("Cloudinary upload failed: $errorMsg")
        }
    }

    private fun extractCloudinaryError(response: JsonObject): String {
        val errorMessage = response["error"]
            ?.jsonObject
            ?.get("message")
            ?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }
            ?.content

        return errorMessage ?: "Cloudinary URL missing in upload response"
    }
}
