package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageStorageViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _storageUsage = MutableStateFlow<StorageUsageBreakdown?>(null)
    val storageUsage: StateFlow<StorageUsageBreakdown?> = _storageUsage.asStateFlow()

    private val _largeFiles = MutableStateFlow<List<LargeFileInfo>>(emptyList())
    val largeFiles: StateFlow<List<LargeFileInfo>> = _largeFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val breakdown = settingsRepository.getStorageBreakdown()
                _storageUsage.value = breakdown

                val files = settingsRepository.getLargeFiles(5L * 1024 * 1024)
                _largeFiles.value = files
            } catch (e: Exception) {
                android.util.Log.e("ManageStorageViewModel", "Error loading storage data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
