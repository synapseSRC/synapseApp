package com.synapse.social.studioasinc.core.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicInteger

@Singleton
class UploadProgressManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationIdGenerator = AtomicInteger(1000)
    private val uploadIds = mutableMapOf<String, Int>()

    fun updateProgress(chatId: String, fileName: String, progress: Int) {
        val key = "${chatId}_$fileName"
        val notificationId = uploadIds.getOrPut(key) { notificationIdGenerator.incrementAndGet() }

        val title = "Uploading $fileName"
        NotificationHelper.showProgressNotification(context, notificationId, progress, title)

        if (progress >= 100) {
            uploadIds.remove(key)
        }
    }
}
