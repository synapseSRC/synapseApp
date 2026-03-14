package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.domain.model.Draft
import com.synapse.social.studioasinc.shared.domain.model.DraftType
import kotlinx.coroutines.flow.Flow

interface DraftDao {
    suspend fun insert(draft: Draft)
    suspend fun deleteById(id: String)
    suspend fun deleteByType(type: DraftType)
    suspend fun getByType(type: DraftType): List<Draft>
    suspend fun getAll(): List<Draft>
    fun getByTypeAsFlow(type: DraftType): Flow<List<Draft>>
}
