package com.synapse.social.studioasinc.shared.core.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual object AppDispatchers {
    actual val IO: CoroutineDispatcher = Dispatchers.IO
}
