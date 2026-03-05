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
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.core.readBytes
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(InternalAPI::class)
class ImgBBUploadService(private val client: HttpClient) : UploadService {
    override suspend fun upload(
        fileProvider: suspend (Long) -> ByteReadChannel,
        fileSize: Long,
        fileName: String,
        config: StorageConfig,
        bucketName: String?,
        onProgress: (Float) -> Unit
    ): String {
        val apiKey = config.imgBBKey

        val fileChannel = fileProvider(0)
        val fileBytes = fileChannel.readRemaining().readBytes()

        val response: JsonObject = client.submitFormWithBinaryData(
            url = "https://api.imgbb.com/1/upload?key=$apiKey",
            formData = formData {
                append("image", fileBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/*")
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }
        ).body()

        onProgress(1.0f)

        val data = response["data"]?.jsonObject ?: throw UploadError.ImgBBError("ImgBB upload failed")
        return data["url"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content ?: throw UploadError.ImgBBError("ImgBB URL missing")
    }
}
