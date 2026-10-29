package com.synapse.social.studioasinc.feature.call

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.navigation.AppDestination
import dagger.hilt.android.AndroidEntryPoint
import io.github.aakira.napier.Napier

/**
 * The call screen, outside the app's navigation graph.
 *
 * Incoming calls need an activity of their own for one reason: the system only shows a UI over the lock
 * screen when that activity declares it, and `MainActivity` is a portrait-locked social feed that must
 * not start rendering calls. Everything else is shared — the same [CallScreen], the same [CallViewModel],
 * the same repository underneath — so a ring answered here behaves exactly like one answered inside the
 * app.
 *
 * It is `singleTask` and deliberately not portrait-locked: a call should rotate with the phone, and two
 * rings must never stack two screens.
 */
@AndroidEntryPoint
class CallActivity : ComponentActivity() {

    private var showing: AppDestination.Call? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ringing is the point of this screen, so it has to be visible on a locked phone and it has to
        // wake the display. Both are API 27+; below that the notification's heads-up view is what the
        // user sees, which is a fallback rather than a bug.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val launch = intent.callRoute()
        if (launch == null) {
            Napier.w("CallActivity started without a call to show")
            finish()
            return
        }
        showing = launch
        setContent {
            SynapseTheme {
                CallHost(launch = launch, onClose = ::finish)
            }
        }
    }

    /**
     * A second ring replaces what is on screen instead of stacking behind it.
     *
     * `recreate()` is heavier than swapping a parameter, and deliberately so: [CallViewModel] reads its
     * route once, and a call screen whose arguments had silently changed underneath it would show one
     * peer while answering another.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val incoming = intent.callRoute()
        if (incoming == null || showing == incoming) return
        setIntent(intent)
        showing = incoming
        recreate()
    }

    companion object {

        const val EXTRA_CALL_ID = "com.synapse.social.studioasinc.call.CALL_ID"
        const val EXTRA_PEER_USER_ID = "com.synapse.social.studioasinc.call.PEER_USER_ID"
        const val EXTRA_PEER_NAME = "com.synapse.social.studioasinc.call.PEER_NAME"
        const val EXTRA_PEER_AVATAR = "com.synapse.social.studioasinc.call.PEER_AVATAR"
        const val EXTRA_CHAT_ID = "com.synapse.social.studioasinc.call.CHAT_ID"
        const val EXTRA_IS_VIDEO = "com.synapse.social.studioasinc.call.IS_VIDEO"

        /**
         * Opens the ring *and* answers it.
         *
         * Only the accept action in the notification sets it, and the ViewModel still gates it on the
         * microphone: answering from the shade can bring a permission dialog forward, which is honest,
         * whereas capturing audio before the user can see why would not be.
         */
        const val EXTRA_AUTO_ACCEPT = "com.synapse.social.studioasinc.call.AUTO_ACCEPT"

        /**
         * Reads the call this intent is about, or null when it describes nothing.
         *
         * Extras only, and never a URI: this activity is not exported, so every intent reaching it was
         * built by this app — a notification this app posted, or its accept/decline actions. A caller id
         * is not optional because the ring is found by *who is ringing*, and inventing a call from a
         * half-filled intent is precisely the bug this screen must not have.
         */
        fun Intent.callRoute(): AppDestination.Call? {
            val caller = getStringExtra(EXTRA_PEER_USER_ID)?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            return AppDestination.Call(
                peerUserId = caller,
                peerName = getStringExtra(EXTRA_PEER_NAME).orEmpty(),
                peerAvatarUrl = getStringExtra(EXTRA_PEER_AVATAR),
                chatId = getStringExtra(EXTRA_CHAT_ID),
                isVideo = getBooleanExtra(EXTRA_IS_VIDEO, false),
                isIncoming = true,
                autoAccept = getBooleanExtra(EXTRA_AUTO_ACCEPT, false)
            )
        }
    }
}

/**
 * The one-destination graph that gives [CallViewModel] its route.
 *
 * The ViewModel reads its arguments through type-safe navigation, and hand-writing those SavedStateHandle
 * keys from an activity would tie this screen to a detail of the navigation library. A graph holding a
 * single destination is the supported way to arrive at a route from outside the app, and it costs one
 * back stack that always contains exactly one call.
 */
@Composable
private fun CallHost(
    launch: AppDestination.Call,
    onClose: () -> Unit
) {
    NavHost(
        navController = rememberNavController(),
        startDestination = launch
    ) {
        composable<AppDestination.Call> {
            CallScreen(
                debugToolsEnabled = BuildConfig.DEBUG,
                onClose = onClose
            )
        }
    }
}
