package com.synapse.social.studioasinc.shared.data.source.remote

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.UploadError
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
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(InternalAPI::class)
class ImgBBUploadService(private val client: HttpClient) : UploadService {

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
        val apiKey = config.imgBBKey

        try {
            val channel = fileProvider(0)
            val bytes = channel.toByteArray()
            
            val response = client.submitFormWithBinaryData(
                url = "https://api.imgbb.com/1/upload?key=$apiKey",
                formData = formData {
                    append("image", bytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        append(HttpHeaders.ContentType, "application/octet-stream")
                    })
                }
            )

            onProgress(1.0f)

            val jsonResponse: JsonObject = response.body()
            val data = jsonResponse["data"]?.jsonObject
            
            if (data == null) {
                val error = jsonResponse["error"]?.jsonObject
                val message = error?.get("message")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else "Unknown error" } ?: "ImgBB upload failed"
                throw UploadError.ImgBBError(message)
            }

            val url = data["url"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                ?: throw UploadError.ImgBBError("ImgBB URL missing in upload response")

            return url
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.toString()
            println("ImgBBUploadService error: $errorMsg")
            throw UploadError.ImgBBError("ImgBB upload failed: $errorMsg")
        }
    }
}
