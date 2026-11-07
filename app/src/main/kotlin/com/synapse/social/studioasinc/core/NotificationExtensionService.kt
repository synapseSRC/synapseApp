package com.synapse.social.studioasinc.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import coil3.ImageLoader
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.onesignal.notifications.IDisplayableMutableNotification
import com.onesignal.notifications.INotificationReceivedEvent
import com.onesignal.notifications.INotificationServiceExtension
import com.synapse.social.studioasinc.data.local.database.MessageDeliveryWorker
import kotlinx.coroutines.runBlocking
import kotlin.math.min

/**
 * OneSignal v5 notification service extension that intercepts push notifications before display,
 * loads the sender's avatar from [data.avatarUrl], applies a circular crop, and injects the result
 * as the notification's large icon.
 *
 * Works on all Android versions (API 21+) regardless of whether the avatar URL is a raster served
 * by OneSignal's large_icon pipeline or an SVG that BitmapFactory cannot decode.
 *
 * Registered via the [com.onesignal.NotificationServiceExtension] meta-data in AndroidManifest.xml.
 */
@Keep
class NotificationExtensionService : INotificationServiceExtension {

    override fun onNotificationReceived(event: INotificationReceivedEvent) {
        val notification: IDisplayableMutableNotification = event.notification
        val context: Context = event.context
        MessageDeliveryWorker.enqueueIfMessage(context, notification.additionalData)

        val avatarUrl = notification.additionalData?.optString("avatarUrl", null)

        if (avatarUrl.isNullOrBlank()) return

        // Suppress auto-display while we load the image, then show it with the large icon set.
        event.preventDefault()

        Thread {
            try {
                val bitmap = loadCircularAvatar(context, avatarUrl)
                if (bitmap != null) {
                    notification.setExtender(object : NotificationCompat.Extender {
                        override fun extend(builder: NotificationCompat.Builder): NotificationCompat.Builder {
                            builder.setLargeIcon(bitmap)
                            return builder
                        }
                    })
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not render notification avatar", e)
            } finally {
                // Always display — the image is best-effort, suppressing the notification entirely
                // because of a failed avatar fetch would lose message delivery.
                notification.display()
            }
        }.start()
    }

    private fun loadCircularAvatar(context: Context, url: String): Bitmap? {
        // Use the application's configured ImageLoader so SVG/Video decoders are registered.
        val imageLoader: ImageLoader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // need a software bitmap for Canvas drawing
            .build()

        val image = runBlocking { imageLoader.execute(request) }.image ?: return null
        val source = image.toBitmap()
        return cropToCircle(source, NOTIFICATION_ICON_SIZE)
    }

    /**
     * Crops [source] to a square, scales to [size], then applies a circular mask.
     */
    private fun cropToCircle(source: Bitmap, size: Int): Bitmap {
        val side = min(source.width, source.height)
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        val squared = Bitmap.createBitmap(source, x, y, side, side)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw the squared source centered and scaled into the output circle.
        val srcRect = Rect(0, 0, squared.width, squared.height)
        val dstRect = RectF(0f, 0f, size.toFloat(), size.toFloat())

        // Clip to circle path via a filled rounded rect (radius = half the size → circle).
        paint.style = Paint.Style.FILL
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squared, srcRect, dstRect, paint)

        // Bitmap.createBitmap returns the *source object* unchanged when the requested rect covers
        // it entirely, which is every square avatar. Recycling that alias would free a bitmap Coil
        // still holds in its memory cache, blanking the avatar everywhere else in the app.
        if (squared !== source) squared.recycle()
        return output
    }

    companion object {
        private const val TAG = "NotifExtension"
        private const val NOTIFICATION_ICON_SIZE = 128
    }
}
