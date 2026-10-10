package com.synapse.social.studioasinc.feature.shared.components.picker

import android.net.Uri
import com.synapse.social.studioasinc.core.media.processing.ThumbnailGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class FilePickerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `PickedFile thumbnailUri defaults to null`() {
        val uri = mock(Uri::class.java)
        val file = PickedFile(
            uri = uri,
            mimeType = "video/mp4",
            fileName = "video.mp4",
            size = 2000L
        )
        assertNull(file.thumbnailUri)
    }

    @Test
    fun `PickedFile preserves explicit thumbnailUri`() {
        val uri = mock(Uri::class.java)
        val thumbUri = mock(Uri::class.java)
        val file = PickedFile(
            uri = uri,
            mimeType = "video/mp4",
            fileName = "video.mp4",
            size = 2000L,
            thumbnailUri = thumbUri
        )
        assertEquals(thumbUri, file.thumbnailUri)
    }

    @Test
    fun `toggleSelection adds and removes URIs maintaining insertion order`() {
        val context = mock(android.content.Context::class.java)
        val viewModel = FilePickerViewModel(context)
        val uri1 = mock(Uri::class.java)
        val uri2 = mock(Uri::class.java)
        val uri3 = mock(Uri::class.java)

        viewModel.toggleSelection(uri1, maxSelection = 5)
        viewModel.toggleSelection(uri2, maxSelection = 5)
        viewModel.toggleSelection(uri3, maxSelection = 5)

        val selectedList = viewModel.uiState.value.selectedUris.toList()
        assertEquals(3, selectedList.size)
        assertEquals(uri1, selectedList[0])
        assertEquals(uri2, selectedList[1])
        assertEquals(uri3, selectedList[2])

        // Toggle uri2 off
        viewModel.toggleSelection(uri2, maxSelection = 5)
        val updatedList = viewModel.uiState.value.selectedUris.toList()
        assertEquals(2, updatedList.size)
        assertEquals(uri1, updatedList[0])
        assertEquals(uri3, updatedList[1])
    }

    @Test
    fun `toggleSelection respects maxSelection limit`() {
        val context = mock(android.content.Context::class.java)
        val viewModel = FilePickerViewModel(context)
        val uri1 = mock(Uri::class.java)
        val uri2 = mock(Uri::class.java)
        val uri3 = mock(Uri::class.java)

        viewModel.toggleSelection(uri1, maxSelection = 2)
        viewModel.toggleSelection(uri2, maxSelection = 2)
        viewModel.toggleSelection(uri3, maxSelection = 2) // Should be ignored as max is 2

        val selectedList = viewModel.uiState.value.selectedUris.toList()
        assertEquals(2, selectedList.size)
        assertEquals(uri1, selectedList[0])
        assertEquals(uri2, selectedList[1])
    }

    @Test
    fun `clearSelection empties selectedUris`() {
        val context = mock(android.content.Context::class.java)
        val viewModel = FilePickerViewModel(context)
        val uri1 = mock(Uri::class.java)

        viewModel.toggleSelection(uri1, maxSelection = 5)
        assertEquals(1, viewModel.uiState.value.selectedUris.size)

        viewModel.clearSelection()
        assertEquals(0, viewModel.uiState.value.selectedUris.size)
    }
}
