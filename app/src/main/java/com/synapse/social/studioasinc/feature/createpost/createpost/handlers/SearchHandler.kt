package com.synapse.social.studioasinc.feature.createpost.createpost.handlers

import com.synapse.social.studioasinc.data.repository.LocationRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.domain.model.FeelingActivity
import com.synapse.social.studioasinc.domain.model.LocationData
import com.synapse.social.studioasinc.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class SearchHandler @Inject constructor(
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository
) {
    private var userSearchJob: Job? = null
    private var locationSearchJob: Job? = null

    fun searchUsers(
        scope: CoroutineScope,
        query: String,
        onLoading: () -> Unit,
        onResult: (Result<List<User>>) -> Unit
    ) {
        userSearchJob?.cancel()
        userSearchJob = scope.launch {
            if (query.isBlank()) {
                onResult(Result.success(emptyList()))
                return@launch
            }
            delay(300)
            onLoading()

            val result = userRepository.searchUsers(query).map { profiles ->
                profiles.map { profile ->
                    User(
                        uid = profile.uid,
                        username = profile.username,
                        displayName = profile.displayName,
                        avatar = profile.avatar,
                        verify = profile.verify
                    )
                }
            }
            onResult(result)
        }
    }

    fun searchLocations(
        scope: CoroutineScope,
        query: String,
        onLoading: () -> Unit,
        onResult: (Result<List<LocationData>>) -> Unit
    ) {
        locationSearchJob?.cancel()
        locationSearchJob = scope.launch {
            if (query.isBlank()) {
                onResult(Result.success(emptyList()))
                return@launch
            }
            delay(500)
            onLoading()

            val result = locationRepository.searchLocations(query)
            onResult(result)
        }
    }

    fun searchFeelings(query: String, allFeelings: List<FeelingActivity>): List<FeelingActivity> {
        return if (query.isBlank()) {
            allFeelings
        } else {
            allFeelings.filter { it.text.contains(query, ignoreCase = true) }
        }
    }

    fun cancelAll() {
        userSearchJob?.cancel()
        locationSearchJob?.cancel()
    }
}
