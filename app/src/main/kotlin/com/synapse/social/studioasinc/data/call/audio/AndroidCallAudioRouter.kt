package com.synapse.social.studioasinc.data.call.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.synapse.social.studioasinc.core.media.AndroidSoundPlayer
import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aakira.napier.Napier
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Routes call audio through the device the user is actually holding.
 *
 * Every operation here is a request to the Android audio stack followed by a read-back, never an
 * assumption. `MODE_IN_COMMUNICATION` is what makes the echo path, the earpiece gain curve and the
 * Bluetooth SCO link behave like a phone call rather than media playback, and it is the reason the
 * call screen may only claim a route once the stack agrees with it.
 *
 * API 31+ takes the modern path — `setCommunicationDevice`, which performs SCO setup and priority
 * itself. Below that the legacy speaker/SCO flags are used, which is genuinely more fragile: SCO
 * takes a moment to come up, so a Bluetooth pick reads back as EARPIECE until the device callback
 * corrects it. The UI therefore follows [activeRoute], never the tap.
 */
@Singleton
class AndroidCallAudioRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soundPlayer: AndroidSoundPlayer
) : CallAudioRouter {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val _activeRoute = MutableStateFlow(CallAudioRoute.EARPIECE)
    override val activeRoute: StateFlow<CallAudioRoute> = _activeRoute.asStateFlow()

    private var previousMode = AudioManager.MODE_NORMAL
    private var previousSpeaker = false
    private var watcher: AudioDeviceCallback? = null
    @Volatile private var inCall = false

    private var focusRequest: AudioFocusRequest? = null

    /**
     * Focus is advisory here, not a gate.
     *
     * A refused request means another app is louder — not that the call cannot happen — so the call
     * proceeds and the loss is only logged. Ending a call because a music player kept focus would hang
     * up on the user's behalf, which is exactly the decision this class is not allowed to make.
     */
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> Napier.d("Call audio focus gained")
            AudioManager.AUDIOFOCUS_LOSS -> Napier.w("Call audio focus lost; another app owns the output")
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> Napier.d("Call audio focus paused temporarily")
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Napier.d("Call audio focus ducked")
        }
    }

    override fun start(video: Boolean): CallAudioRoute {
        val manager = audioManager ?: return _activeRoute.value
        synchronized(this) {
            if (!inCall) {
                previousMode = manager.mode
                previousSpeaker = manager.isSpeakerphoneOn
                inCall = true
                requestFocus(manager)
                // The interface stops talking the moment this device starts carrying a conversation: a
                // notification blip out of the earpiece is close enough to the microphone to be picked up
                // again, and the peer has no part in it.
                soundPlayer.setInCallAudioMode(true)
            }
            runCatching { manager.mode = AudioManager.MODE_IN_COMMUNICATION }
                .onFailure { Napier.w("Call audio mode refused: ${it.message}") }
        }
        watchDevices(manager)
        // A video call is watched by more than the person holding the phone, so it opens on the
        // loudspeaker; a voice call defers to whatever is already connected.
        return apply(manager, if (video) CallAudioRoute.SPEAKER else connectedRoute() ?: CallAudioRoute.EARPIECE)
    }

    override fun select(route: CallAudioRoute): CallAudioRoute {
        val manager = audioManager ?: return _activeRoute.value
        return apply(manager, route)
    }

    override fun stop() {
        val manager = audioManager ?: return
        synchronized(this) {
            if (!inCall) return
            inCall = false
            soundPlayer.setInCallAudioMode(false)
            // Released before the mode goes back: while the call is still the audio owner, giving
            // focus up is what lets the previous app (music, a podcast) resume.
            abandonFocus(manager)
            runCatching { stopSco(manager) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching { manager.clearCommunicationDevice() }
            } else {
                runCatching { manager.isSpeakerphoneOn = false }
                runCatching { manager.isBluetoothScoOn = false }
            }
            runCatching { manager.mode = previousMode }
                .onFailure { Napier.w("Audio mode restore failed: ${it.message}") }
            // Only the flag this call turned on gets put back; a speaker already up before the call
            // stays up.
            runCatching { manager.isSpeakerphoneOn = previousSpeaker }
        }
        releaseDeviceWatcher(manager)
        _activeRoute.value = readRoute(manager)
        Napier.d("Call audio released")
    }

    // ---- Application -------------------------------------------------------------

    private fun requestFocus(manager: AudioManager) {
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            // A call is never ducked: half-volume speech is not a call. Another app's playback
            // should stop rather than play quietly under the conversation.
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
            .also { focusRequest = it }

        val result = runCatching { manager.requestAudioFocus(request) }
            .onFailure { Napier.w("Audio focus request failed: ${it.message}") }
            .getOrNull()
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Napier.w("Call audio focus was not granted; continuing anyway (focus is advisory)")
        }
    }

    /** Safe to call twice, and safe after a refused request — both are no-ops on the platform. */
    private fun abandonFocus(manager: AudioManager) {
        val request = focusRequest ?: return
        runCatching { manager.abandonAudioFocusRequest(request) }
            .onFailure { Napier.w("Audio focus release failed: ${it.message}") }
    }

    private fun apply(manager: AudioManager, route: CallAudioRoute): CallAudioRoute {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) applyModern(manager, route)
            else applyLegacy(manager, route)
        }.onFailure { Napier.w("Audio route '$route' could not be applied: ${it.message}") }
        val settled = readRoute(manager)
        if (settled != route && route == CallAudioRoute.BLUETOOTH) {
            Napier.w("Bluetooth is not carrying the call yet (no SCO link, or permission withheld)")
        }
        _activeRoute.value = settled
        return settled
    }

    /**
     * API 31+: Android chooses from the communication-device list, so an unavailable device is
     * simply absent rather than a half-configured route.
     */
    private fun applyModern(manager: AudioManager, route: CallAudioRoute) {
        val types = when (route) {
            CallAudioRoute.SPEAKER -> intArrayOf(TYPE_BUILTIN_SPEAKER)
            CallAudioRoute.EARPIECE -> intArrayOf(TYPE_BUILTIN_EARPIECE)
            CallAudioRoute.BLUETOOTH -> intArrayOf(TYPE_BLUETOOTH_SCO, TYPE_BLUETOOTH_A2DP)
            CallAudioRoute.WIRED_HEADSET -> intArrayOf(TYPE_WIRED_HEADSET, TYPE_WIRED_HEADPHONES, TYPE_USB_HEADSET)
        }
        val device = firstMatching(manager.availableCommunicationDevices, types)
        if (device != null) {
            manager.setCommunicationDevice(device)
        } else {
            // Nothing to switch to: leave the live device carrying the call rather than yanking it
            // back to the earpiece, which would cut a headset link mid-sentence.
            Napier.d("No communication device for route '$route'; current one kept")
        }
    }

    @Suppress("DEPRECATION")
    private fun applyLegacy(manager: AudioManager, route: CallAudioRoute) {
        when (route) {
            CallAudioRoute.SPEAKER -> {
                stopSco(manager)
                manager.isSpeakerphoneOn = true
            }

            CallAudioRoute.EARPIECE -> {
                stopSco(manager)
                manager.isSpeakerphoneOn = false
            }

            CallAudioRoute.BLUETOOTH -> {
                manager.isSpeakerphoneOn = false
                manager.startBluetoothSco()
                manager.isBluetoothScoOn = true
            }

            CallAudioRoute.WIRED_HEADSET -> {
                stopSco(manager)
                manager.isSpeakerphoneOn = false
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun stopSco(manager: AudioManager) {
        runCatching { manager.isBluetoothScoOn = false }
        runCatching { manager.stopBluetoothSco() }
    }

    /** The route the hardware reports — the only version of the truth worth showing. */
    private fun readRoute(manager: AudioManager): CallAudioRoute {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.communicationDevice?.type?.let { return it.toRoute() }
        }
        val connected = connectedRoute()
        val speaker = runCatching { manager.isSpeakerphoneOn }.getOrDefault(false)
        val scoUp = runCatching { manager.isBluetoothScoOn }.getOrDefault(false)
        return when {
            connected == CallAudioRoute.BLUETOOTH && scoUp -> CallAudioRoute.BLUETOOTH
            connected == CallAudioRoute.WIRED_HEADSET && !speaker -> CallAudioRoute.WIRED_HEADSET
            speaker -> CallAudioRoute.SPEAKER
            else -> CallAudioRoute.EARPIECE
        }
    }

    /** The best device currently attached, or null when only the built-ins are. */
    private fun connectedRoute(manager: AudioManager? = audioManager): CallAudioRoute? {
        val outputs = runCatching { manager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.toList() }
            .getOrNull().orEmpty()
        val wired = outputs.firstOrNull { it.type == TYPE_WIRED_HEADSET || it.type == TYPE_WIRED_HEADPHONES || it.type == TYPE_USB_HEADSET }
        val bluetooth = outputs.firstOrNull { it.type == TYPE_BLUETOOTH_SCO || it.type == TYPE_BLUETOOTH_A2DP }
        // Hands-free wins over cables: a phone paired to a car is not going back to a wire.
        return when {
            bluetooth != null -> CallAudioRoute.BLUETOOTH
            wired != null -> CallAudioRoute.WIRED_HEADSET
            else -> null
        }
    }

    // ---- Device changes ----------------------------------------------------------

    private fun watchDevices(manager: AudioManager) {
        if (watcher != null) return
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) = refresh()

            override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = refresh()
        }
        watcher = callback
        runCatching { manager.registerAudioDeviceCallback(callback, null) }
            .onFailure { Napier.w("Audio device monitoring unavailable: ${it.message}") }
    }

    private fun releaseDeviceWatcher(manager: AudioManager) {
        watcher?.let { current ->
            runCatching { manager.unregisterAudioDeviceCallback(current) }
            watcher = null
        }
    }

    /**
     * Republishes the route when the hardware moves.
     *
     * Headphones pulled out mid-call must move the audio back on their own, and the displayed route
     * has to follow: the user's last tap is stale the moment a device disappears. On API 31+ the
     * system performs the switch itself, so this only keeps the readout honest; on older devices the
     * platform falls back the same way once the SCO/headset link drops.
     */
    private fun refresh() {
        val manager = audioManager ?: return
        if (!inCall) return
        val route = readRoute(manager)
        if (route != _activeRoute.value) {
            _activeRoute.value = route
            Napier.d("Audio route settled on '$route' after a hardware change")
        }
    }
}

// Numeric device types, written out instead of using `AudioDeviceInfo` constants because several of
// those names only exist on API levels above the ones this class has to support at runtime. A wrong
// id routes audio to the wrong speaker silently, so each value is fixed here and checked against the
// platform's own numbering (TYPE_BUILTIN_EARPIECE=1 … TYPE_USB_HEADSET=22).
private const val TYPE_BUILTIN_EARPIECE = 1
private const val TYPE_BUILTIN_SPEAKER = 2
private const val TYPE_WIRED_HEADSET = 3
private const val TYPE_WIRED_HEADPHONES = 4
private const val TYPE_BLUETOOTH_SCO = 7
private const val TYPE_BLUETOOTH_A2DP = 15
private const val TYPE_USB_HEADSET = 22

private fun firstMatching(devices: List<AudioDeviceInfo>, types: IntArray): AudioDeviceInfo? {
    types.forEach { type -> devices.firstOrNull { it.type == type }?.let { return it } }
    return null
}

private fun Int.toRoute(): CallAudioRoute = when (this) {
    TYPE_BLUETOOTH_SCO, TYPE_BLUETOOTH_A2DP -> CallAudioRoute.BLUETOOTH
    TYPE_WIRED_HEADSET, TYPE_WIRED_HEADPHONES, TYPE_USB_HEADSET -> CallAudioRoute.WIRED_HEADSET
    TYPE_BUILTIN_SPEAKER -> CallAudioRoute.SPEAKER
    else -> CallAudioRoute.EARPIECE
}
