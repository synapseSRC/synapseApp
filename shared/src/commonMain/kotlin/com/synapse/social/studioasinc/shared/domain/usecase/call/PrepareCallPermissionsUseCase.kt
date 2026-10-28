package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan
import com.synapse.social.studioasinc.shared.domain.repository.CallPermissionRepository

/**
 * Answer "may this call start, and if not, what would fix it?" before anything is dialled.
 *
 * A UseCase rather than a ViewModel helper because the answer is policy: a voice call must not ask
 * for a camera, and a permission that only Settings can undo must not be re-asked. Both of those
 * would otherwise be re-derived at every entry point that can start a call.
 *
 * Returns the plan directly instead of a `Result`, because an unanswered question is not a failure —
 * "you still have to go to Settings" is a successful answer to this question, and encoding it as a
 * `Result.failure` would push callers into catching what they should be reading.
 */
class PrepareCallPermissionsUseCase(
    private val permissionRepository: CallPermissionRepository
) {
    operator fun invoke(mediaType: CallMediaType): CallPermissionPlan =
        permissionRepository.planFor(mediaType)
}
