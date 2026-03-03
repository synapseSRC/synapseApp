package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class FileInfo(val fileName: String, val mimeType: String, val extension: String)

object FileUtils {

    private const val TAG = "FileUtils"

    fun detectFileInfoFromUrl(
        url: String,
        baseFileName: String,
        defaultExtension: String,
        defaultMimeType: String,
        mimePrefix: String,
        extensionMap: Map<String, String>
    ): FileInfo {
        var extension = defaultExtension
        var mimeType = defaultMimeType
        try {
            val urlLower = url.lowercase()
            for ((ext, mime) in extensionMap) {
                if (urlLower.contains(ext)) {
                    extension = ext
                    mimeType = mime
                    break
                }
            }

            val uri = Uri.parse(url)
            val path = uri.path
            if (path != null) {
                val lastDot = path.lastIndexOf('.')
                if (lastDot > 0 && lastDot < path.length - 1) {
                    val urlExtension = path.substring(lastDot).lowercase()
                    val detectedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(urlExtension.substring(1))
                    if (detectedMimeType != null && detectedMimeType.startsWith(mimePrefix)) {
                        extension = urlExtension
                        mimeType = detectedMimeType
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error detecting file type from URL", e)
        }

        var finalFileName = baseFileName
        if (!finalFileName.lowercase().endsWith(extension.lowercase())) {
            finalFileName += extension
        }
        return FileInfo(finalFileName, mimeType, extension)
    }

    fun getTmpFileUri(context: Context, extension: String = ".png", prefix: String? = null): Uri {
        val filePrefix = prefix ?: if (extension == ".mp4") "tmp_video_file" else "tmp_image_file"
        val tempDir = File(context.cacheDir, "temp")
        if (!tempDir.exists()) tempDir.mkdirs()
        val tmpFile = File.createTempFile(filePrefix, extension, tempDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmpFile)
    }

    fun createNewFile(path: String) {
        val lastSep = path.lastIndexOf(File.separator)
        if (lastSep > 0) {
            val dirPath = path.substring(0, lastSep)
            makeDir(dirPath)
        }

        val file = File(path)
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (e: Exception) {
            Log.e(TAG, "createNewFile failed: $path", e)
        }
    }

    fun readFile(path: String): String {
        createNewFile(path)
        return try {
            File(path).readText()
        } catch (e: Exception) {
            Log.e(TAG, "readFile failed: $path", e)
            ""
        }
    }

    fun writeFile(path: String, str: String) {
        createNewFile(path)
        try {
            File(path).writeText(str)
        } catch (e: Exception) {
            Log.e(TAG, "writeFile failed: $path", e)
        }
    }

    fun copyFile(sourcePath: String, destPath: String) {
        if (!isExistFile(sourcePath)) return
        createNewFile(destPath)

        try {
            FileInputStream(sourcePath).use { fis ->
                FileOutputStream(destPath).use { fos ->
                    fis.copyTo(fos)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "copyFile failed", e)
        }
    }

    fun copyDir(oldPath: String, newPath: String) {
        val oldFile = File(oldPath)
        val files = oldFile.listFiles() ?: return
        val newFile = File(newPath)

        if (!newFile.exists()) {
            newFile.mkdirs()
        }

        for (file in files) {
            when {
                file.isFile -> copyFile(file.path, "$newPath/${file.name}")
                file.isDirectory -> copyDir(file.path, "$newPath/${file.name}")
            }
        }
    }

    fun moveFile(sourcePath: String, destPath: String) {
        copyFile(sourcePath, destPath)
        deleteFile(sourcePath)
    }

    fun deleteFile(path: String) {
        val file = File(path)
        if (!file.exists()) return

        if (file.isFile) {
            file.delete()
            return
        }

        file.listFiles()?.forEach { subFile ->
            when {
                subFile.isDirectory -> deleteFile(subFile.absolutePath)
                subFile.isFile -> subFile.delete()
            }
        }
        file.delete()
    }

    fun isExistFile(path: String): Boolean {
        return File(path).exists()
    }

    fun makeDir(path: String) {
        if (!isExistFile(path)) {
            File(path).mkdirs()
        }
    }

    fun listDir(path: String, list: ArrayList<String>?) {
        val dir = File(path)
        if (!dir.exists() || dir.isFile) return

        val listFiles = dir.listFiles()
        if (listFiles.isNullOrEmpty()) return

        list?.apply {
            clear()
            addAll(listFiles.map { it.absolutePath })
        }
    }

    fun isDirectory(path: String): Boolean {
        if (!isExistFile(path)) return false
        return File(path).isDirectory
    }

    fun isFile(path: String): Boolean {
        if (!isExistFile(path)) return false
        return File(path).isFile
    }

    fun getFileLength(path: String): Long {
        if (!isExistFile(path)) return 0
        return File(path).length()
    }

    fun getPackageDataDir(context: Context): String {
        return context.getExternalFilesDir(null)?.absolutePath ?: ""
    }

    fun validateAndCleanPath(context: Context, path: String): String? {
        if (path.startsWith("content://", ignoreCase = true)) return path

        try {
            // Strip file:// prefix if present and handle URL decoding
            val cleanPath = if (path.startsWith("file:", ignoreCase = true)) {
                Uri.parse(path).path ?: path
            } else {
                path
            }
            val file = java.io.File(cleanPath)

            if (!file.exists()) {
                Log.e(TAG, "File not found: $cleanPath")
                return null
            }

            val canonicalPath = file.canonicalPath
            val dataDir = java.io.File(context.applicationInfo.dataDir).canonicalPath
            val cacheDir = context.cacheDir.canonicalPath

            // Robust check for directory containment
            val isInDataDir = canonicalPath == dataDir || canonicalPath.startsWith(dataDir + java.io.File.separator)
            val isInCacheDir = canonicalPath == cacheDir || canonicalPath.startsWith(cacheDir + java.io.File.separator)

            // Block access to private data directory unless it's in the cache directory
            if (isInDataDir && !isInCacheDir) {
                Log.e(TAG, "Invalid file source (private data dir): $canonicalPath")
                return null
            }
            return cleanPath
        } catch (e: Exception) {
            Log.e(TAG, "Path validation failed", e)
            return null
        }
    }
}
