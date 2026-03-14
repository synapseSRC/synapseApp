package com.synapse.social.studioasinc.feature.shared.components.picker

import android.net.Uri

data class PickedFile(
    val uri: Uri,
    val mimeType: String,
    val fileName: String,
    val size: Long
)
