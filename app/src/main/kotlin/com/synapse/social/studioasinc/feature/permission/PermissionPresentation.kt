package com.synapse.social.studioasinc.feature.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.permission.AppPermission
import com.synapse.social.studioasinc.data.permission.PermissionSnapshot
import com.synapse.social.studioasinc.data.permission.PermissionStatus

@StringRes
internal fun AppPermission.titleRes(): Int = when (this) {
    AppPermission.NOTIFICATIONS -> R.string.permission_notifications_title
    AppPermission.MICROPHONE -> R.string.permission_microphone_title
    AppPermission.CAMERA -> R.string.permission_camera_title
}

@StringRes
internal fun AppPermission.purposeRes(): Int = when (this) {
    AppPermission.NOTIFICATIONS -> R.string.permission_notifications_purpose
    AppPermission.MICROPHONE -> R.string.permission_microphone_purpose
    AppPermission.CAMERA -> R.string.permission_camera_purpose
}

@StringRes
internal fun AppPermission.benefitRes(): Int = when (this) {
    AppPermission.NOTIFICATIONS -> R.string.permission_notifications_benefit
    AppPermission.MICROPHONE -> R.string.permission_microphone_benefit
    AppPermission.CAMERA -> R.string.permission_camera_benefit
}

@StringRes
internal fun AppPermission.impactRes(): Int = when (this) {
    AppPermission.NOTIFICATIONS -> R.string.permission_notifications_impact
    AppPermission.MICROPHONE -> R.string.permission_microphone_impact
    AppPermission.CAMERA -> R.string.permission_camera_impact
}

internal fun AppPermission.icon(): ImageVector = when (this) {
    AppPermission.NOTIFICATIONS -> Icons.Rounded.Notifications
    AppPermission.MICROPHONE -> Icons.Rounded.Mic
    AppPermission.CAMERA -> Icons.Rounded.CameraAlt
}

@StringRes
internal fun PermissionStatus.labelRes(): Int = when (this) {
    PermissionStatus.GRANTED -> R.string.permission_status_granted
    PermissionStatus.NOT_REQUESTED -> R.string.permission_status_not_requested
    PermissionStatus.DENIED -> R.string.permission_status_denied
    PermissionStatus.PERMANENTLY_DENIED -> R.string.permission_status_permanently_denied
    PermissionStatus.RESTRICTED -> R.string.permission_status_restricted
}

@StringRes
internal fun PermissionStatus.hintRes(): Int? = when (this) {
    PermissionStatus.DENIED -> R.string.permission_hint_denied
    PermissionStatus.PERMANENTLY_DENIED -> R.string.permission_hint_permanently_denied
    PermissionStatus.RESTRICTED -> R.string.permission_hint_restricted
    PermissionStatus.GRANTED, PermissionStatus.NOT_REQUESTED -> null
}

internal fun PermissionStatus.icon(): ImageVector = when (this) {
    PermissionStatus.GRANTED -> Icons.Rounded.CheckCircle
    PermissionStatus.NOT_REQUESTED -> Icons.Rounded.RadioButtonUnchecked
    PermissionStatus.DENIED -> Icons.Rounded.Cancel
    PermissionStatus.PERMANENTLY_DENIED -> Icons.Rounded.Block
    PermissionStatus.RESTRICTED -> Icons.Rounded.Settings
}

/** Whether this activity can still put [permission] to the user instead of needing system settings. */
internal fun Activity.canAskAbout(permission: AppPermission): Boolean =
    shouldShowRequestPermissionRationale(permission.manifestName)

/** Unwraps the theme wrappers Compose may hand out so the screen can reach its Activity. */
internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Routes a card action to the only path its status allows: the system dialog where Android would still
 * show one, or the app's Settings page where it would not.
 */
internal fun handlePermissionAction(
    context: Context,
    snapshot: PermissionSnapshot,
    onRequest: (AppPermission) -> Unit
) {
    when (snapshot.status) {
        PermissionStatus.NOT_REQUESTED, PermissionStatus.DENIED -> onRequest(snapshot.permission)
        PermissionStatus.PERMANENTLY_DENIED, PermissionStatus.RESTRICTED ->
            context.openPermissionSettings()
        PermissionStatus.GRANTED -> Unit
    }
}

/**
 * Opens this app's page in system Settings, reporting whether it got there.
 *
 * The generic app page is used rather than the permission-specific one, which needs an API check and a
 * per-permission argument; a screen that throws is worse than one extra tap.
 */
internal fun Context.openPermissionSettings(): Boolean = runCatching {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    true
}.getOrDefault(false)
