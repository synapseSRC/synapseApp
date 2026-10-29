package com.synapse.social.studioasinc.data.call.webrtc

import android.content.Context
import io.github.aakira.napier.Napier
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.Size
import org.webrtc.VideoCapturer

/**
 * Resolves a camera device for capture.
 *
 * Camera2 is preferred (better format control and rotation metadata); devices without a working
 * Camera2 stack fall back to Camera1 so that the voice-to-video upgrade still succeeds. Selection
 * is front/rear aware because the in-call UI shows which lens is live, and the capture size is
 * chosen from what the hardware actually reports so Camera2 is never asked for an impossible mode.
 */
class VideoCapturerFactory(private val context: Context) {

    data class Selection(
        val capturer: CameraVideoCapturer,
        val deviceName: String,
        val isFrontFacing: Boolean,
        val width: Int,
        val height: Int,
        val fps: Int
    )

    private val enumerator: CameraEnumerator =
        if (Camera2Enumerator.isSupported(context)) Camera2Enumerator(context) else Camera1Enumerator(true)

    val supportsCamera2: Boolean get() = enumerator is Camera2Enumerator

    fun deviceCount(): Int = enumerator.deviceNames.size

    fun pickCamera(preferFront: Boolean): String? {
        val names = enumerator.deviceNames
        if (names.isEmpty()) return null
        // A device with a single lens, or with no front camera at all, still has to work.
        return names.firstOrNull { name -> facing(name, preferFront) } ?: names.first()
    }

    fun facing(deviceName: String, front: Boolean): Boolean = runCatching {
        if (front) enumerator.isFrontFacing(deviceName) else enumerator.isBackFacing(deviceName)
    }.getOrDefault(false)

    fun isFront(deviceName: String): Boolean = facing(deviceName, true)

    private fun supportedFormats(deviceName: String): List<CameraEnumerationAndroid.CaptureFormat> =
        runCatching { enumerator.getSupportedFormats(deviceName) }.getOrNull().orEmpty()

    /** Nearest supported capture mode to the request, with a mid-range frame rate. */
    fun chooseFormat(deviceName: String, width: Int, height: Int, fps: Int): Triple<Int, Int, Int> {
        val formats = supportedFormats(deviceName)
        if (formats.isEmpty()) return Triple(width, height, fps)
        val size: Size = CameraEnumerationAndroid.getClosestSupportedSize(
            formats.map { Size(it.width, it.height) },
            width,
            height
        )
        val ranges = formats.filter { it.width == size.width && it.height == size.height }.map { it.framerate }
        val chosen = if (ranges.isEmpty()) {
            fps
        } else {
            val range = CameraEnumerationAndroid.getClosestSupportedFramerateRange(ranges, fps)
            ((range.min + range.max) / 2).coerceAtLeast(1)
        }
        return Triple(size.width, size.height, chosen)
    }

    fun create(
        preferFront: Boolean,
        requestedWidth: Int,
        requestedHeight: Int,
        requestedFps: Int,
        eventsHandler: CameraVideoCapturer.CameraEventsHandler
    ): Selection? {
        val deviceName = pickCamera(preferFront)
        if (deviceName == null) {
            Napier.w("No camera device enumerable on this hardware")
            return null
        }
        val capturer = runCatching { enumerator.createCapturer(deviceName, eventsHandler) }.getOrNull()
        if (capturer == null) {
            Napier.w("Enumerator produced no capturer for $deviceName")
            return null
        }
        val (w, h, fps) = chooseFormat(deviceName, requestedWidth, requestedHeight, requestedFps)
        return Selection(
            capturer = capturer,
            deviceName = deviceName,
            isFrontFacing = isFront(deviceName),
            width = w,
            height = h,
            fps = fps
        )
    }

    /** Other capture sizes for this device, useful for diagnostics only. */
    fun supportedSizes(deviceName: String): List<Pair<Int, Int>> =
        supportedFormats(deviceName).map { it.width to it.height }.distinct()
}
