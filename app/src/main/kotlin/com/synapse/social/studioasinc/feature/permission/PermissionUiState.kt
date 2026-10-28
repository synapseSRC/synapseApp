package com.synapse.social.studioasinc.feature.permission

import com.synapse.social.studioasinc.data.permission.AppPermission
import com.synapse.social.studioasinc.data.permission.PermissionSnapshot

enum class PermissionOnboardingStep { CARDS, SUMMARY }

/**
 * A request the screen has been asked to put to the system, keyed so that configuration changes and
 * recompositions cannot launch the dialog twice.
 */
data class PendingPermissionRequest(
    val permission: AppPermission,
    val requestKey: Int,
    val launched: Boolean = false
)

data class PermissionOnboardingUiState(
    val step: PermissionOnboardingStep = PermissionOnboardingStep.CARDS,
    val permissions: List<PermissionSnapshot> = emptyList(),
    val pending: PendingPermissionRequest? = null,
    val rationaleFor: AppPermission? = null,
    val skipped: Boolean = false,
    val isCompleting: Boolean = false
)

data class PermissionManagerUiState(
    val permissions: List<PermissionSnapshot> = emptyList(),
    val pending: PendingPermissionRequest? = null,
    val rationaleFor: AppPermission? = null
)

sealed interface PermissionOnboardingEvent {
    data object EnterMainApp : PermissionOnboardingEvent
}
