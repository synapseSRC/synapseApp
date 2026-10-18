package com.synapse.social.studioasinc.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutAppViewModelsTest {
    @Test
    fun latestReleaseExpansionTogglesInViewModelState() {
        val viewModel = WhatsNewViewModel()

        assertFalse(viewModel.uiState.value.expandedLatest)
        viewModel.toggleLatestRelease()
        assertTrue(viewModel.uiState.value.expandedLatest)
        viewModel.toggleLatestRelease()
        assertFalse(viewModel.uiState.value.expandedLatest)
    }

    @Test
    fun licenseSearchMatchesLibraryAndAuthor() {
        val viewModel = AboutOpenSourceViewModel()
        val entries = licenseEntries()

        viewModel.onQueryChanged("coil")
        assertEquals(listOf("coil-kt / coil-compose"), viewModel.visibleEntries(entries).map { it.name })

        viewModel.onQueryChanged("JetBrains")
        assertEquals(listOf("kotlinx.coroutines"), viewModel.visibleEntries(entries).map { it.name })
    }

    @Test
    fun licenseSortByAuthorIsOptionalAndStable() {
        val viewModel = AboutOpenSourceViewModel()
        val entries = licenseEntries()

        assertEquals(listOf("kotlinx.coroutines", "coil-kt / coil-compose", "androidx.compose.material3"), viewModel.visibleEntries(entries).map { it.name })
        viewModel.setSortByAuthor(true)
        assertEquals(listOf("coil-kt / coil-compose", "kotlinx.coroutines", "androidx.compose.material3"), viewModel.visibleEntries(entries).map { it.name })
    }

    private fun licenseEntries() = listOf(
        AboutLicenseEntry("kotlinx.coroutines", "Apache License 2.0 • JetBrains", "JetBrains", "https://example.test/coroutines"),
        AboutLicenseEntry("coil-kt / coil-compose", "Apache License 2.0 • Coil Contributors", "Coil Contributors", "https://example.test/coil"),
        AboutLicenseEntry("androidx.compose.material3", "Apache License 2.0 • The Android Open Source Project", "The Android Open Source Project", "https://example.test/androidx")
    )
}
