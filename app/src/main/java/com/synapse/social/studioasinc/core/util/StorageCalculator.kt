package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.os.StatFs
import com.synapse.social.studioasinc.ui.settings.LargeFileInfo
import com.synapse.social.studioasinc.ui.settings.MediaType
import com.synapse.social.studioasinc.ui.settings.StorageUsageBreakdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StorageCalculator(private val context: Context) {

    suspend fun getStorageBreakdown(): StorageUsageBreakdown = withContext(Dispatchers.IO) {
        val dataDir = context.dataDir
        val stat = StatFs(dataDir.path)
        
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val totalSize = totalBlocks * blockSize
        val freeSize = availableBlocks * blockSize
        val appSizeMap = calculateAppSize()
        val synapseSize = appSizeMap["total"] ?: 0L
        val usedSize = totalSize - freeSize
        val appsAndOtherSize = usedSize - synapseSize
        
        StorageUsageBreakdown(
            totalSize = totalSize,
            usedSize = usedSize,
            freeSize = freeSize,
            appsAndOtherSize = appsAndOtherSize.coerceAtLeast(0),
            synapseSize = synapseSize,
            photoSize = appSizeMap["photo"] ?: 0L,
            videoSize = appSizeMap["video"] ?: 0L,
            documentSize = appSizeMap["document"] ?: 0L,
            otherSize = appSizeMap["other"] ?: 0L,
            chatSize = appSizeMap["chat"] ?: 0L
        )
    }

    suspend fun getLargeFiles(minSizeBytes: Long): List<LargeFileInfo> = withContext(Dispatchers.IO) {
        val largeFiles = mutableListOf<LargeFileInfo>()
        
        listOf(
            context.cacheDir,
            context.externalCacheDir,
            context.filesDir,
            context.getExternalFilesDir(null)
        ).forEach { dir ->
            dir?.let { scanDirectory(it, minSizeBytes, largeFiles) }
        }
        
        largeFiles.sortedByDescending { it.size }
    }

    private suspend fun calculateAppSize(): Map<String, Long> = withContext(Dispatchers.IO) {
        var photoSize = 0L
        var videoSize = 0L
        var documentSize = 0L
        var otherSize = 0L
        var chatSize = 0L
        
        val dbDir = context.getDatabasePath("storage.db").parentFile

        listOf(
            context.cacheDir,
            context.externalCacheDir,
            context.codeCacheDir,
            context.filesDir,
            context.getExternalFilesDir(null),
            dbDir
        ).forEach { dir ->
            if (dir != null && dir.exists()) {
                dir.walkBottomUp().filter { it.isFile }.forEach { file ->
                    val size = file.length()
                    if (dir == dbDir) {
                        chatSize += size
                    } else {
                        when (determineMediaType(file)) {
                            MediaType.PHOTO -> photoSize += size
                            MediaType.VIDEO -> videoSize += size
                            MediaType.DOCUMENT -> documentSize += size
                            else -> otherSize += size
                        }
                    }
                }
            }
        }
        
        mapOf(
            "photo" to photoSize,
            "video" to videoSize,
            "document" to documentSize,
            "other" to otherSize,
            "chat" to chatSize,
            "total" to (photoSize + videoSize + documentSize + otherSize + chatSize)
        )
    }

    private fun scanDirectory(dir: File, minSize: Long, result: MutableList<LargeFileInfo>) {
        if (!dir.exists()) return
        
        dir.walkTopDown()
            .filter { it.isFile && it.length() >= minSize }
            .take(50)
            .forEach { file ->
                result.add(
                    LargeFileInfo(
                        fileId = file.absolutePath,
                        fileName = file.name,
                        size = file.length(),
                        thumbnailUri = null,
                        type = determineMediaType(file)
                    )
                )
            }
    }

    private fun getDirectorySize(dir: File): Long {
        return if (dir.exists()) {
            dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
        } else {
            0L
        }
    }

    private fun determineMediaType(file: File): MediaType {
        val ext = file.extension.lowercase()
        if (ext.isEmpty()) {
            val parentName = file.parentFile?.name?.lowercase() ?: ""
            if (parentName.contains("image") ||
                parentName.contains("photo") ||
                parentName.contains("coil") ||
                parentName.contains("glide")) {
                return MediaType.PHOTO
            }
        }

        return when (ext) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp" -> MediaType.PHOTO
            "mp4", "mkv", "avi", "mov", "webm" -> MediaType.VIDEO
            "mp3", "wav", "flac", "aac", "ogg" -> MediaType.AUDIO
            "pdf", "doc", "docx", "txt", "xls", "xlsx" -> MediaType.DOCUMENT
            else -> MediaType.OTHER
        }
    }
}
