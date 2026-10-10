package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowingUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getFollowingUseCase: GetFollowingUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val authService: SupabaseAuthenticationService
) : ViewModel() {
    private val _contacts = MutableStateFlow<List<User>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            val currentUserId = authService.getCurrentUserId() ?: return@launch
            // Load following users as contacts
            val result = getFollowingUseCase(userId = currentUserId)
            result.onSuccess { users ->
                _contacts.value = users.sortedBy { it.displayName ?: it.username ?: "" }
            }
        }
    }

    fun searchContacts(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                loadContacts()
            } else {
                val result = searchUsersUseCase(query = query)
                result.onSuccess { users ->
                    _contacts.value = users.sortedBy { it.displayName ?: it.username ?: "" }
                }
            }
        }
    }
}
