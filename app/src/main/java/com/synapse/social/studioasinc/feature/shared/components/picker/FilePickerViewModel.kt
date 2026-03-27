package com.synapse.social.studioasinc.feature.shared.components.picker

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
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
            val items = withContext(Dispatchers.IO) { queryMediaStore() }
            updateState { it.copy(mediaItems = items, isLoading = false) }
        }
    }

    fun loadFiles(category: FilePickerCategory) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            val items = withContext(Dispatchers.IO) { queryFiles(category) }
            updateState { it.copy(fileItems = items, isLoading = false) }
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            val items = withContext(Dispatchers.IO) { queryContacts() }
            updateState { it.copy(contactItems = items, isLoading = false) }
        }
    }

    private fun queryContacts(): List<PickedFile> {
        val items = mutableListOf<PickedFile>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, projection, null, null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id.toString())
                items.add(PickedFile(uri = uri, mimeType = "text/vcard", fileName = name, size = 0))
            }
        }
        return items
    }

    private fun queryFiles(category: FilePickerCategory): List<PickedFile> {
        val items = mutableListOf<PickedFile>()
        val queryUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE
        )
        val (selection, selectionArgs) = when (category) {
            FilePickerCategory.AUDIO -> "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?" to
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString())
            FilePickerCategory.DOCS -> "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?" to
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString())
            else -> "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ? AND ${MediaStore.Files.FileColumns.MEDIA_TYPE} != ? AND ${MediaStore.Files.FileColumns.MEDIA_TYPE} != ?" to
                arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString()
                )
        }
        context.contentResolver.query(
            queryUri, projection, selection, selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val mime = cursor.getString(mimeCol) ?: continue
                if (mime.isBlank()) continue
                items.add(
                    PickedFile(
                        uri = ContentUris.withAppendedId(queryUri, id),
                        mimeType = mime,
                        fileName = cursor.getString(nameCol) ?: "",
                        size = cursor.getLong(sizeCol)
                    )
                )
            }
        }
        return items
    }

    private fun queryMediaStore(): List<PickedFile> {
        val items = mutableListOf<PickedFile>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Video.VideoColumns.DURATION
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val queryUri = MediaStore.Files.getContentUri("external")

        context.contentResolver.query(
            queryUri, projection, selection, selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val durCol = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val mimeType = cursor.getString(mimeCol) ?: ""
                val duration = if (durCol >= 0 && mimeType.startsWith("video/"))
                    cursor.getLong(durCol).takeIf { it > 0 } else null

                items.add(
                    PickedFile(
                        uri = ContentUris.withAppendedId(queryUri, id),
                        mimeType = mimeType,
                        fileName = cursor.getString(nameCol) ?: "",
                        size = cursor.getLong(sizeCol),
                        duration = duration
                    )
                )
            }
        }
        return items
    }

    val filteredMediaItems get() = uiState.value.let { state ->
        when (state.mediaFilter) {
            MediaFilter.IMAGES -> state.mediaItems.filter { it.mimeType.startsWith("image/") }
            MediaFilter.VIDEOS -> state.mediaItems.filter { it.mimeType.startsWith("video/") }
            MediaFilter.ALL -> state.mediaItems
        }
    }

    fun setFilter(filter: MediaFilter) {
        updateState { it.copy(mediaFilter = filter) }
    }

    fun toggleSelection(uri: Uri, maxSelection: Int) {
        updateState { state ->
            val current = LinkedHashSet(state.selectedUris)
            if (current.contains(uri)) current.remove(uri)
            else if (current.size < maxSelection) current.add(uri)
            state.copy(selectedUris = current)
        }
    }

    fun clearSelection() {
        updateState { it.copy(selectedUris = LinkedHashSet()) }
    }

    fun setCategory(category: FilePickerCategory) {
        updateState { it.copy(selectedCategory = category) }
    }
}
