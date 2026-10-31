package com.synapse.social.studioasinc.feature.echo.room

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.synapse.social.studioasinc.data.call.toManifestNames
import com.synapse.social.studioasinc.feature.call.canAskAbout
import com.synapse.social.studioasinc.feature.call.mapOutcomes
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermission
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionOutcome
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan

/**
 * Turns the room's permission plan into a system dialog and the answer back into domain outcomes.
 *
 * It lives with the room rather than the call screen because the plan it reads is produced by a
 * different ViewModel, but the three platform-only facts it needs — manifest names, whether a
 * rationale would still show, and whether a "no" is permanent — are the same ones the call UI
 * already owns, so they are imported rather than restated.
 *
 * Keyed on [requestKey], not on the plan: an answer that has just been given republishes the same
 * plan and must not reopen the dialog the user just closed.
 */
@Composable
internal fun EchoMicrophonePermissionEffect(
    prompt: EchoPermissionPrompt?,
    onAnswered: (Map<CallPermission, CallPermissionOutcome>) -> Unit
) {
    val activity = LocalContext.current as? Activity
    var askedPlan by remember { mutableStateOf<CallPermissionPlan?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val plan = askedPlan
        askedPlan = null
        if (plan != null) {
            onAnswered(plan.mapOutcomes(results) { permission -> activity?.canAskAbout(permission) == true })
        }
    }

    LaunchedEffect(prompt?.requestKey) {
        val blocked = prompt ?: return@LaunchedEffect
        if (!blocked.plan.canAsk) return@LaunchedEffect
        askedPlan = blocked.plan
        launcher.launch(blocked.plan.requestable().toManifestNames())
    }
}
