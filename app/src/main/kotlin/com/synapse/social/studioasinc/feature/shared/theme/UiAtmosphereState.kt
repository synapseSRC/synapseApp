package com.synapse.social.studioasinc.feature.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@Immutable
data class UiAtmosphereColors(
    val dominantColor: Color = Color.Transparent,
    val vibrantColor: Color = Color.Transparent,
    val mutedColor: Color = Color.Transparent
)

@Stable
class UiAtmosphereState {
    var colors by mutableStateOf(UiAtmosphereColors())
        private set

    fun updateColors(newColors: UiAtmosphereColors) {
        colors = newColors
    }

    fun reset() {
        colors = UiAtmosphereColors()
    }
}

val LocalUiAtmosphere = compositionLocalOf { UiAtmosphereState() }
