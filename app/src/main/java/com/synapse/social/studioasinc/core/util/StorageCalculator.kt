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
        val synapseSize = calculateAppSize()
        val usedSize = totalSize - freeSize
        val appsAndOtherSize = usedSize - synapseSize
        
        StorageUsageBreakdown(
            totalSize = totalSize,
            usedSize = usedSize,
            freeSize = freeSize,
            appsAndOtherSize = appsAndOtherSize.coerceAtLeast(0),
            synapseSize = synapseSize
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

    private suspend fun calculateAppSize(): Long = withContext(Dispatchers.IO) {
        var totalSize = 0L
        
        listOf(
            context.cacheDir,
            context.externalCacheDir,
            context.codeCacheDir,
            context.filesDir,
            context.getExternalFilesDir(null),
            context.getDatabasePath("storage.db").parentFile
        ).forEach { dir ->
            dir?.let { totalSize += getDirectorySize(it) }
        }
        
        totalSize
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
        return when (file.extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp" -> MediaType.PHOTO
            "mp4", "mkv", "avi", "mov", "webm" -> MediaType.VIDEO
            "mp3", "wav", "flac", "aac", "ogg" -> MediaType.AUDIO
            "pdf", "doc", "docx", "txt", "xls", "xlsx" -> MediaType.DOCUMENT
            else -> MediaType.DOCUMENT
        }
    }
}
