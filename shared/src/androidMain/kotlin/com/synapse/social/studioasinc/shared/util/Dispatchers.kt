package com.synapse.social.studioasinc.shared.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val Dispatchers.MeshIO: CoroutineDispatcher
    get() = Dispatchers.IO
