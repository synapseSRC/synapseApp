package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.call.CallErrorKind
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession

/**
 * What the Calls tab renders.
 *
 * [error] is a [CallErrorKind] rather than a message: the backend's wording is diagnostics, and
 * the screen has its own string for what the user can do about it. [hasLoaded] separates "this
 * account has never made a call" from "we have not asked yet", so the empty state never appears
 * as if it were an answer before a read has happened.
 */
data class CallsUiState(
    val history: List<CallSession> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val error: CallErrorKind? = null
)
