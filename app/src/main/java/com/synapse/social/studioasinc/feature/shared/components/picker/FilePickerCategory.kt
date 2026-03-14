package com.synapse.social.studioasinc.feature.shared.components.picker

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import com.synapse.social.studioasinc.R

enum class FilePickerCategory(
    @StringRes val labelResId: Int,
    val icon: ImageVector
) {
    MEDIA(R.string.picker_category_media, Icons.Default.Image),
    DOCS(R.string.picker_category_docs, Icons.Default.InsertDriveFile),
    AUDIO(R.string.picker_category_audio, Icons.Default.AudioFile),
    FILE(R.string.picker_category_file, Icons.Default.Folder),
    CONTACT(R.string.picker_category_contact, Icons.Default.Person)
}
