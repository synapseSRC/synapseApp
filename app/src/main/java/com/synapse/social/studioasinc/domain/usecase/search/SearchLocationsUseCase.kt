package com.synapse.social.studioasinc.domain.usecase.search

import com.synapse.social.studioasinc.data.repository.LocationRepository
import com.synapse.social.studioasinc.domain.model.LocationData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SearchLocationsUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    operator fun invoke(query: String): Flow<Result<List<LocationData>>> = flow {
        if (query.isBlank()) {
            emit(Result.success(emptyList()))
            return@flow
        }
        delay(500)

        val result = locationRepository.searchLocations(query)
        emit(result)
    }
}
