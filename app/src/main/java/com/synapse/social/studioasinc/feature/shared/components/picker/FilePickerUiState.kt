package com.synapse.social.studioasinc.feature.shared.components.picker

import android.net.Uri

data class FilePickerUiState(
    val mediaItems: List<PickedFile> = emptyList(),
    val selectedUris: Set<Uri> = emptySet(),
    val isLoading: Boolean = false,
    val selectedCategory: FilePickerCategory = FilePickerCategory.MEDIA
)
