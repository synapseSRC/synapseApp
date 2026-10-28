package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

/**
 * Call geometry. Project design decision (not in M3): in-call controls are sized up from the
 * standard icon button because they are the only interaction available during a call and are used
 * one-handed while the display is covered by media.
 */
object CallDimens {
    val Avatar = Sizes.AvatarLargeProfile
    val AvatarCompact = Sizes.AvatarProfile
    val Portrait = 224.dp
    val PortraitCompact = 156.dp
    val AvatarRingGap = 5.dp
    val Control = Sizes.ButtonHeightM3Medium
    val ControlLabelWidth = 72.dp

    /**
     * My design decision (not in M3): the label column grows from [ControlLabelWidth] up to this
     * ceiling so long labels stay on two lines instead of clipping at large font scales; the row
     * wraps to fewer controls per line as the columns widen.
     */
    val ControlLabelMaxWidth = 152.dp
    val AnswerBarHeight = 72.dp
    val AnswerThumb = 64.dp
    val AnswerTrackInset = 4.dp
    val AnswerBarIcon = Sizes.IconExtraLarge
    val EndPillHeight = 64.dp
    val EndPillMinWidth = 200.dp
    val PipWidth = 108.dp
    val PipHeight = 144.dp
    val TopScrimHeight = 168.dp
    val BottomScrimHeight = 240.dp
    val ChipMinHeight = 32.dp
    val DebugChipMinHeight = 36.dp
    val HaloGlow = 72.dp
    val ControlIcon = Sizes.IconLarge
    val ChipIcon = Sizes.IconSemiMedium
    val ChipSpinner = Sizes.IconSemiSmall
    val DebugChipIcon = Sizes.IconMedium
    val SpinnerStroke = Sizes.BorderDefault
    val EndedIconCircle = Sizes.AvatarSemiLarge
    val EndedIcon = Sizes.IconExtraLarge
    val EndedActionMaxWidth = 320.dp
    val CompactThresholdHeight = 640.dp
    val BorderRing = Sizes.BorderThinAlt
    val PipBorder = Sizes.BorderThinAlt
    val PipElevation = Sizes.ElevationMedium

    const val TopScrimAlpha = 0.62f
    const val BottomScrimAlpha = 0.72f
    const val OutgoingMediaDim = 0.35f
    const val IncomingMediaDim = 0.45f
    const val ConnectingMediaDim = 0.55f
    const val ControlSurfaceAlpha = 0.55f
    const val PillSurfaceAlpha = 0.72f
    const val PipBorderAlpha = 0.60f
    const val HaloMaxScale = 1.10f
    const val HaloAmbientAlpha = 0.20f
    const val AtmosphereWashAlpha = 0.14f
    const val AtmosphereEdgeAlpha = 0.10f

    /** Fraction of the screen an atmosphere wash travels across one cycle. */
    const val AtmospherePrimaryDriftX = 0.16f
    const val AtmospherePrimaryDriftY = 0.09f
    const val AtmosphereEdgeDriftX = 0.09f
    const val AtmosphereEdgeDriftY = 0.07f

    /** Breathing of a wash's radius and opacity around its resting value. */
    const val AtmosphereRadiusPulse = 0.10f
    const val AtmosphereAlphaPulse = 0.30f

    /** How far a wash's colour leans toward its neighbour at the peak of the cycle. */
    const val AtmosphereTintBlend = 0.30f
    const val HaloCycleMillis = 1_600
    const val AtmosphereCycleMillis = 12_000
    const val AnswerCommitFraction = 0.55f
    const val LargeFontScale = 1.5f
    const val AutoDismissMillis = 2_400L
}
