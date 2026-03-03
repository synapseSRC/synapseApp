package com.synapse.social.studioasinc.core.media.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap



class ThumbnailGenerator(private val context: Context) {

    companion object {
        private const val THUMBNAIL_SIZE = 200
        private const val THUMBNAIL_QUALITY = 85
        private const val VIDEO_FRAME_TIME_US = 1000000L
        private const val CACHE_DIR_NAME = "thumbnails"
        private const val MAX_CACHE_SIZE = 100 * 1024 * 1024L
    }


    private val thumbnailCache = ConcurrentHashMap<String, File>()


    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }



    suspend fun generateImageThumbnail(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateCacheKey(uri, "image")


            getCachedThumbnail(cacheKey)?.let { cachedFile ->
                return@withContext Result.success(cachedFile)
            }


            val inputStream = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                null
            } ?: return@withContext Result.failure(IOException("Invalid or inaccessible media URI: $uri"))


            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            val dimensionsResult = try {
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                val originalWidth = options.outWidth
                val originalHeight = options.outHeight


                if (originalWidth <= 0 || originalHeight <= 0) {
                    return@withContext Result.failure(IOException("Corrupted image: Invalid dimensions ($originalWidth x $originalHeight)"))
                }


                if (originalWidth > 10000 || originalHeight > 10000) {
                    return@withContext Result.failure(IOException("Image too large: ${originalWidth}x${originalHeight}"))
                }


                val mimeType = options.outMimeType
                if (mimeType == null || !isValidImageMimeType(mimeType)) {
                    return@withContext Result.failure(IOException("Unsupported or corrupted image format: $mimeType"))
                }

                Pair(originalWidth, originalHeight)
            } catch (e: Exception) {
                inputStream.close()
                return@withContext Result.failure(IOException("Corrupted image file: ${e.message}", e))
            }

            val (originalWidth, originalHeight) = dimensionsResult


            val sampleSize = calculateThumbnailSampleSize(originalWidth, originalHeight)


            val newInputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Cannot reopen input stream"))

            val bitmap = try {
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                    inTempStorage = ByteArray(16 * 1024)
                }
                BitmapFactory.decodeStream(newInputStream, null, decodeOptions)
            } catch (e: OutOfMemoryError) {

                val higherSampleSize = sampleSize * 2
                val retryOptions = BitmapFactory.Options().apply {
                    inSampleSize = higherSampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                    inTempStorage = ByteArray(16 * 1024)
                }
                newInputStream.close()
                val retryInputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IOException("Cannot reopen input stream after OOM"))
                BitmapFactory.decodeStream(retryInputStream, null, retryOptions)
            } catch (e: Exception) {
                newInputStream.close()
                return@withContext Result.failure(IOException("Failed to decode corrupted image: ${e.message}", e))
            } finally {
                try {
                    newInputStream.close()
                } catch (e: Exception) {

                }
            }

            if (bitmap == null) {
                return@withContext Result.failure(IOException("Failed to decode bitmap for thumbnail - possibly corrupted"))
            }


            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                bitmap.recycle()
                return@withContext Result.failure(IOException("Decoded bitmap is invalid or corrupted"))
            }


            val thumbnail = createCenterCroppedThumbnail(bitmap, THUMBNAIL_SIZE)
            bitmap.recycle()


            if (thumbnail.isRecycled || thumbnail.width != THUMBNAIL_SIZE || thumbnail.height != THUMBNAIL_SIZE) {
                thumbnail.recycle()
                return@withContext Result.failure(IOException("Failed to create valid thumbnail"))
            }


            val thumbnailFile = saveThumbnailToCache(thumbnail, cacheKey)
            thumbnail.recycle()


            thumbnailCache[cacheKey] = thumbnailFile

            Result.success(thumbnailFile)

        } catch (e: OutOfMemoryError) {
            Result.failure(IOException("Out of memory while generating image thumbnail", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun generateVideoThumbnail(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateCacheKey(uri, "video")


            getCachedThumbnail(cacheKey)?.let { cachedFile ->
                return@withContext Result.success(cachedFile)
            }


            val retriever = MediaMetadataRetriever()

            try {
                try {
                    retriever.setDataSource(context, uri)
                } catch (e: Exception) {
                    return@withContext Result.failure(IOException("Invalid or inaccessible video URI: $uri", e))
                }


                val duration = try {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                } catch (e: Exception) {
                    null
                }

                val width = try {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                } catch (e: Exception) {
                    null
                }

                val height = try {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                } catch (e: Exception) {
                    null
                }


                if (duration == null || duration <= 0) {
                    return@withContext Result.failure(IOException("Corrupted video: Invalid or missing duration"))
                }

                if (width == null || height == null || width <= 0 || height <= 0) {
                    return@withContext Result.failure(IOException("Corrupted video: Invalid dimensions ($width x $height)"))
                }


                if (width > 10000 || height > 10000) {
                    return@withContext Result.failure(IOException("Video resolution too large: ${width}x${height}"))
                }


                val bitmap = try {

                    retriever.getFrameAtTime(
                        VIDEO_FRAME_TIME_US,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                } catch (e: Exception) {
                    try {

                        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } catch (e2: Exception) {
                        try {

                            retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST)
                        } catch (e3: Exception) {
                            null
                        }
                    }
                }

                if (bitmap == null) {
                    return@withContext Result.failure(IOException("Failed to extract any frame from video - possibly corrupted"))
                }


                if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                    bitmap.recycle()
                    return@withContext Result.failure(IOException("Extracted video frame is invalid"))
                }


                val thumbnail = createCenterCroppedThumbnail(bitmap, THUMBNAIL_SIZE)
                bitmap.recycle()


                if (thumbnail.isRecycled || thumbnail.width != THUMBNAIL_SIZE || thumbnail.height != THUMBNAIL_SIZE) {
                    thumbnail.recycle()
                    return@withContext Result.failure(IOException("Failed to create valid video thumbnail"))
                }


                val thumbnailFile = saveThumbnailToCache(thumbnail, cacheKey)
                thumbnail.recycle()


                thumbnailCache[cacheKey] = thumbnailFile

                Result.success(thumbnailFile)

            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {

                }
            }

        } catch (e: OutOfMemoryError) {
            Result.failure(IOException("Out of memory while generating video thumbnail", e))
        } catch (e: Exception) {
            Result.failure(IOException("Failed to generate video thumbnail: ${e.message}", e))
        }
    }



    suspend fun extractVideoFrame(uri: Uri, timeMs: Long): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()

            try {
                retriever.setDataSource(context, uri)

                val timeUs = timeMs * 1000L
                val bitmap = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

                if (bitmap == null) {
                    return@withContext Result.failure(IOException("Failed to extract video frame at ${timeMs}ms"))
                }

                Result.success(bitmap)

            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {

                }
            }

        } catch (e: OutOfMemoryError) {
            Result.failure(IOException("Out of memory while extracting video frame", e))
        } catch (e: Exception) {
            Result.failure(IOException("Failed to extract video frame: ${e.message}", e))
        }
    }



    private fun calculateThumbnailSampleSize(width: Int, height: Int): Int {
        var inSampleSize = 1
        val targetSize = THUMBNAIL_SIZE * 2

        if (width > targetSize || height > targetSize) {
            val halfWidth = width / 2
            val halfHeight = height / 2


            while ((halfWidth / inSampleSize) >= targetSize && (halfHeight / inSampleSize) >= targetSize) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }



    private fun createCenterCroppedThumbnail(source: Bitmap, size: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height


        val cropSize = minOf(sourceWidth, sourceHeight)
        val cropX = (sourceWidth - cropSize) / 2
        val cropY = (sourceHeight - cropSize) / 2


        val croppedBitmap = try {
            Bitmap.createBitmap(source, cropX, cropY, cropSize, cropSize)
        } catch (e: OutOfMemoryError) {

            return Bitmap.createScaledBitmap(source, size, size, true)
        }


        val thumbnail = if (cropSize != size) {
            try {
                val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, size, size, true)
                if (scaledBitmap != croppedBitmap) {
                    croppedBitmap.recycle()
                }
                scaledBitmap
            } catch (e: OutOfMemoryError) {
                croppedBitmap
            }
        } else {
            croppedBitmap
        }

        return thumbnail
    }



    private suspend fun saveThumbnailToCache(thumbnail: Bitmap, cacheKey: String): File = withContext(Dispatchers.IO) {
        val thumbnailFile = File(cacheDir, "$cacheKey.jpg")

        try {
            FileOutputStream(thumbnailFile).use { outputStream ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, outputStream)
            }
        } catch (e: IOException) {

            if (thumbnailFile.exists()) {
                thumbnailFile.delete()
            }
            throw e
        }

        thumbnailFile
    }



    private fun generateCacheKey(uri: Uri, type: String): String {
        return "${type}_${uri.toString().hashCode()}_${THUMBNAIL_SIZE}"
    }



    private fun getCachedThumbnail(cacheKey: String): File? {

        thumbnailCache[cacheKey]?.let { cachedFile ->
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return cachedFile
            } else {

                thumbnailCache.remove(cacheKey)
            }
        }


        val thumbnailFile = File(cacheDir, "$cacheKey.jpg")
        if (thumbnailFile.exists() && thumbnailFile.length() > 0) {

            thumbnailCache[cacheKey] = thumbnailFile
            return thumbnailFile
        }

        return null
    }



    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {

            thumbnailCache.clear()


            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    try {
                        file.delete()
                    } catch (e: Exception) {

                    }
                }
            }
        } catch (e: Exception) {

        }
    }



    suspend fun performCacheMaintenance() = withContext(Dispatchers.IO) {
        try {
            if (!cacheDir.exists()) return@withContext

            val files = cacheDir.listFiles() ?: return@withContext
            val totalSize = files.sumOf { it.length() }


            if (totalSize > MAX_CACHE_SIZE) {
                // Optimization: Map files to store attributes and avoid repeated system calls
                // structure: (File, length, lastModified)
                // We only do this if we actually need to clean up, preserving performance for the happy path.
                val fileInfos = files.map { file ->
                    Triple(file, file.length(), file.lastModified())
                }

                // Sort using pre-fetched lastModified (third component)
                val sortedInfos = fileInfos.sortedBy { it.third }
                var currentSize = totalSize

                for ((file, length, _) in sortedInfos) {
                    if (currentSize <= MAX_CACHE_SIZE * 0.8) break

                    try {
                        currentSize -= length
                        file.delete()


                        val fileName = file.nameWithoutExtension
                        thumbnailCache.remove(fileName)
                    } catch (e: Exception) {

                    }
                }
            }
        } catch (e: Exception) {

        }
    }






    private fun isValidImageMimeType(mimeType: String): Boolean {
        return when (mimeType.lowercase()) {
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp" -> true
            else -> false
        }
    }
}
