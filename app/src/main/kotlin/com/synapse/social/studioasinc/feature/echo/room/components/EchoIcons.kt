package com.synapse.social.studioasinc.feature.echo.room.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FrontHand
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.ui.graphics.vector.ImageVector

/** One glyph per room control, so a state always maps to the same icon. */
internal object EchoIcons {
    val micOn: ImageVector = Icons.Rounded.Mic
    val micOff: ImageVector = Icons.Rounded.MicOff
    val raisedHand: ImageVector = Icons.Rounded.FrontHand
}
