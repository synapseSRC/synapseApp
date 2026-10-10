package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.synapse.social.studioasinc.feature.shared.theme.safeParseColorHex
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType

private const val MAX_BLUR_RADIUS = 50f

@Composable
internal fun ChatBackground(
    chatWallpaperType: WallpaperType,
    chatWallpaperValue: String?,
    chatWallpaperBlur: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    when (chatWallpaperType) {
        WallpaperType.SOLID_COLOR -> {
            if (!chatWallpaperValue.isNullOrBlank()) {
                val parsedColor = safeParseColorHex(chatWallpaperValue, androidx.compose.ui.graphics.Color.Transparent)
                if (parsedColor != androidx.compose.ui.graphics.Color.Transparent) {
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .background(parsedColor)
                    )
                }
            }
        }
        WallpaperType.DEFAULT -> {
            val resId = context.resources.getIdentifier("pattern_11", "raw", context.packageName)
            if (resId != 0) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resId)
                        .crossfade(false)
                        .decoderFactory(SvgDecoder.Factory())
                        .build(),
                    contentDescription = "Background",
                    modifier = modifier
                        .fillMaxSize()
                        .blur(radius = (chatWallpaperBlur * MAX_BLUR_RADIUS).dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.20f
                )
            }
        }
        WallpaperType.PATTERN, WallpaperType.PRESET_IMAGE -> {
            chatWallpaperValue?.let { rawValue ->
                val cleanName = rawValue.substringBeforeLast(".")
                val resId = context.resources.getIdentifier(cleanName, "raw", context.packageName)
                if (resId != 0) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resId)
                            .crossfade(false)
                            .apply {
                                if (chatWallpaperType == WallpaperType.PATTERN) {
                                    decoderFactory(SvgDecoder.Factory())
                                }
                            }
                            .build(),
                        contentDescription = "Background",
                        modifier = modifier
                            .fillMaxSize()
                            .blur(radius = (chatWallpaperBlur * MAX_BLUR_RADIUS).dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
