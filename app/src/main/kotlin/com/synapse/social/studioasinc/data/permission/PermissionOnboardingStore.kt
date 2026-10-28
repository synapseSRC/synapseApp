package com.synapse.social.studioasinc.data.permission

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers what the permission onboarding has already shown, never whether a permission is granted.
 *
 * Completion is keyed by account so switching users cannot inherit another account's decision. Request
 * history is device-wide because that is how the operating system remembers it — it is only used to tell
 * "never asked" apart from "won't ask again".
 */
@Singleton
class PermissionOnboardingStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isCompleted(userId: String): Boolean =
        preferences.getString(completionKey(userId), null) != null

    fun markCompleted(userId: String, skipped: Boolean) {
        preferences.edit()
            .putString(completionKey(userId), if (skipped) VALUE_SKIPPED else VALUE_COMPLETED)
            // Synchronous so a process death right after finishing cannot replay the onboarding.
            .commit()
    }

    fun hasRequested(permission: AppPermission): Boolean =
        preferences.getBoolean(requestedKey(permission), false)

    fun markRequested(permission: AppPermission) {
        preferences.edit().putBoolean(requestedKey(permission), true).apply()
    }

    private fun completionKey(userId: String) = "completion:$userId"

    private fun requestedKey(permission: AppPermission) = "requested:${permission.name}"

    private companion object {
        const val PREFERENCES_NAME = "permission_onboarding"
        const val VALUE_COMPLETED = "completed"
        const val VALUE_SKIPPED = "skipped"
    }
}
