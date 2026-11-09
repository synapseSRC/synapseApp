package com.synapse.social.studioasinc.data.remote.services.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.synapse.social.studioasinc.shared.domain.repository.IncomingCallWatcher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Direct FCM data messages.
 *
 * OneSignal owns the notification path — the fan-out that reads `notifications` rows is the deployed
 * OneSignal integration, and a push tapped there goes through
 * [com.synapse.social.studioasinc.core.navigation.NotificationNavigationCoordinator]. What arrives here
 * is a data message sent straight to the device, which the calling feature does use: an `incoming_call`
 * hint is how a ring reaches a process that has no realtime connection open.
 *
 * The hint is treated as exactly that. Nothing in this file decides anything about a call from the
 * payload — it only asks the watch to look again, and the watch reads the backend. A forged, duplicated
 * or stale push can therefore wake the app, and nothing more: no ringing row, no ringing screen.
 */
@AndroidEntryPoint
class SynapseFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var incomingCallWatcher: IncomingCallWatcher

    /**
     * Registration callbacks in place of the retired `onNewToken`. Firebase deprecated
     * [FirebaseMessagingService.onNewToken] in firebase-messaging 26 and points here instead; the
     * argument identifies this app instance's registration with FCM. OneSignal reads the device
     * token itself, so both are debug-only — the unregister case is logged too, because a token
     * that silently stops being valid is exactly the thing you want visible in logcat.
     */
    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        Log.d(TAG, "FCM registered: $installationId")
    }

    override fun onUnregistered(installationId: String) {
        super.onUnregistered(installationId)
        Log.d(TAG, "FCM unregistered: $installationId")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "From: ${message.from}")

        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${message.data}")
        }

        message.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        if (message.data[KEY_TYPE] == TYPE_INCOMING_CALL) {
            // Idempotent when already watching, and re-reads the open rows either way — which is the whole
            // point of a wake-up. Errors are not reported to the user from here: a hint that led nowhere is
            // not a call, and saying so would be a false alarm.
            incomingCallWatcher.start()
        }
    }

    companion object {
        private const val TAG = "SynapseFCM"
        private const val KEY_TYPE = "type"
        private const val TYPE_INCOMING_CALL = "incoming_call"
    }
}
