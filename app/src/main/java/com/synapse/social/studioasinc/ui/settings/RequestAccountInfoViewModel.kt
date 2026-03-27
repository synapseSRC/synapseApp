package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.local.database.SettingsDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale



sealed class RequestStatus {
    object Idle : RequestStatus()
    object Processing : RequestStatus()
    data class Ready(val availableUntil: String) : RequestStatus()
}



data class RequestAccountInfoUiState(
    val isAccountInfoSelected: Boolean = false,
    val isChannelActivitySelected: Boolean = false,
    val isAutoReportEnabled: Boolean = false,
    val status: RequestStatus = RequestStatus.Idle,
    val error: String? = null
)



class RequestAccountInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore.getInstance(application)





    private val _uiState = MutableStateFlow(RequestAccountInfoUiState())
    val uiState: StateFlow<RequestAccountInfoUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }



    private fun loadSettings() {
        viewModelScope.launch {
            val accountAuto = settingsDataStore.accountReportsAutoCreate.first()
            val channelsAuto = settingsDataStore.channelsReportsAutoCreate.first()


            _uiState.value = _uiState.value.copy(
                isAutoReportEnabled = accountAuto || channelsAuto
            )
        }
    }

    fun toggleAccountSelection() {
        if (_uiState.value.status !is RequestStatus.Processing) {
            _uiState.value = _uiState.value.copy(
                isAccountInfoSelected = !_uiState.value.isAccountInfoSelected
            )
        }
    }

    fun toggleChannelSelection() {
        if (_uiState.value.status !is RequestStatus.Processing) {
            _uiState.value = _uiState.value.copy(
                isChannelActivitySelected = !_uiState.value.isChannelActivitySelected
            )
        }
    }

    fun toggleAutoReport(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAutoReportEnabled = enabled)

            settingsDataStore.setAccountReportsAutoCreate(enabled)
            settingsDataStore.setChannelsReportsAutoCreate(enabled)
        }
    }

    fun requestReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = RequestStatus.Processing)

            try {

                delay(3000)


                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, 3)
                val readyDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(calendar.time)

                _uiState.value = _uiState.value.copy(
                    status = RequestStatus.Ready(readyDate)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = RequestStatus.Idle,
                    error = "Failed to request report. Please try again."
                )
            }
        }
    }



    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
