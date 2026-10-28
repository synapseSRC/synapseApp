package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession

/**
 * The precondition every in-call control shares.
 *
 * Kept as a function that returns the rejection rather than one that throws, so each UseCase decides
 * what its own success type is — a control that answers `Result<Unit>` and one that answers
 * `Result<CallMediaChange>` can still fail for the same reason in the same words.
 */
internal fun CallSession.endedRejection(operation: String): IllegalStateException? =
    if (phase == CallPhase.ENDED) {
        IllegalStateException("Cannot $operation: the call has already ended")
    } else {
        null
    }
