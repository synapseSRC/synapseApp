package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.domain.model.Draft
import com.synapse.social.studioasinc.shared.domain.model.DraftType
import com.synapse.social.studioasinc.shared.data.local.database.DraftDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DraftRepositoryTest {

    private class FakeDraftDao : DraftDao {
        private val drafts = mutableMapOf<String, Draft>()
        private val draftsFlow = MutableStateFlow<List<Draft>>(emptyList())

        override suspend fun insert(draft: Draft) {
            drafts[draft.id] = draft
            draftsFlow.value = drafts.values.toList()
        }

        override suspend fun deleteById(id: String) {
            drafts.remove(id)
            draftsFlow.value = drafts.values.toList()
        }

        override suspend fun deleteByType(type: DraftType) {
            drafts.values.removeAll { it.type == type }
            draftsFlow.value = drafts.values.toList()
        }

        override suspend fun getByType(type: DraftType): List<Draft> {
            return drafts.values.filter { it.type == type }
        }

        override suspend fun getAll(): List<Draft> {
            return drafts.values.toList()
        }

        override fun getByTypeAsFlow(type: DraftType): Flow<List<Draft>> {
            return draftsFlow
        }
    }

    @Test
    fun `test saving and retrieving draft`() = runTest {
        val dao = FakeDraftDao()
        val repository = DraftRepositoryImpl(dao)
        val draft = Draft("1", DraftType.POST, "content", 123L)

        repository.saveDraft(draft)
        val retrieved = repository.getDraftByType(DraftType.POST)

        assertEquals(draft, retrieved)
    }

    @Test
    fun `test deleting draft`() = runTest {
        val dao = FakeDraftDao()
        val repository = DraftRepositoryImpl(dao)
        val draft = Draft("1", DraftType.POST, "content", 123L)

        repository.saveDraft(draft)
        repository.deleteDraftByType(DraftType.POST)
        val retrieved = repository.getDraftByType(DraftType.POST)

        assertNull(retrieved)
    }
}
