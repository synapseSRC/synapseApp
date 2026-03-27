package com.synapse.social.studioasinc.shared.core.media

import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.writeToURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class IosMediaCompressor : MediaCompressor {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun compress(filePath: String): Result<String> = withContext(Dispatchers.Default) {
        try {
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(filePath)) {
                return@withContext Result.failure(Exception("File not found at $filePath"))
            }

            val image = UIImage.imageWithContentsOfFile(filePath)
                ?: return@withContext Result.failure(Exception("Could not load image from $filePath"))

            // Compress to JPEG with 0.7 quality
            val compressedData = UIImageJPEGRepresentation(image, 0.7)
                ?: return@withContext Result.failure(Exception("Could not compress image"))

            val fileName = NSUUID.UUID().UUIDString + ".jpg"
            val tempDir = NSTemporaryDirectory()
            val tempUrl = NSURL.fileURLWithPath(tempDir).URLByAppendingPathComponent(fileName)
            val outputInfo = tempUrl?.path ?: return@withContext Result.failure(Exception("Could not create temp file path"))
            val destinationUrl = tempUrl ?: return@withContext Result.failure(Exception("Invalid temp URL"))

            // Write data
            val success = compressedData.writeToURL(destinationUrl, true)
            if (!success) {
                return@withContext Result.failure(Exception("Failed to write compressed image to $outputInfo"))
            }

            Result.success(outputInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
