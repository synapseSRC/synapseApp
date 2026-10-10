package com.synapse.social.studioasinc.core.ui.util

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.synapse.social.studioasinc.feature.shared.theme.UiAtmosphereColors
import com.synapse.social.studioasinc.feature.shared.theme.UiAtmosphereState

object AmbientColorExtractor {
    fun extractColorsFromDrawable(drawable: Drawable, onColorsExtracted: (UiAtmosphereColors) -> Unit) {
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return
        extractColorsFromBitmap(bitmap, onColorsExtracted)
    }

    fun extractColorsFromBitmap(bitmap: Bitmap, onColorsExtracted: (UiAtmosphereColors) -> Unit) {
        Palette.from(bitmap).generate { palette ->
            palette?.let {
                val dominant = it.getDominantColor(0)
                val vibrant = it.getVibrantColor(it.getMutedColor(0))
                val muted = it.getMutedColor(0)

                onColorsExtracted(
                    UiAtmosphereColors(
                        dominantColor = if (dominant != 0) Color(dominant) else Color.Transparent,
                        vibrantColor = if (vibrant != 0) Color(vibrant) else Color.Transparent,
                        mutedColor = if (muted != 0) Color(muted) else Color.Transparent
                    )
                )
            }
        }
    }
}
