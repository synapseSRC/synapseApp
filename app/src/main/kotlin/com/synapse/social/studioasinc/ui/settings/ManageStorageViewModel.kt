package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.core.util.MediaCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ManageStorageViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    application: Application
) : AndroidViewModel(application) {

    private val mediaCache = MediaCache(application)

    private val _storageUsage = MutableStateFlow<StorageUsageBreakdown?>(null)
    val storageUsage: StateFlow<StorageUsageBreakdown?> = _storageUsage.asStateFlow()

    private val _largeFiles = MutableStateFlow<List<LargeFileInfo>>(emptyList())
    val largeFiles: StateFlow<List<LargeFileInfo>> = _largeFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _keepMediaDays = MutableStateFlow(7)
    val keepMediaDays: StateFlow<Int> = _keepMediaDays.asStateFlow()
    private val _maxCacheSizeGB = MutableStateFlow(5)
    val maxCacheSizeGB: StateFlow<Int> = _maxCacheSizeGB.asStateFlow()

    init {
        loadData()
        settingsRepository.keepMediaDays
            .onEach { _keepMediaDays.value = it }
            .launchIn(viewModelScope)
        settingsRepository.maxCacheSizeGB
            .onEach { _maxCacheSizeGB.value = it }
            .launchIn(viewModelScope)
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                refreshStorageData()
            } catch (e: Exception) {
                android.util.Log.e("ManageStorageViewModel", "Error loading storage data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun refreshStorageData() {
        val breakdown = settingsRepository.getStorageBreakdown()
        _storageUsage.value = breakdown

        val files = settingsRepository.getLargeFiles(5L * 1024 * 1024)
        _largeFiles.value = files
    }

    fun clearEntireCache() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                settingsRepository.clearCache()
                mediaCache.clear()
                refreshStorageData()
            } catch (e: Exception) {
                android.util.Log.e("ManageStorageViewModel", "Error clearing cache", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteLargeFile(fileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Delete using direct File and clear from MediaCache if it matches
                val file = File(fileId)
                if (file.exists()) {
                    if (file.delete()) {
                        mediaCache.remove(fileId) // Keep cache in sync
                        withContext(Dispatchers.Main) {
                            refreshStorageData()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ManageStorageViewModel", "Error deleting large file", e)
            }
        }
    }

    fun setKeepMediaDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setKeepMediaDays(days)
        }
    }

    fun setMaxCacheSizeGB(gb: Int) {
        viewModelScope.launch {
            settingsRepository.setMaxCacheSizeGB(gb)
        }
    }
}
