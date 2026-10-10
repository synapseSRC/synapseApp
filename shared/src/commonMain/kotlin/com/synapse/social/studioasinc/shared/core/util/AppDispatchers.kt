package com.synapse.social.studioasinc.shared.core.util
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import kotlinx.coroutines.CoroutineDispatcher

expect object AppDispatchers {
    val IO: CoroutineDispatcher
}
