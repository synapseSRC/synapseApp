package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatFoldersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _chatFolders = MutableStateFlow<List<ChatFolder>>(emptyList())
    val chatFolders: StateFlow<List<ChatFolder>> = _chatFolders.asStateFlow()

    init {
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            val json = settingsRepository.chatFoldersJson.firstOrNull()
            if (!json.isNullOrBlank()) {
                try {
                    val parsedFolders = Json.decodeFromString<List<ChatFolder>>(json)
                    _chatFolders.value = parsedFolders
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun addFolder(name: String) {
        val newFolder = ChatFolder(
            id = UUID.randomUUID().toString(),
            name = name
        )
        val updatedList = _chatFolders.value + newFolder
        _chatFolders.value = updatedList
        saveFolders(updatedList)
    }

    fun renameFolder(folderId: String, newName: String) {
        val updatedList = _chatFolders.value.map { if (it.id == folderId) it.copy(name = newName) else it }
        _chatFolders.value = updatedList
        saveFolders(updatedList)
    }

    fun reorderFolders(from: Int, to: Int) {
        val list = _chatFolders.value.toMutableList()
        list.add(to, list.removeAt(from))
        _chatFolders.value = list
        saveFolders(list)
    }

    fun removeFolder(folderId: String) {
        val updatedList = _chatFolders.value.filter { it.id != folderId }
        _chatFolders.value = updatedList
        saveFolders(updatedList)
    }

    private fun saveFolders(folders: List<ChatFolder>) {
        viewModelScope.launch {
            val json = Json.encodeToString(folders)
            settingsRepository.setChatFoldersJson(json)
        }
    }
}