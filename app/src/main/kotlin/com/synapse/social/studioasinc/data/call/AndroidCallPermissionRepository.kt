package com.synapse.social.studioasinc.data.call

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermission
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionOutcome
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan
import com.synapse.social.studioasinc.shared.domain.model.call.requiredCallPermissions
import com.synapse.social.studioasinc.shared.domain.repository.CallPermissionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Answers "may this device capture audio and video right now" from two sources.
 *
 * The live grant state comes from the platform, which is the only authority on it — a cached "yes"
 * would let a revoked permission open a call that records nothing. What the user *said* is cached,
 * because that is the one thing the platform will not tell an application-level context, and it is
 * what separates a dialog worth showing from a dialog that will never be answered.
 */
@Singleton
class AndroidCallPermissionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : CallPermissionRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun planFor(mediaType: CallMediaType): CallPermissionPlan {
        val required = requiredCallPermissions(mediaType)
        val missing = required.filterNot { isGranted(it) }
        val blocked = missing.filter { remembered(it) == CallPermissionOutcome.DENIED_PERMANENTLY }
        return CallPermissionPlan(
            mediaType = mediaType,
            required = required,
            missing = missing.filterNot { it in blocked },
            needsSettings = blocked
        )
    }

    override fun recordOutcome(outcomes: Map<CallPermission, CallPermissionOutcome>) {
        if (outcomes.isEmpty()) return
        prefs.edit().apply {
            outcomes.forEach { (permission, outcome) ->
                // A grant wins over an old refusal: the user went to Settings and changed it.
                if (outcome == CallPermissionOutcome.GRANTED) remove(key(permission))
                else putString(key(permission), outcome.name)
            }
        }.apply()
    }

    private fun isGranted(permission: CallPermission): Boolean =
        context.checkSelfPermission(permission.manifestName) == PackageManager.PERMISSION_GRANTED

    private fun remembered(permission: CallPermission): CallPermissionOutcome? =
        prefs.getString(key(permission), null)
            ?.let { stored -> CallPermissionOutcome.entries.firstOrNull { it.name == stored } }

    private fun key(permission: CallPermission) = "call_permission_${permission.name}"

    private companion object {
        const val FILE_NAME = "call_permissions"
    }
}
