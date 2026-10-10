package com.synapse.social.studioasinc.data.remote.services.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.onesignal.OneSignal
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SynapseFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")
        
        // OneSignal automatically picks up the FCM token, 
        // but we can manually pass it if needed or log it for debugging.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // OneSignal usually handles messages automatically if they come from OneSignal.
        // If you send direct FCM messages, they will appear here.
        Log.d(TAG, "From: ${message.from}")
        
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${message.data}")
        }
        
        message.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    companion object {
        private const val TAG = "SynapseFCM"
    }
}
