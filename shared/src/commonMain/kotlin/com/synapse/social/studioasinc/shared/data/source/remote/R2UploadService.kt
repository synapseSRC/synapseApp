package com.synapse.social.studioasinc.shared.data.source.remote

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.UploadError
import com.synapse.social.studioasinc.shared.util.DateFormatterUtil
import com.synapse.social.studioasinc.shared.util.TimeProvider
import com.synapse.social.studioasinc.shared.data.PlatformUtils
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable

import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.core.readBytes

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
            val hostPart = endpoint.split('/')[0] // Ensure we only get the host
            val cleanEndpoint = endpoint.removeSuffix("/")
            hostPart to "https://$cleanEndpoint/$targetBucket/$encodedFileName"
        } else {
            val host = "$accountId.r2.cloudflarestorage.com"
            host to "https://$host/$targetBucket/$encodedFileName"
        }
        
        // For S3 signature, the canonical path needs to be URL-encoded once
        val canonicalPath = "/$targetBucket/$encodedFileName"

        val amzDate = getAmzDate()

        try {
            val channel = fileProvider(0)
            val bytes = channel.readRemaining().readBytes()
            
            // Calculate SHA256 hash of the payload for signature
            val payloadHash = PlatformUtils.sha256(bytes)
            
            val signedHeaders = S3Signer.signS3(
                method = "PUT",
                canonicalPath = canonicalPath,
                region = "auto",
                host = host,
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                amzDate = amzDate,
                contentType = "application/octet-stream",
                payloadHash = payloadHash
            )
            
            val response = client.put(url) {
                signedHeaders.forEach { (k, v) ->
                    headers[k] = v
                }
                
                headers["Content-Length"] = bytes.size.toString()
                setBody(bytes)
                
                onUpload { bytesSentTotal, totalBytes ->
                     val total = totalBytes ?: bytes.size.toLong()
                     if (total > 0) {
                        onProgress(bytesSentTotal.toFloat() / total.toFloat())
                    }
                }
            }

            if (!response.status.isSuccess()) {
                val errorBody = try { response.bodyAsText() } catch (e: Exception) { response.status.description }
                throw UploadError.R2Error("R2 upload failed with status ${response.status}: $errorBody")
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
