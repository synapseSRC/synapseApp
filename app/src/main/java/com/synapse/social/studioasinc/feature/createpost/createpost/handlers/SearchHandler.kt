package com.synapse.social.studioasinc.feature.createpost.createpost.handlers

import com.synapse.social.studioasinc.data.repository.LocationRepositoryImpl
import com.synapse.social.studioasinc.data.repository.UserRepositoryImpl
import com.synapse.social.studioasinc.domain.model.FeelingActivity
import com.synapse.social.studioasinc.domain.model.LocationData
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class SearchHandler @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val locationRepository: LocationRepositoryImpl
) {
    private val activeJobs = mutableListOf<Job>()

    fun searchUsers(
        scope: CoroutineScope,
        query: String,
        onLoading: () -> Unit,
        onResult: (Result<List<User>>) -> Unit
    ) {
        val job = scope.launch {
            onLoading()
            val result = userRepository.searchUsers(query)
            val userList = result.getOrNull()?.map { userProfile ->
                User(
                    uid = userProfile.uid,
                    username = userProfile.username,
                    displayName = userProfile.displayName,
                    avatar = userProfile.avatar,
                    email = userProfile.email,
                    bio = userProfile.bio,
                    verify = userProfile.verify
                )
            }?.let { Result.success(it) } ?: result.mapCatching { emptyList() }
            onResult(userList)
        }
        activeJobs.add(job)
    }

    fun searchLocations(
        scope: CoroutineScope,
        query: String,
        onLoading: () -> Unit,
        onResult: (Result<List<LocationData>>) -> Unit
    ) {
        val job = scope.launch {
            onLoading()
            val result = locationRepository.searchLocations(query)
            onResult(result)
        }
        activeJobs.add(job)
    }

    fun searchFeelings(query: String, allFeelings: List<FeelingActivity>): List<FeelingActivity> {
        if (query.isBlank()) return allFeelings
        return allFeelings.filter { feeling ->
            feeling.text.contains(query, ignoreCase = true) ||
            feeling.emoji.contains(query, ignoreCase = true)
        }
    }

    fun cancelAll() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
    }
}
