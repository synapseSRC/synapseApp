package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong



class MediaCache(private val context: Context) {

    companion object {
        private const val TAG = "MediaCache"
        private const val CACHE_DIR_NAME = "media"
        private const val MAX_CACHE_SIZE_BYTES = 500L * 1024 * 1024
        private const val CACHE_EXPIRATION_DAYS = 7L
        private const val CACHE_EXPIRATION_MS = CACHE_EXPIRATION_DAYS * 24 * 60 * 60 * 1000
        private const val METADATA_FILE_SUFFIX = ".meta"
    }

    private val cacheDir: File = context.cacheDir.resolve(CACHE_DIR_NAME)
    private val currentCacheSize = AtomicLong(0)

    // Using LinkedHashMap with accessOrder = true for LRU behavior.
    // accessOrder = true means iteration order is least-recently-accessed to most-recently-accessed.
    private val accessTimes: MutableMap<String, Long> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(16, 0.75f, true) { }
    )
    private val fileSizes = ConcurrentHashMap<String, Long>()

    init {

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }


        initializeCacheMetadata()
    }



    suspend fun put(key: String, file: File) = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.isFile) {
            Log.w(TAG, "Cannot cache non-existent or invalid file: ${file.path}")
            return@withContext
        }

        try {
            val cacheKey = generateCacheKey(key)
            val cachedFile = getCacheFile(cacheKey)


            file.copyTo(cachedFile, overwrite = true)

            val fileSize = cachedFile.length()
            val currentTime = System.currentTimeMillis()


            updateFileMetadata(cacheKey, fileSize, currentTime)


            if (currentCacheSize.get() > MAX_CACHE_SIZE_BYTES) {
                evictLRU()
            }

            Log.d(TAG, "Cached file with key: $key, size: ${fileSize}B")

        } catch (e: IOException) {
            Log.e(TAG, "Failed to cache file with key: $key", e)
        }
    }



    suspend fun get(key: String): File? = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(key)
        val cachedFile = getCacheFile(cacheKey)

        if (!cachedFile.exists()) {

            removeFileMetadata(cacheKey)
            return@withContext null
        }


        val fileAge = System.currentTimeMillis() - cachedFile.lastModified()
        if (fileAge > CACHE_EXPIRATION_MS) {
            Log.d(TAG, "Cache file expired: $key")
            removeByCacheKey(cacheKey)
            return@withContext null
        }


        accessTimes[cacheKey] = System.currentTimeMillis()

        Log.d(TAG, "Cache hit for key: $key")
        return@withContext cachedFile
    }



    suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(key)
        removeByCacheKey(cacheKey)
    }

    private fun removeByCacheKey(cacheKey: String) {
        val cachedFile = getCacheFile(cacheKey)

        if (cachedFile.exists()) {
            val fileSize = cachedFile.length()

            if (cachedFile.delete()) {
                removeFileMetadata(cacheKey)
                currentCacheSize.addAndGet(-fileSize)
                Log.d(TAG, "Removed cached file: $cacheKey")
            } else {
                Log.w(TAG, "Failed to delete cached file: $cacheKey")
            }
        }
    }



    suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }

            accessTimes.clear()
            fileSizes.clear()
            currentCacheSize.set(0)

            Log.d(TAG, "Cache cleared")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }



    suspend fun evictExpired() = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val expiredFiles = mutableListOf<String>()

        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.endsWith(METADATA_FILE_SUFFIX)) {
                val fileAge = currentTime - file.lastModified()
                if (fileAge > CACHE_EXPIRATION_MS) {
                    val cacheKey = file.nameWithoutExtension
                    expiredFiles.add(cacheKey)
                }
            }
        }

        expiredFiles.forEach { cacheKey ->
            removeByCacheKey(cacheKey)
        }

        if (expiredFiles.isNotEmpty()) {
            Log.d(TAG, "Evicted ${expiredFiles.size} expired files")
        }
    }



    suspend fun evictLRU() = withContext(Dispatchers.IO) {
        if (currentCacheSize.get() <= MAX_CACHE_SIZE_BYTES) {
            return@withContext
        }

        var bytesEvicted = 0L
        var filesEvicted = 0
        val batchSize = 50

        while (currentCacheSize.get() > MAX_CACHE_SIZE_BYTES * 0.8) {

            // OPTIMIZATION: Process in batches to reduce lock contention
            val keysToEvict = synchronized(accessTimes) {
                if (accessTimes.isEmpty()) emptyList()
                else {
                    val iterator = accessTimes.keys.iterator()
                    val batch = ArrayList<String>(batchSize)
                    var count = 0

                    // Since accessTimes is a LinkedHashMap with accessOrder=true,
                    // the iterator yields the least recently used entries first.
                    while (iterator.hasNext() && count < batchSize) {
                        val k = iterator.next()
                        batch.add(k)
                        iterator.remove() // Safely remove via iterator
                        count++
                    }

                    // Clean up metadata
                    batch.forEach { fileSizes.remove(it) }

                    batch
                }
            }

            if (keysToEvict.isEmpty()) break

            // Delete files without holding the map lock
            for (cacheKey in keysToEvict) {
                val cachedFile = getCacheFile(cacheKey)
                if (cachedFile.exists()) {
                    val fileSize = cachedFile.length()

                    if (cachedFile.delete()) {
                        currentCacheSize.addAndGet(-fileSize)
                        bytesEvicted += fileSize
                        filesEvicted++
                        Log.d(TAG, "Removed cached file: $cacheKey")
                    } else {
                        Log.w(TAG, "Failed to delete cached file: $cacheKey")
                    }
                }
            }
        }

        if (filesEvicted > 0) {
            Log.d(TAG, "LRU evicted $filesEvicted files, freed ${bytesEvicted}B")
        }
    }



    fun getCacheSize(): Long = currentCacheSize.get()



    fun getCacheFileCount(): Int = accessTimes.size



    fun getCacheStats(): CacheStats {
        return CacheStats(
            totalSize = currentCacheSize.get(),
            fileCount = accessTimes.size,
            maxSize = MAX_CACHE_SIZE_BYTES,
            expirationDays = CACHE_EXPIRATION_DAYS
        )
    }



    private fun initializeCacheMetadata() {
        var totalSize = 0L

        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.endsWith(METADATA_FILE_SUFFIX)) {
                val cacheKey = file.nameWithoutExtension
                val fileSize = file.length()
                val accessTime = file.lastModified()

                accessTimes[cacheKey] = accessTime
                fileSizes[cacheKey] = fileSize
                totalSize += fileSize
            }
        }

        currentCacheSize.set(totalSize)
        Log.d(TAG, "Initialized cache: ${accessTimes.size} files, ${totalSize}B")
    }

    private fun updateFileMetadata(cacheKey: String, fileSize: Long, accessTime: Long) {

        val oldSize = fileSizes[cacheKey] ?: 0L
        currentCacheSize.addAndGet(fileSize - oldSize)


        accessTimes[cacheKey] = accessTime
        fileSizes[cacheKey] = fileSize
    }

    private fun removeFileMetadata(cacheKey: String) {
        accessTimes.remove(cacheKey)
        fileSizes.remove(cacheKey)
    }

    private fun generateCacheKey(key: String): String {

        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(key.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun reverseGenerateCacheKey(cacheKey: String): String {


        return cacheKey
    }

    private fun getCacheFile(cacheKey: String): File {
        return cacheDir.resolve(cacheKey)
    }



    fun schedulePeriodicCleanup() {
        MediaCacheCleanupWorker.schedulePeriodicCleanup(context)
    }



    fun cancelPeriodicCleanup() {
        MediaCacheCleanupWorker.cancelPeriodicCleanup(context)
    }



    fun runCleanupNow() {
        MediaCacheCleanupWorker.runCleanupNow(context)
    }



    fun contains(key: String): Boolean {
        val cacheKey = generateCacheKey(key)
        val cachedFile = getCacheFile(cacheKey)
        return cachedFile.exists()
    }



    fun getFileSize(key: String): Long? {
        val cacheKey = generateCacheKey(key)
        return fileSizes[cacheKey]
    }



    fun getLastAccessTime(key: String): Long? {
        val cacheKey = generateCacheKey(key)
        return accessTimes[cacheKey]
    }



    fun getAllKeys(): Set<String> {
        return accessTimes.keys.toSet()
    }



    suspend fun performMaintenance(): MaintenanceResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val initialFileCount = accessTimes.size
        val initialSize = currentCacheSize.get()


        evictExpired()
        val afterExpiredFileCount = accessTimes.size
        val afterExpiredSize = currentCacheSize.get()


        evictLRU()
        val finalFileCount = accessTimes.size
        val finalSize = currentCacheSize.get()

        val duration = System.currentTimeMillis() - startTime

        return@withContext MaintenanceResult(
            expiredFilesRemoved = initialFileCount - afterExpiredFileCount,
            lruFilesRemoved = afterExpiredFileCount - finalFileCount,
            totalFilesRemoved = initialFileCount - finalFileCount,
            bytesFreed = initialSize - finalSize,
            durationMs = duration,
            finalCacheSize = finalSize,
            finalFileCount = finalFileCount
        )
    }



    data class CacheStats(
        val totalSize: Long,
        val fileCount: Int,
        val maxSize: Long,
        val expirationDays: Long
    )



    data class MaintenanceResult(
        val expiredFilesRemoved: Int,
        val lruFilesRemoved: Int,
        val totalFilesRemoved: Int,
        val bytesFreed: Long,
        val durationMs: Long,
        val finalCacheSize: Long,
        val finalFileCount: Int
    )
}
