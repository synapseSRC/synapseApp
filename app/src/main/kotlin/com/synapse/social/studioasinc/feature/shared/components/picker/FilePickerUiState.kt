package com.synapse.social.studioasinc.feature.shared.components.picker

import android.net.Uri

data class FilePickerUiState(
    val mediaItems: List<PickedFile> = emptyList(),
    val fileItems: List<PickedFile> = emptyList(),
    val contactItems: List<PickedFile> = emptyList(),
    val selectedUris: LinkedHashSet<Uri> = LinkedHashSet(),
    val isLoading: Boolean = false,
    val selectedCategory: FilePickerCategory = FilePickerCategory.MEDIA,
    val mediaFilter: MediaFilter = MediaFilter.ALL
)
