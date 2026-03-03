package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StorageProviderConfigViewModel : ViewModel() {






    private val _photoProviders = MutableStateFlow(setOf("ImgBB"))
    val photoProviders: StateFlow<Set<String>> = _photoProviders.asStateFlow()

    private val _videoProviders = MutableStateFlow(setOf("Cloudinary"))
    val videoProviders: StateFlow<Set<String>> = _videoProviders.asStateFlow()

    private val _fileProviders = MutableStateFlow(setOf("Supabase"))
    val fileProviders: StateFlow<Set<String>> = _fileProviders.asStateFlow()





    fun togglePhotoProvider(provider: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = _photoProviders.value.toMutableSet()
            if (enabled) {
                current.add(provider)
            } else {
                current.remove(provider)
            }



            if (current.isNotEmpty()) {
                _photoProviders.value = current
            }
        }
    }

    fun toggleVideoProvider(provider: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = _videoProviders.value.toMutableSet()
            if (enabled) {
                current.add(provider)
            } else {
                current.remove(provider)
            }
             if (current.isNotEmpty()) {
                _videoProviders.value = current
            }
        }
    }

    fun toggleFileProvider(provider: String, enabled: Boolean) {
         viewModelScope.launch {
            val current = _fileProviders.value.toMutableSet()
            if (enabled) {
                current.add(provider)
            } else {
                current.remove(provider)
            }
             if (current.isNotEmpty()) {
                _fileProviders.value = current
            }
        }
    }
}
