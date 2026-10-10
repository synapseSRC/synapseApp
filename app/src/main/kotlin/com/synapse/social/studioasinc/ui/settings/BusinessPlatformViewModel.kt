package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.model.business.AccountType
import com.synapse.social.studioasinc.shared.domain.model.business.AnalyticsData
import com.synapse.social.studioasinc.shared.domain.model.business.DataPoint
import com.synapse.social.studioasinc.shared.domain.model.business.PostAnalytics
import com.synapse.social.studioasinc.shared.domain.model.business.RevenueData
import com.synapse.social.studioasinc.shared.domain.model.business.VerificationStatus
import com.synapse.social.studioasinc.shared.domain.repository.BusinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusinessPlatformViewModel @Inject constructor(
    private val businessRepository: BusinessRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        BusinessPlatformState(
            accountType = AccountType.PERSONAL,
            isBusinessAccount = false,
            analytics = null,
            monetizationEnabled = false,
            revenue = null,
            verificationStatus = VerificationStatus.NOT_APPLIED
        )
    )
    val state: StateFlow<BusinessPlatformState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadBusinessData()
    }

    fun loadBusinessData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userId = authRepository.getCurrentUserId()

                if (userId != null) {
                    val businessAccountResult = businessRepository.getBusinessAccount(userId)
                    val businessAccount = businessAccountResult.getOrNull()

                    val accountType = businessAccount?.accountType ?: AccountType.PERSONAL

                    val analyticsResult = businessRepository.getAnalytics(userId)
                    val analytics = analyticsResult.getOrNull()

                    val revenueResult = businessRepository.getRevenue(userId)
                    val revenue = revenueResult.getOrNull()

                    _state.value = _state.value.copy(
                        accountType = accountType,
                        isBusinessAccount = accountType != AccountType.PERSONAL,
                        monetizationEnabled = businessAccount?.monetizationEnabled ?: false,
                        verificationStatus = businessAccount?.verificationStatus ?: VerificationStatus.NOT_APPLIED,
                        analytics = analytics,
                        revenue = revenue
                    )
                }
            } catch (e: Exception) {
                _error.value = "Failed to load business data: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun switchToBusinessAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch

                businessRepository.createBusinessAccount(userId)
                    .onSuccess {
                        loadBusinessData()
                    }
                    .onFailure { e ->
                        _error.value = "Failed to switch account type: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "Failed to switch account type: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleMonetization(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch

                businessRepository.updateMonetization(userId, enabled)
                    .onSuccess {
                        _state.value = _state.value.copy(monetizationEnabled = enabled)
                    }
                    .onFailure { e ->
                        _error.value = "Failed to update monetization settings: ${e.message}"
                        loadBusinessData()
                    }
            } catch (e: Exception) {
                _error.value = "Failed to update monetization settings: ${e.message}"
                loadBusinessData()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun applyForVerification() {
        viewModelScope.launch {
             _isLoading.value = true
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch

                businessRepository.applyForVerification(userId)
                    .onSuccess {
                        _state.value = _state.value.copy(verificationStatus = VerificationStatus.PENDING)
                    }
                    .onFailure { e ->
                        _error.value = "Failed to apply for verification: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "Failed to apply for verification: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

data class BusinessPlatformState(
    val accountType: AccountType,
    val isBusinessAccount: Boolean,
    val analytics: AnalyticsData?,
    val monetizationEnabled: Boolean,
    val revenue: RevenueData?,
    val verificationStatus: VerificationStatus
)
