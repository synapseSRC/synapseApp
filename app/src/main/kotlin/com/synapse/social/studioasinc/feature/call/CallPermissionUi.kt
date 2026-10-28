package com.synapse.social.studioasinc.feature.call

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.synapse.social.studioasinc.data.call.manifestName
import com.synapse.social.studioasinc.data.call.toManifestNames
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermission
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionOutcome
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan

/**
 * The bridge between a permission dialog and the domain's view of what the user said.
 *
 * Three things are known only here: the manifest names behind a capability, whether the platform would
 * still show a rationale, and how to reach the system screen that can undo a permanent refusal. None of
 * them is business policy, which is why [CallPermissionPlan] stays pure Kotlin and this file does not.
 */

/** The names to hand to the system launcher, in the order the plan lists them. */
internal fun CallPermissionPlan.manifestNames(): Array<String> = requestable().toManifestNames()

/**
 * Reads the launcher's answer as the domain understands it.
 *
 * A denial is only permanent when the platform has stopped offering to explain it — on Android that
 * means "don't ask again" was ticked, or the two-request limit was reached. Getting this wrong in either
 * direction is user-visible: call a first "no" permanent and the app sends the user to Settings for
 * something a second tap could have granted; call a permanent one repeatable and the dialog loops
 * forever against a decision that has already been made.
 */
internal fun CallPermissionPlan.mapOutcomes(
    results: Map<String, Boolean>,
    rationaleWouldShow: (CallPermission) -> Boolean
): Map<CallPermission, CallPermissionOutcome> = requestable().associateWith { permission ->
    when {
        results[permission.manifestName] == true -> CallPermissionOutcome.GRANTED
        rationaleWouldShow(permission) -> CallPermissionOutcome.DENIED
        else -> CallPermissionOutcome.DENIED_PERMANENTLY
    }
}

/** Whether this activity can still put [permission] to the user, as opposed to needing Settings. */
internal fun Activity.canAskAbout(permission: CallPermission): Boolean =
    shouldShowRequestPermissionRationale(permission.manifestName)

/**
 * Opens this app's page in system Settings, reporting whether it got there.
 *
 * The generic app page rather than the permission-specific one: the latter needs an API-level check and
 * a per-permission argument, and a screen that throws is worse than one where the user taps
 * "Permissions" once. The return value is what lets the UI fall back to plain instructions on a ROM
 * with no settings screen at all.
 */
internal fun Context.openAppSettings(): Boolean = runCatching {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    true
}.getOrDefault(false)
