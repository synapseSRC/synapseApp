package com.synapse.social.studioasinc.shared.data.source.remote

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.UploadError
import com.synapse.social.studioasinc.shared.util.DateFormatterUtil
import com.synapse.social.studioasinc.shared.util.TimeProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel

@OptIn(kotlin.time.ExperimentalTime::class)
class R2UploadService(private val client: HttpClient) : UploadService {
    override suspend fun upload(
        fileProvider: suspend (Long) -> ByteReadChannel,
        fileSize: Long,
        fileName: String,
        config: StorageConfig,
        bucketName: String?,
        onProgress: (Float) -> Unit
    ): String {
        val accountId = config.r2AccountId
        val accessKeyId = config.r2AccessKeyId
        val secretAccessKey = config.r2SecretAccessKey
        val targetBucket = bucketName ?: config.r2BucketName

        if (accountId.isBlank() || accessKeyId.isBlank() || secretAccessKey.isBlank() || targetBucket.isBlank()) {
            throw UploadError.R2Error("R2 configuration is incomplete")
        }

        val encodedFileName = fileName.encodeURLPathPart()

        val (host, url) = if (accountId.startsWith("http://") || accountId.startsWith("https://")) {
            val endpoint = accountId.removePrefix("https://").removePrefix("http://")
            val path = "/$targetBucket/$encodedFileName"
            endpoint to "https://$endpoint$path"
        } else {
            val host = "$accountId.r2.cloudflarestorage.com"
            val path = "/$targetBucket/$encodedFileName"
            host to "https://$host$path"
        }
        
        val path = "/$targetBucket/$encodedFileName"

        val amzDate = getAmzDate()

        val signedHeaders = S3Signer.signS3(
            method = "PUT",
            canonicalPath = path,
            region = "auto",
            host = host,
            accessKeyId = accessKeyId,
            secretAccessKey = secretAccessKey,
            amzDate = amzDate,
            contentType = "application/octet-stream"
        )

        try {
            val response = client.put(url) {
                signedHeaders.forEach { (k, v) ->
                    headers.append(k, v)
                }
                headers.append("Content-Length", fileSize.toString())
                setBody(fileProvider(0))
                onUpload { bytesSentTotal, totalBytes ->
                     val total = totalBytes ?: fileSize
                     if (total > 0) {
                        onProgress(bytesSentTotal.toFloat() / total.toFloat())
                    }
                }
            }

            if (!response.status.isSuccess()) {
                throw UploadError.R2Error("Upload failed with status: ${response.status}")
            }

            return url
        } catch (e: Exception) {
            if (e is UploadError) throw e
            throw UploadError.R2Error("R2 upload failed: ${e.message}")
        }
    }

    private fun getAmzDate(): String {
        return DateFormatterUtil.formatAmzDate(TimeProvider.nowInstant())
    }
}
