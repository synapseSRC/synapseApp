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
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.InternalAPI
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(InternalAPI::class)
class CloudinaryUploadService(private val client: HttpClient) : UploadService {
    
    private suspend fun ByteReadChannel.toByteArray(): ByteArray {
        val buffer = mutableListOf<Byte>()
        val temp = ByteArray(8192)
        while (!isClosedForRead) {
            val read = readAvailable(temp)
            if (read > 0) {
                buffer.addAll(temp.take(read))
            }
        }
        return buffer.toByteArray()
    }
    
    override suspend fun upload(
        fileProvider: suspend (Long) -> ByteReadChannel,
        fileSize: Long,
        fileName: String,
        config: StorageConfig,
        bucketName: String?,
        onProgress: (Float) -> Unit
    ): String {
        val cloudName = config.cloudinaryCloudName
        val apiKey = config.cloudinaryApiKey.trim()
        val apiSecret = config.cloudinaryApiSecret.trim()
        val uploadPreset = config.cloudinaryUploadPreset.trim()

        if (cloudName.isBlank()) {
            throw Exception("Cloudinary configuration is incomplete: cloud name is missing")
        }
        val useUnsigned = uploadPreset.isNotBlank()
        val useSigned = apiKey.isNotBlank() && apiSecret.isNotBlank()
        if (!useUnsigned && !useSigned) {
            throw Exception("Cloudinary configuration is incomplete: provide either an upload preset (unsigned) or API key + secret (signed)")
        }

        val resourceType = when {
            fileName.endsWith(".mp4", ignoreCase = true) ||
            fileName.endsWith(".mov", ignoreCase = true) ||
            fileName.endsWith(".avi", ignoreCase = true) ||
            fileName.endsWith(".mkv", ignoreCase = true) -> "video"
            else -> "auto"
        }

        val fileChannel = fileProvider(0)
        val bytes = fileChannel.toByteArray()

        return try {
            val response: JsonObject = client.submitFormWithBinaryData(
                url = "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload",
                formData = formData {
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                    if (useUnsigned) {
                        append("upload_preset", uploadPreset)
                    } else {
                        val timestamp = (TimeProvider.nowMillis() / 1000).toString()
                        val signature = PlatformUtils.sha1("timestamp=$timestamp$apiSecret")
                        append("api_key", apiKey)
                        append("timestamp", timestamp)
                        append("signature", signature)
                    }
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
