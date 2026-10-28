package com.synapse.social.studioasinc.data.permission

import android.Manifest
import android.os.Build

/**
 * The runtime permissions this app actually uses, in the order they are presented.
 *
 * Only permissions backed by a shipped feature belong here: notifications for message and call alerts,
 * the microphone for voice notes and calls, and the camera for calls, stories and profile photos.
 */
enum class AppPermission(val manifestName: String) {
    NOTIFICATIONS(Manifest.permission.POST_NOTIFICATIONS),
    MICROPHONE(Manifest.permission.RECORD_AUDIO),
    CAMERA(Manifest.permission.CAMERA);

    /** Notifications only became a runtime permission in Android 13 (API 33). */
    val isRuntimeRequestable: Boolean
        get() = this != NOTIFICATIONS || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
