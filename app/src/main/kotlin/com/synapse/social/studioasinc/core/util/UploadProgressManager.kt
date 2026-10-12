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
    }

    fun showProgress(id: Int, percentage: Int, title: String) {
        NotificationHelper.showProgressNotification(context, id, percentage, title)
    }

    fun finishProgress(id: Int, success: Boolean, title: String) {
        if (success) {
            NotificationHelper.showProgressNotification(context, id, 100, title)
        } else {
            NotificationHelper.dismissNotification(context, id)
        }
    }

    fun dismissProgress(chatId: String, fileName: String) {
        val key = "${chatId}_$fileName"
        val id = uploadIds.remove(key) ?: return
        NotificationHelper.dismissNotification(context, id)
    }

    fun finishProgress(chatId: String, fileName: String, success: Boolean, title: String) {
        val key = "${chatId}_$fileName"
        val id = uploadIds.remove(key) ?: return
        if (success) {
            NotificationHelper.showProgressNotification(context, id, 100, title)
        } else {
            NotificationHelper.dismissNotification(context, id)
        }
    }

    fun dismissProgress(id: Int) {
        NotificationHelper.dismissNotification(context, id)
    }
}
