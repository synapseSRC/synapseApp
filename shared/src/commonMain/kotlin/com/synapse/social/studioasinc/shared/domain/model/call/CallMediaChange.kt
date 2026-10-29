package com.synapse.social.studioasinc.shared.domain.model.call

/**
 * What changing a call's media type actually required.
 *
 * Two outcomes that look identical to the user and are nothing alike to the rest of the app: a live
 * connection renegotiates, and a call that has not connected has to be placed again. Returning the
 * distinction instead of a bare success is what stops the UI from showing a video screen for a call
 * that is still ringing as audio.
 */
sealed interface CallMediaChange {

    /** The connected call now carries [mediaType]; both peers were told through the session row. */
    data class Renegotiated(val mediaType: CallMediaType) : CallMediaChange

    /**
     * Nothing had connected, so the ring was cancelled and this request should be dialled instead.
     *
     * The old session is already ended when this is returned — a re-dial must never race the
     * invitation it replaces.
     */
    data class Redial(val request: CallRequest) : CallMediaChange
}
