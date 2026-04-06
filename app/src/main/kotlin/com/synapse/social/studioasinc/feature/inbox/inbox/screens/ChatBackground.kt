package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
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
            // Do nothing, default surface color
        }
        WallpaperType.DEFAULT -> {
            val resId = context.resources.getIdentifier("pattern_11", "raw", context.packageName)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(resId)
                    .crossfade(false)
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = "Background",
                modifier = modifier.fillMaxSize().blur(radius = (chatWallpaperBlur * MAX_BLUR_RADIUS).dp),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )
        }
        WallpaperType.PATTERN, WallpaperType.PRESET_IMAGE -> {
            chatWallpaperValue?.let { value ->
                val resId = context.resources.getIdentifier(value, "raw", context.packageName)
                if (resId != 0) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resId)
                            .placeholder(resId)
                            .crossfade(false)
                            .apply {
                                if (chatWallpaperType == WallpaperType.PATTERN) {
                                    decoderFactory(SvgDecoder.Factory())
                                }
                            }
                            .build(),
                        contentDescription = "Background",
                        modifier = modifier.fillMaxSize().blur(radius = (chatWallpaperBlur * MAX_BLUR_RADIUS).dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
