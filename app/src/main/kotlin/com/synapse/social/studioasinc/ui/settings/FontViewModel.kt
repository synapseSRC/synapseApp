package com.synapse.social.studioasinc.ui.settings

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

data class FontUiState(
    val fonts: List<AppFontFamily> = emptyList(),
    val selectedFontId: String = "product_sans",
    val isImporting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FontViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FontUiState())
    val uiState: StateFlow<FontUiState> = _uiState.asStateFlow()

    init {
        loadFonts()
        viewModelScope.launch {
            settingsRepository.selectedFontId.collect { selectedFontId ->
                _uiState.update { it.copy(selectedFontId = selectedFontId) }
            }
        }
    }

    private fun loadFonts() {
        val builtInFont = AppFontFamily(
            id = "product_sans",
            displayName = "Product Sans",
            isBuiltIn = true,
            fontFamily = com.synapse.social.studioasinc.feature.shared.theme.ProductSans
        )

        val customFonts = mutableListOf<AppFontFamily>()
        val fontsDir = File(context.filesDir, "fonts")
        if (fontsDir.exists()) {
            fontsDir.listFiles()?.forEach { file ->
                if (file.extension.equals("ttf", ignoreCase = true) || file.extension.equals("otf", ignoreCase = true)) {
                    val fontFamily = try {
                        androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(file))
                    } catch (e: Exception) {
                        null
                    }
                    customFonts.add(
                        AppFontFamily(
                            id = "custom_${file.name}",
                            displayName = file.nameWithoutExtension,
                            isBuiltIn = false,
                            fontFamily = fontFamily
                        )
                    )
                }
            }
        }

        val allFonts = listOf(builtInFont) + customFonts
        _uiState.update { it.copy(fonts = allFonts) }
    }

    fun selectFont(id: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setSelectedFontId(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to select font") }
            }
        }
    }

    fun importFont(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, error = null) }
            try {
                val fontsDir = File(context.filesDir, "fonts")
                if (!fontsDir.exists()) {
                    fontsDir.mkdirs()
                }

                val fileName = getFileName(uri) ?: "imported_font_${System.currentTimeMillis()}.ttf"
                val destFile = File(fontsDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                loadFonts()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to import font: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isImporting = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { File(it).name }
        }
        return result
    }
}
