package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallPermission
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionOutcome
import com.synapse.social.studioasinc.shared.domain.repository.CallPermissionRepository

/**
 * Files what the user just answered, so the next call can plan against it.
 *
 * The policy here is the filter: a permission the user was never asked about must not be written down.
 * A launcher that returns a map of *every* permission the app declares is a common shape, and storing
 * a phantom "DENIED" for the camera because a voice call never asked for it would lock a later video
 * call out of ever asking.
 */
class RecordCallPermissionOutcomeUseCase(
    private val permissionRepository: CallPermissionRepository
) {
    operator fun invoke(
        asked: List<CallPermission>,
        outcomes: Map<CallPermission, CallPermissionOutcome>
    ) {
        val relevant = asked.mapNotNull { permission ->
            outcomes[permission]?.let { permission to it }
        }.toMap()
        if (relevant.isNotEmpty()) permissionRepository.recordOutcome(relevant)
    }
}
