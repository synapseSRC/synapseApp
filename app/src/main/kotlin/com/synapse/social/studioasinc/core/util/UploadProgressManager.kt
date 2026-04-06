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

    fun showProgress(id: Int, percentage: Int, title: String) {
        NotificationHelper.showProgressNotification(context, id, percentage, title)
    }

    fun finishProgress(id: Int, success: Boolean, title: String) {
        val progress = if (success) 100 else 0
        NotificationHelper.showProgressNotification(context, id, progress, title)
    }

    fun dismissProgress(chatId: String, fileName: String) {
        val key = "${chatId}_$fileName"
        val id = uploadIds.remove(key) ?: return
        NotificationHelper.dismissNotification(context, id)
    }

    fun finishProgress(chatId: String, fileName: String, success: Boolean, title: String) {
        val key = "${chatId}_$fileName"
        val id = uploadIds.getOrPut(key) { notificationIdGenerator.incrementAndGet() }
        val progress = if (success) 100 else 0
        NotificationHelper.showProgressNotification(context, id, progress, title)
        if (!success) uploadIds.remove(key)
    }

    fun dismissProgress(id: Int) {
        NotificationHelper.dismissNotification(context, id)
    }
}
