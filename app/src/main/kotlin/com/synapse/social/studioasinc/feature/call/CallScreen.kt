package com.synapse.social.studioasinc.feature.call

import android.app.Activity
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.data.call.CallSimulationAction
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.call.components.CallAudioRouteSheet
import com.synapse.social.studioasinc.feature.call.components.CallDebugSheet
import com.synapse.social.studioasinc.feature.call.components.CallDimens
import com.synapse.social.studioasinc.feature.call.components.CallEndedContent
import com.synapse.social.studioasinc.feature.call.components.CallEndConfirmationDialog
import com.synapse.social.studioasinc.feature.call.components.CallPermissionContent
import com.synapse.social.studioasinc.feature.call.components.CallStartFailedContent
import com.synapse.social.studioasinc.feature.call.components.CallSystemBarEffect
import com.synapse.social.studioasinc.feature.call.components.CallVideoContent
import com.synapse.social.studioasinc.feature.call.components.CallVoiceContent
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    onClose: () -> Unit,
    debugToolsEnabled: Boolean = false,
    viewModel: CallViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val actions = remember(viewModel) {
        CallActions(
            onAccept = viewModel::accept,
            onDecline = viewModel::decline,
            onEnd = viewModel::end,
            onEndAndClose = viewModel::endAndClose,
            onToggleMute = viewModel::toggleMute,
            onToggleSpeaker = viewModel::toggleSpeaker,
            onSelectAudioRoute = viewModel::selectAudioRoute,
            onToggleCamera = viewModel::toggleCamera,
            onFlipCamera = viewModel::flipCamera,
            onSwitchMediaType = viewModel::switchMediaType,
            onSimulate = viewModel::simulate,
            onRedial = viewModel::redial,
            onAskPermissions = viewModel::askPermissions,
            onPermissionsAnswered = viewModel::permissionsAnswered
        )
    }

    CallScreenContent(
        state = state,
        actions = actions,
        debugToolsEnabled = debugToolsEnabled,
        onClose = onClose
    )
}

@Composable
fun CallScreenContent(
    state: CallUiState,
    actions: CallActions,
    debugToolsEnabled: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = state.session
    var showRouteSheet by rememberSaveable { mutableStateOf(false) }
    var showDebugSheet by rememberSaveable { mutableStateOf(false) }
    var showEndDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val accessibilityRunning = remember(context) {
        context.getSystemService(AccessibilityManager::class.java)?.isEnabled == true
    }
    val scheme = MaterialTheme.colorScheme
    val prompt = state.permissionPrompt

    // The plan the dialog was opened for, held here because the answer has to be read back against it:
    // a launcher returns manifest names, and only the plan that asked can say which capability each one
    // was, and whether a "no" was the kind that Settings can undo.
    var askedPlan by remember { mutableStateOf<CallPermissionPlan?>(null) }
    val activity = LocalContext.current as? Activity
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val plan = askedPlan
        askedPlan = null
        if (plan != null) {
            actions.onPermissionsAnswered(
                plan.mapOutcomes(results) { permission -> activity?.canAskAbout(permission) == true }
            )
        }
    }

    val surfaceActions = actions.copy(
        onOpenAudioSheet = { showRouteSheet = true },
        onOpenDebugSheet = { if (debugToolsEnabled) showDebugSheet = true },
        onOpenAppSettings = { context.openAppSettings() }
    )

    // Keyed on the request, not the plan: an answer that has just been given re-publishes the same plan
    // with the same key, and must not put the dialog back in front of a user who just closed it.
    LaunchedEffect(prompt?.requestKey) {
        val blocked = prompt ?: return@LaunchedEffect
        if (!blocked.plan.canAsk) return@LaunchedEffect
        askedPlan = blocked.plan
        permissionLauncher.launch(blocked.plan.manifestNames())
    }

    val isVideoSurface = session != null &&
        session.mediaType == CallMediaType.VIDEO &&
        session.phase != CallPhase.ENDED
    CallSystemBarEffect(
        useLightIcons = isVideoSurface || scheme.background.luminance() < 0.5f,
        keepScreenOn = session != null && session.phase != CallPhase.ENDED
    )

    LaunchedEffect(state.closeRequested) {
        if (state.closeRequested) onClose()
    }

    LaunchedEffect(session?.phase) {
        if (session == null || session.phase == CallPhase.ENDED) {
            showRouteSheet = false
            showDebugSheet = false
            showEndDialog = false
        }
    }

    LaunchedEffect(session?.id, session?.phase) {
        val ended = session ?: return@LaunchedEffect
        if (ended.phase == CallPhase.ENDED && ended.connectedAtMillis == null && !accessibilityRunning) {
            delay(CallDimens.AutoDismissMillis)
            onClose()
        }
    }

    BackHandler {
        when {
            session == null || session.phase == CallPhase.ENDED -> onClose()
            session.direction == CallDirection.INCOMING && session.phase == CallPhase.RINGING ->
                surfaceActions.onDecline()

            session.direction == CallDirection.OUTGOING &&
                (session.phase == CallPhase.DIALING || session.phase == CallPhase.RINGING) ->
                surfaceActions.onEndAndClose()

            else -> showEndDialog = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        val current = session
        if (current == null) {
            when {
                // Nothing was dialled yet and the call cannot be dialled until the user answers a
                // permission question: say what is missing instead of showing a blank screen.
                prompt != null -> CallPermissionContent(
                    plan = prompt.plan,
                    onTryAgain = surfaceActions.onAskPermissions,
                    onOpenSettings = surfaceActions.onOpenAppSettings,
                    onDone = onClose
                )

                state.startFailed -> CallStartFailedContent(onDone = onClose)
            }
        } else {
            BoxWithConstraints {
                val isCompact = maxHeight < CallDimens.CompactThresholdHeight ||
                    LocalDensity.current.fontScale > CallDimens.LargeFontScale
                val reducedMotion = AnimationUtil.rememberReducedMotion()
                AnimatedContent(
                    targetState = current.callVisualSurface(prompt),
                    transitionSpec = { callSurfaceTransition(reducedMotion) },
                    label = "callSurface"
                ) { surface ->
                    when (surface) {
                        CallVisualSurface.VOICE -> CallVoiceContent(
                            session = current,
                            elapsedMs = state.elapsedMs,
                            actions = surfaceActions,
                            debugToolsEnabled = debugToolsEnabled,
                            isCompact = isCompact
                        )

                        CallVisualSurface.VIDEO -> CallVideoContent(
                            session = current,
                            elapsedMs = state.elapsedMs,
                            actions = surfaceActions,
                            debugToolsEnabled = debugToolsEnabled
                        )

                        // A live call stopped at a permission: the ringing or dialling UI cannot offer
                        // an accept that would be refused, so the question takes the screen until it is
                        // answered. "Done" leaves the call, which is the honest third option.
                        CallVisualSurface.PERMISSION -> CallPermissionContent(
                            plan = prompt?.plan ?: return@AnimatedContent,
                            onTryAgain = surfaceActions.onAskPermissions,
                            onOpenSettings = surfaceActions.onOpenAppSettings,
                            onDone = onClose
                        )

                        CallVisualSurface.ENDED -> CallEndedContent(
                            session = current,
                            elapsedMs = state.elapsedMs,
                            onCallAgain = surfaceActions.onRedial,
                            onDone = onClose
                        )
                    }
                }
            }
        }
    }

    if (showRouteSheet && session != null && session.phase != CallPhase.ENDED) {
        CallAudioRouteSheet(
            currentRoute = session.audioRoute,
            onSelectRoute = surfaceActions.onSelectAudioRoute,
            onDismiss = { showRouteSheet = false }
        )
    }

    if (showDebugSheet && debugToolsEnabled && session != null && session.phase != CallPhase.ENDED) {
        CallDebugSheet(
            onSimulate = { action: CallSimulationAction -> surfaceActions.onSimulate(action) },
            onDismiss = { showDebugSheet = false }
        )
    }

    if (showEndDialog) {
        CallEndConfirmationDialog(
            onConfirm = {
                showEndDialog = false
                surfaceActions.onEndAndClose()
            },
            onDismiss = { showEndDialog = false }
        )
    }
}

private enum class CallVisualSurface { VOICE, VIDEO, PERMISSION, ENDED }

private fun CallSession.callVisualSurface(prompt: CallPermissionPrompt?): CallVisualSurface = when {
    // An ended call keeps its own surface: the summary of what happened outranks a question that can
    // no longer change it.
    phase == CallPhase.ENDED -> CallVisualSurface.ENDED

    prompt != null -> CallVisualSurface.PERMISSION

    mediaType == CallMediaType.VIDEO -> CallVisualSurface.VIDEO
    else -> CallVisualSurface.VOICE
}

private fun callSurfaceTransition(reducedMotion: Boolean): ContentTransform =
    if (reducedMotion) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        (fadeIn(tween(SURFACE_FADE_MILLIS)) + scaleIn(tween(SURFACE_FADE_MILLIS), initialScale = 0.98f))
            .togetherWith(
                fadeOut(tween(SURFACE_FADE_MILLIS)) + scaleOut(
                    tween(SURFACE_FADE_MILLIS),
                    targetScale = 0.98f
                )
            )
    }

private const val SURFACE_FADE_MILLIS = 180
