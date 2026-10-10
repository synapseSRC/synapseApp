package com.synapse.social.studioasinc.shared.data.source.remote

import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.UploadError
import com.synapse.social.studioasinc.shared.domain.model.UploadResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    ): UploadResult {
        val apiKey = config.imgBBKey

        try {
            val channel = fileProvider(0)
            val bytes = channel.toByteArray()

            val baseEndpoint = SynapseConfig.IMGBB_API_ENDPOINT
            val response = client.submitFormWithBinaryData(
                url = "$baseEndpoint?key=$apiKey",
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
                ?: run {
                    val error = jsonResponse["error"]?.jsonObject
                    val message = error?.get("message")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else "Unknown error" } ?: "ImgBB upload failed"
                    throw UploadError.ImgBBError(message)
                }

            val imageUrl = data["url"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                ?: throw UploadError.ImgBBError("ImgBB URL missing in upload response")

            val deleteUrl = data["delete_url"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                ?: throw UploadError.ImgBBError("ImgBB delete URL missing in upload response")

            return UploadResult(url = imageUrl, deleteUrl = deleteUrl)
        } catch (e: Exception) {
            if (e is UploadError) throw e
            val errorMsg = e.message ?: e.toString()
            println("ImgBBUploadService error: $errorMsg")
            throw UploadError.ImgBBError("ImgBB upload failed: $errorMsg")
        }
    }

    suspend fun deleteImgBbImage(
        client: HttpClient,
        deleteUrl: String
    ): Result<Unit> {
        return try {
            val response = client.delete(deleteUrl)

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(
                    IllegalStateException(
                        "ImgBB deletion failed: HTTP ${response.status.value}"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteImgBbImages(
        client: HttpClient,
        deleteUrls: List<String>
    ) {
        deleteUrls.forEach { deleteUrl ->
            try {
                val result = deleteImgBbImage(client, deleteUrl)
                if (result.isFailure) {
                    // Record failure without logging the delete URL/token.
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort cleanup; continue with remaining images.
            }
        }
    }

    suspend fun deleteImage(deleteUrl: String): Result<Unit> {
        return deleteImgBbImage(client, deleteUrl)
    }

    suspend fun deleteImages(deleteUrls: List<String>) {
        deleteImgBbImages(client, deleteUrls)
    }
}
