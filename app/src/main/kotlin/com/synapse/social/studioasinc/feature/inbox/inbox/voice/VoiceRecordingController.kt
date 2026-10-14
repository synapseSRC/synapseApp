package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import com.synapse.social.studioasinc.core.media.VoicePlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class VoiceRecordingController(
    private val scope: CoroutineScope,
    private val voiceRecorder: VoiceRecorder,
    private val voicePlayerManager: VoicePlayerManager?,
    private val getOutputFile: () -> File,
    private val timeProvider: TimeProvider = SystemTimeProvider,
    private val onHapticPerform: () -> Unit = {},
    private var onSendVoiceMessage: (File, Long, (Boolean, String?) -> Unit) -> Job? = { _, _, cb -> cb(true, null); null },
    var gestureConfig: VoiceGestureConfig = VoiceGestureConfig(),
    var cancelThresholdPx: Float = 300f,
    var lockThresholdPx: Float = 250f,
    var directionSlopPx: Float = 36f
) {
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Normal)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private var holdJob: Job? = null
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var playerProgressJob: Job? = null

    private var cumulativeX = 0f
    private var cumulativeY = 0f
    private var gestureDirection = GestureDirection.Undecided
    private val gestureNeutralTolerancePx = 1f
    private var lastRawAmplitude = 0f
    private var smoothedAmplitude = 0.1f
    private var recordingStartTime = 0L
    private var pausedAccumulatedDurationMs = 0L
    private var pauseStartTime = 0L
    private var pendingPermissionIntent = false
    private var releaseHandled = false

    fun onPauseBackground() {
        val currentState = _state.value
        if (currentState is VoiceState.Recording) {
            lockRecording()
            togglePause()
        } else if (currentState is VoiceState.Locked && !currentState.isPaused) {
            togglePause()
        }
    }

    fun updateThresholds(cancelPx: Float, lockPx: Float, slopPx: Float) {
        if (cancelPx > 0f) cancelThresholdPx = cancelPx
        if (lockPx > 0f) lockThresholdPx = lockPx
        if (slopPx > 0f) directionSlopPx = slopPx
    }

    fun handleAction(action: VoiceAction) {
        when (action) {
            VoiceAction.OpenVoiceMode -> {
                if (_state.value is VoiceState.Normal || _state.value is VoiceState.Failed) {
                    _state.value = VoiceState.VoiceReady
                }
            }
            VoiceAction.CloseVoiceMode -> {
                if (_state.value is VoiceState.VoiceReady) {
                    _state.value = VoiceState.Normal
                }
            }
            VoiceAction.StartRecording -> {
                releaseHandled = false
                onPressDown()
            }
            is VoiceAction.Dragged -> {
                handleDrag(action.deltaX, action.deltaY)
            }
            VoiceAction.ReleaseRecording -> {
                handleRelease()
            }
            VoiceAction.PointerGestureInterrupted -> {
                when (_state.value) {
                    is VoiceState.Pressing -> {
                        holdJob?.cancel()
                        holdJob = null
                        releaseHandled = true
                        _state.value = VoiceState.Normal
                    }
                    is VoiceState.Recording -> cancelRecording()
                    // A successful lock disposes the drag recognizer by design; do not cancel the recorder.
                    else -> Unit
                }
            }
            VoiceAction.CancelRecording -> {
                when (_state.value) {
                    is VoiceState.Pressing -> {
                        holdJob?.cancel()
                        holdJob = null
                        _state.value = VoiceState.Normal
                    }
                    is VoiceState.Recording, is VoiceState.Locked -> cancelRecording()
                    else -> Unit
                }
            }
            VoiceAction.TogglePause -> {
                togglePause()
            }
            VoiceAction.FinishRecording -> {
                finishRecordingToDraft()
            }
            VoiceAction.DiscardDraft -> {
                discardDraft()
            }
            VoiceAction.TogglePlayDraft -> {
                togglePlayDraft()
            }
            is VoiceAction.SeekDraft -> {
                seekDraft(action.positionMs)
            }
            VoiceAction.SendDraft -> {
                sendDraft()
            }
            is VoiceAction.ErrorOccurred -> {
                _state.value = VoiceState.Failed(action.message)
            }
            VoiceAction.RequestPermission -> {
                requestPermissionAndRecord()
            }
            is VoiceAction.PermissionResult -> {
                onPermissionResult(action.isGranted)
            }
        }
    }

    fun requestPermissionAndRecord() {
        pendingPermissionIntent = true
    }

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            if (pendingPermissionIntent) {
                pendingPermissionIntent = false
                startActualRecording()
                lockRecording()
            }
        } else {
            pendingPermissionIntent = false
            holdJob?.cancel()
            holdJob = null
            _state.value = VoiceState.Normal
        }
    }

    private fun onPressDown() {
        val current = _state.value
        if (current !is VoiceState.Normal && current !is VoiceState.VoiceReady) return

        _state.value = VoiceState.Pressing

        holdJob?.cancel()
        holdJob = scope.launch {
            delay(gestureConfig.holdThresholdMs)
            if (_state.value is VoiceState.Pressing) {
                startActualRecording()
            }
        }
    }

    private fun startActualRecording() {
        try {
            val file = getOutputFile()
            voiceRecorder.start(file, scope)
            recordingStartTime = timeProvider.currentTimeMillis()
            pausedAccumulatedDurationMs = 0L
            cumulativeX = 0f
            cumulativeY = 0f
            gestureDirection = GestureDirection.Undecided
            smoothedAmplitude = 0.1f

            _state.value = VoiceState.Recording()
            onHapticPerform()

            startTimerAndAmplitudeTracking()
        } catch (e: Exception) {
            _state.value = VoiceState.Failed(e.message ?: "Failed to start recording")
        }
    }

    private fun handleDrag(deltaX: Float, deltaY: Float) {
        val currentState = _state.value
        if (currentState !is VoiceState.Recording) return

        cumulativeX += deltaX
        cumulativeY += deltaY

        var leftDistance = (-cumulativeX).coerceAtLeast(0f)
        var upDistance = (-cumulativeY).coerceAtLeast(0f)

        // Once a gesture picks an axis, keep it latched until the active drag axis returns
        // to the mic's neutral position. Perpendicular movement cannot switch cancel/lock.
        val wasLatched = gestureDirection != GestureDirection.Undecided
        when (gestureDirection) {
            GestureDirection.Cancel -> if (leftDistance <= gestureNeutralTolerancePx) {
                cumulativeX = 0f
                cumulativeY = 0f
                leftDistance = 0f
                upDistance = 0f
                gestureDirection = GestureDirection.Undecided
            }
            GestureDirection.Lock -> if (upDistance <= gestureNeutralTolerancePx) {
                cumulativeX = 0f
                cumulativeY = 0f
                leftDistance = 0f
                upDistance = 0f
                gestureDirection = GestureDirection.Undecided
            }
            GestureDirection.Undecided -> Unit
        }

        // A fresh direction is selected only after one axis clearly leads by slop.
        // If the previous axis just returned to neutral, wait for the next pointer delta.
        if (!wasLatched && gestureDirection == GestureDirection.Undecided) {
            val candidate = when {
                leftDistance - upDistance >= directionSlopPx && leftDistance > directionSlopPx -> GestureDirection.Cancel
                upDistance - leftDistance >= directionSlopPx && upDistance > directionSlopPx -> GestureDirection.Lock
                else -> GestureDirection.Undecided
            }
            if (candidate != GestureDirection.Undecided) gestureDirection = candidate
        }

        val cancelOffset = if (gestureDirection == GestureDirection.Cancel) -leftDistance else 0f
        val lockOffset = if (gestureDirection == GestureDirection.Lock) -upDistance else 0f
        val cancelProgress = (leftDistance / cancelThresholdPx).coerceIn(0f, 1f)
            .takeIf { gestureDirection == GestureDirection.Cancel } ?: 0f
        val lockProgress = (upDistance / lockThresholdPx).coerceIn(0f, 1f)
            .takeIf { gestureDirection == GestureDirection.Lock } ?: 0f
        // Commit cancellation as soon as the normalized drag reaches its threshold; release is not required.
        if (gestureDirection == GestureDirection.Cancel && leftDistance >= cancelThresholdPx) {
            cancelRecording()
            return
        }

        // A successful lock commits once and subsequent pointer events are ignored in Locked mode.
        if (gestureDirection == GestureDirection.Lock && upDistance >= lockThresholdPx) {
            lockRecording()
            return
        }

        _state.value = currentState.copy(
            slideOffsetPx = cancelOffset,
            dragOffsetYPx = lockOffset,
            cancelProgress = cancelProgress,
            lockProgress = lockProgress,
        )
    }

    private fun handleRelease() {
        if (releaseHandled) return
        releaseHandled = true
        val currentState = _state.value
        if (currentState is VoiceState.Pressing) {
            holdJob?.cancel()
            holdJob = null
            // Single tap only if pointer was not moved significantly
            if (abs(cumulativeX) < 15f && abs(cumulativeY) < 15f) {
                startActualRecording()
                lockRecording()
            } else {
                _state.value = VoiceState.Normal
            }
            return
        }

        if (currentState is VoiceState.Recording) {
            finishRecordingToDraft()
        }
    }

    private fun returnFromAbandonedSlide(currentState: VoiceState.Recording) {
        gestureDirection = GestureDirection.Undecided
        cumulativeX = 0f
        cumulativeY = 0f
        _state.value = currentState.copy(
            slideOffsetPx = 0f,
            dragOffsetYPx = 0f,
            cancelProgress = 0f,
            lockProgress = 0f
        )
    }

    private fun cancelRecording() {
        if (_state.value !is VoiceState.Recording && _state.value !is VoiceState.Locked) return

        holdJob?.cancel()
        holdJob = null
        stopTimerAndAmplitudeTracking()
        try {
            voiceRecorder.cancel()
        } catch (_: Exception) {}
        onHapticPerform()
        releaseHandled = true
        _state.value = VoiceState.Normal
    }

    private fun lockRecording() {
        val currentState = _state.value
        if (currentState is VoiceState.Recording) {
            releaseHandled = true
            onHapticPerform()
            _state.value = VoiceState.Locked(
                durationMs = currentState.durationMs,
                amplitudeHistory = currentState.amplitudeHistory,
                isPaused = false
            )
        }
    }

    private fun togglePause() {
        val currentState = _state.value
        if (currentState is VoiceState.Locked) {
            if (currentState.isPaused) {
                try {
                    voiceRecorder.resume()
                } catch (e: Exception) {
                    try { voiceRecorder.cancel() } catch (_: Exception) {}
                    _state.value = VoiceState.Failed(e.message ?: "Failed to resume recording")
                    return
                }
                pausedAccumulatedDurationMs += (timeProvider.currentTimeMillis() - pauseStartTime)
                _state.value = currentState.copy(isPaused = false)
                startTimerAndAmplitudeTracking()
            } else {
                try {
                    voiceRecorder.pause()
                } catch (e: Exception) {
                    try { voiceRecorder.cancel() } catch (_: Exception) {}
                    _state.value = VoiceState.Failed(e.message ?: "Failed to pause recording")
                    return
                }
                stopTimerAndAmplitudeTracking()
                pauseStartTime = timeProvider.currentTimeMillis()
                _state.value = currentState.copy(isPaused = true)
            }
            onHapticPerform()
        }
    }

    private fun finishRecordingToDraft() {
        val currentState = _state.value
        if (currentState !is VoiceState.Recording && currentState !is VoiceState.Locked) return

        val currentAmpHistory = when (currentState) {
            is VoiceState.Recording -> currentState.amplitudeHistory
            is VoiceState.Locked -> currentState.amplitudeHistory
        }
        val currentDuration = when (currentState) {
            is VoiceState.Recording -> currentState.durationMs
            is VoiceState.Locked -> currentState.durationMs
        }
        val activeRecordedDuration = try { voiceRecorder.getRecordedDurationMs() } catch (_: Exception) { 0L }
        val duration = if (activeRecordedDuration > 0L) {
            activeRecordedDuration
        } else if (currentDuration > 0L) {
            currentDuration
        } else if (recordingStartTime > 0L) {
            (timeProvider.currentTimeMillis() - recordingStartTime - pausedAccumulatedDurationMs).coerceAtLeast(0L)
        } else {
            500L
        }

        stopTimerAndAmplitudeTracking()

        when (val result = voiceRecorder.stop()) {
            is VoiceRecorderStopResult.Success -> {
                if (duration >= 600L) {
                    onHapticPerform()
                    _state.value = VoiceState.VoiceDraft(
                        audioFile = result.file,
                        durationMs = duration,
                        amplitudeHistory = currentAmpHistory,
                        playbackPositionMs = 0L,
                        isPlaying = false
                    )
                } else {
                    result.file.delete()
                    _state.value = VoiceState.VoiceReady
                }
            }
            is VoiceRecorderStopResult.EmptyOrTooShort -> {
                _state.value = VoiceState.Normal
            }
            is VoiceRecorderStopResult.Error -> {
                _state.value = VoiceState.Failed(result.cause.message ?: "Failed to stop recorder")
            }
        }
    }

    private fun discardDraft() {
        val currentState = _state.value
        if (currentState is VoiceState.VoiceDraft) {
            voicePlayerManager?.stop()
            stopPlayerProgressTracker()
            currentState.audioFile.delete()
            onHapticPerform()
            _state.value = VoiceState.Normal
        } else if (currentState is VoiceState.Failed && currentState.draftFile != null) {
            currentState.draftFile.delete()
            _state.value = VoiceState.Normal
        }
    }

    private fun togglePlayDraft() {
        val currentState = _state.value
        if (currentState is VoiceState.VoiceDraft && voicePlayerManager != null) {
            if (currentState.isPlaying) {
                voicePlayerManager.pause()
                stopPlayerProgressTracker()
                _state.value = currentState.copy(isPlaying = false)
            } else {
                val mediaPath = currentState.audioFile.absolutePath
                voicePlayerManager.play(url = mediaPath, mediaPath = mediaPath)
                _state.value = currentState.copy(isPlaying = true)
                startPlayerProgressTracker()
            }
        }
    }

    private fun seekDraft(positionMs: Long) {
        val currentState = _state.value
        if (currentState is VoiceState.VoiceDraft && voicePlayerManager != null) {
            voicePlayerManager.seekTo(positionMs)
            _state.value = currentState.copy(playbackPositionMs = positionMs)
        }
    }

    fun setSendHandler(handler: (File, Long, (Boolean, String?) -> Unit) -> Job?) {
        this.onSendVoiceMessage = handler
    }

    private fun sendDraft() {
        val currentState = _state.value
        val (audioFile, durationMs) = when (currentState) {
            is VoiceState.VoiceDraft -> currentState.audioFile to currentState.durationMs
            is VoiceState.Failed -> (currentState.draftFile ?: return) to currentState.durationMs
            else -> return
        }
        if (!audioFile.exists()) {
            _state.value = VoiceState.Failed("Audio draft file missing", draftFile = null, durationMs = 0L)
            return
        }

        voicePlayerManager?.stop()
        stopPlayerProgressTracker()

        _state.value = VoiceState.Sending(audioFile)

        val isHandled = AtomicBoolean(false)
        var timeoutJob: Job? = null
        var uploadJob: Job? = null

        timeoutJob = scope.launch {
            delay(45000L)
            if (isHandled.compareAndSet(false, true)) {
                uploadJob?.cancel()
                _state.value = VoiceState.Failed(
                    error = "Upload timed out",
                    draftFile = audioFile,
                    durationMs = durationMs
                )
            }
        }

        uploadJob = onSendVoiceMessage(audioFile, durationMs) { isSuccess, errorMsg ->
            if (isHandled.compareAndSet(false, true)) {
                timeoutJob.cancel()
                if (isSuccess) {
                    audioFile.delete()
                    _state.value = VoiceState.Normal
                } else {
                    _state.value = VoiceState.Failed(
                        error = errorMsg ?: "Failed to upload voice message",
                        draftFile = audioFile,
                        durationMs = durationMs
                    )
                }
            }
        }
    }

    private fun startTimerAndAmplitudeTracking() {
        stopTimerAndAmplitudeTracking()

        amplitudeJob = scope.launch {
            voiceRecorder.amplitudeFlow.collect { rawAmp ->
                lastRawAmplitude = rawAmp.toFloat()
            }
        }

        timerJob = scope.launch {
            while (true) {
                delay(60)
                val elapsed = timeProvider.currentTimeMillis() - recordingStartTime - pausedAccumulatedDurationMs

                // Perceptual logarithmic scaling (dB-like curve) for high audio reactivity
                val rawNormalized = (lastRawAmplitude / 32767f).coerceIn(0f, 1f)
                val dbNormalized = if (rawNormalized > 0f) {
                    (kotlin.math.log10(1f + 19f * rawNormalized) / kotlin.math.log10(20f)).coerceIn(0f, 1f)
                } else 0f

                val targetAmp = (0.08f + 0.92f * dbNormalized).coerceIn(0.08f, 1f)
                // Fast attack for immediate speech response, smooth release for natural fluid decay
                smoothedAmplitude = if (targetAmp > smoothedAmplitude) {
                    smoothedAmplitude * 0.20f + targetAmp * 0.80f
                } else {
                    smoothedAmplitude * 0.88f + targetAmp * 0.12f
                }

                _state.update { current ->
                    when (current) {
                        is VoiceState.Recording -> {
                            val history = current.amplitudeHistory.toMutableList()
                            history.add(smoothedAmplitude)
                            if (history.size > 40) {
                                history.removeAt(0)
                            }
                            current.copy(
                                durationMs = elapsed,
                                amplitudeHistory = history
                            )
                        }
                        is VoiceState.Locked -> {
                            if (!current.isPaused) {
                                val history = current.amplitudeHistory.toMutableList()
                                history.add(smoothedAmplitude)
                                if (history.size > 40) {
                                    history.removeAt(0)
                                }
                                current.copy(
                                    durationMs = elapsed,
                                    amplitudeHistory = history
                                )
                            } else {
                                current
                            }
                        }
                        else -> current
                    }
                }
            }
        }
    }

    private fun stopTimerAndAmplitudeTracking() {
        timerJob?.cancel()
        timerJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null
    }

    private fun startPlayerProgressTracker() {
        stopPlayerProgressTracker()
        playerProgressJob = scope.launch {
            while (true) {
                delay(100)
                val currentState = _state.value
                if (currentState is VoiceState.VoiceDraft && voicePlayerManager != null) {
                    val pState = voicePlayerManager.playbackState.value
                    if (pState.isCompleted) {
                        _state.value = currentState.copy(isPlaying = false, playbackPositionMs = 0L)
                        stopPlayerProgressTracker()
                        break
                    } else {
                        _state.value = currentState.copy(
                            isPlaying = pState.isPlaying,
                            playbackPositionMs = pState.currentPositionMs
                        )
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun stopPlayerProgressTracker() {
        playerProgressJob?.cancel()
        playerProgressJob = null
    }

    private enum class GestureDirection { Undecided, Cancel, Lock }

    fun cleanup() {
        pendingPermissionIntent = false
        holdJob?.cancel()
        holdJob = null
        stopTimerAndAmplitudeTracking()
        stopPlayerProgressTracker()
        val s = _state.value
        if (s is VoiceState.VoiceDraft) {
            s.audioFile.delete()
        } else if (s is VoiceState.Failed && s.draftFile != null) {
            s.draftFile.delete()
        } else if (s is VoiceState.Recording || s is VoiceState.Locked) {
            try { voiceRecorder.cancel() } catch (_: Exception) {}
        }
        _state.value = VoiceState.Normal
    }
}
