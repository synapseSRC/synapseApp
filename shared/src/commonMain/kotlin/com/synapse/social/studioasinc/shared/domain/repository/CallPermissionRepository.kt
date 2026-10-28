package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermission
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionOutcome
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan

/**
 * Reports and remembers what the device will let a call do.
 *
 * The interface exists because two very different things have to be combined to answer "may we
 * dial?": what the operating system grants right now, and what the user already said when asked.
 * The first is a platform query, the second is stored state, and neither belongs in a ViewModel.
 *
 * No permission name from any platform appears here — see [CallPermission].
 */
interface CallPermissionRepository {

    /** Current grant state for [mediaType], including whether asking again can still work. */
    fun planFor(mediaType: CallMediaType): CallPermissionPlan

    /**
     * Records what the user answered, so the next [planFor] can tell a repeatable "no" from a
     * "no" that only Settings can undo.
     */
    fun recordOutcome(outcomes: Map<CallPermission, CallPermissionOutcome>)
}
