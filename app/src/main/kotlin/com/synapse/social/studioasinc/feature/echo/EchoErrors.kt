package com.synapse.social.studioasinc.feature.echo

import androidx.annotation.StringRes
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoBackendException
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoErrorKind

/**
 * The kind behind a failure, in one place.
 *
 * Screens never read exception messages: the backend's `SYNAPSE_ECHO_*` tokens are mapped to
 * [EchoErrorKind] in the data layer, and everything the user sees comes from a string resource.
 */
fun Throwable.toEchoErrorKind(): EchoErrorKind =
    (this as? EchoBackendException)?.kind ?: EchoErrorKind.UNKNOWN

@StringRes
fun EchoErrorKind.messageRes(): Int = when (this) {
    EchoErrorKind.NOT_AUTHENTICATED -> R.string.echo_error_not_authenticated
    EchoErrorKind.FORBIDDEN -> R.string.echo_error_forbidden
    EchoErrorKind.BLOCKED -> R.string.echo_error_blocked
    EchoErrorKind.BANNED -> R.string.echo_error_banned
    EchoErrorKind.NOT_FOUND -> R.string.echo_error_not_found
    EchoErrorKind.ENDED -> R.string.echo_error_ended
    EchoErrorKind.STAGE_FULL -> R.string.echo_error_stage_full
    EchoErrorKind.NOT_AUDIENCE -> R.string.echo_error_not_audience
    EchoErrorKind.INVALID_INPUT -> R.string.echo_error_invalid_input
    EchoErrorKind.BACKEND_UNREACHABLE -> R.string.echo_error_backend_unreachable
    EchoErrorKind.AUDIO_FAILED -> R.string.echo_error_audio_failed
    EchoErrorKind.UNKNOWN -> R.string.echo_error_unknown
}
