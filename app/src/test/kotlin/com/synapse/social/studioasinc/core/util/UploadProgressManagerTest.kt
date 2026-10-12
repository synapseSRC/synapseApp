package com.synapse.social.studioasinc.core.util

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UploadProgressManagerTest {

    private lateinit var uploadProgressManager: UploadProgressManager
    private lateinit var systemNotificationManager: NotificationManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        uploadProgressManager = UploadProgressManager(context)
        systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun failedUploadDismissesItsProgressNotification() {
        uploadProgressManager.updateProgress("chat-1", "photo.jpg", 40)
        assertEquals(1, systemNotificationManager.activeNotifications.size)

        uploadProgressManager.finishProgress("chat-1", "photo.jpg", success = false, title = "Upload Failed")

        assertEquals(0, systemNotificationManager.activeNotifications.size)
    }

    @Test
    fun failedUploadWithoutProgressDoesNotCreateAStuckNotification() {
        uploadProgressManager.finishProgress("chat-1", "photo.jpg", success = false, title = "Upload Failed")

        assertEquals(0, systemNotificationManager.activeNotifications.size)
    }

    @Test
    fun failedLegacyProgressNotificationIsDismissed() {
        uploadProgressManager.showProgress(42, 40, "Uploading photo.jpg")
        assertEquals(1, systemNotificationManager.activeNotifications.size)

        uploadProgressManager.finishProgress(42, success = false, title = "Upload Failed")

        assertEquals(0, systemNotificationManager.activeNotifications.size)
    }
}
