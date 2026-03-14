package com.synapse.social.studioasinc.feature.shared.components.picker

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.feature.shared.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FilePickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : BaseViewModel<FilePickerUiState>(FilePickerUiState()) {



    fun loadMedia() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            val items = withContext(Dispatchers.IO) {
                queryMediaStore()
            }
            updateState { it.copy(mediaItems = items, isLoading = false) }
        }
    }

    private fun queryMediaStore(): List<PickedFile> {
        val items = mutableListOf<PickedFile>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE
        )

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        val queryUri = MediaStore.Files.getContentUri("external")

        context.contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: ""
                val mimeType = cursor.getString(mimeTypeColumn) ?: ""
                val size = cursor.getLong(sizeColumn)

                val uri = ContentUris.withAppendedId(queryUri, id)

                items.add(PickedFile(uri, mimeType, name, size))
            }
        }
        return items
    }

    fun toggleSelection(uri: Uri, maxSelection: Int) {
        updateState { state ->
            val currentSelections = state.selectedUris.toMutableSet()
            if (currentSelections.contains(uri)) {
                currentSelections.remove(uri)
            } else {
                if (currentSelections.size < maxSelection) {
                    currentSelections.add(uri)
                }
            }
            state.copy(selectedUris = currentSelections)
        }
    }

    fun clearSelection() {
        updateState { it.copy(selectedUris = emptySet()) }
    }

    fun setCategory(category: FilePickerCategory) {
        updateState { it.copy(selectedCategory = category) }
    }
}
