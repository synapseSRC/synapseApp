package com.synapse.social.studioasinc.data.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the live state of each [AppPermission] from the operating system.
 *
 * The status is always derived on demand; nothing here caches a grant. [canAsk] is supplied by the
 * caller because only an Activity can answer `shouldShowRequestPermissionRationale`.
 */
@Singleton
class AndroidPermissionStatusProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: PermissionOnboardingStore
) {
    fun snapshot(canAsk: (String) -> Boolean): List<PermissionSnapshot> =
        AppPermission.entries.map { permission ->
            PermissionSnapshot(permission = permission, status = status(permission, canAsk))
        }

    fun status(permission: AppPermission, canAsk: (String) -> Boolean): PermissionStatus {
        val granted = ContextCompat.checkSelfPermission(context, permission.manifestName) ==
            PackageManager.PERMISSION_GRANTED

        if (permission == AppPermission.NOTIFICATIONS) {
            val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            return when {
                // Before Android 13 there is no runtime dialog; the per-app toggle lives in Settings.
                !permission.isRuntimeRequestable ->
                    if (notificationsEnabled) PermissionStatus.GRANTED else PermissionStatus.RESTRICTED
                !granted -> when {
                    canAsk(permission.manifestName) -> PermissionStatus.DENIED
                    store.hasRequested(permission) -> PermissionStatus.PERMANENTLY_DENIED
                    else -> PermissionStatus.NOT_REQUESTED
                }
                // The grant can exist while the channel or app toggle is switched off in Settings.
                notificationsEnabled -> PermissionStatus.GRANTED
                else -> PermissionStatus.RESTRICTED
            }
        }

        return when {
            granted -> PermissionStatus.GRANTED
            canAsk(permission.manifestName) -> PermissionStatus.DENIED
            store.hasRequested(permission) -> PermissionStatus.PERMANENTLY_DENIED
            else -> PermissionStatus.NOT_REQUESTED
        }
    }
}
