package com.synapse.social.studioasinc.feature.shared.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Brand-only colour roles for the onboarding surface.
 *
 * The welcome screen is the one surface that has to read as Synapse regardless of the palette the
 * device supplies, so its teal accent and hero artwork are declared as brand tokens instead of
 * being read from [MaterialTheme.colorScheme]. Nothing here redefines or shadows an existing
 * scheme role, so Material You / dynamic color keeps working everywhere else in the app, and the
 * light/dark pair keeps the screen legible when the user runs a dark theme.
 */
@Immutable
data class SynapseBrandPalette(
    /** Deep Synapse teal used by the primary call to action and by inline links. */
    val accent: Color,
    /** Content colour on top of [accent]; contrast ratio is >= 4.5:1 in both brand palettes. */
    val onAccent: Color,
    val pageTop: Color,
    val pageMid: Color,
    val pageBottom: Color,
    /** Soft light bloom sitting behind the logo plate. */
    val bloom: Color,
    val ribbonDeep: Color,
    val ribbonMid: Color,
    val ribbonLight: Color,
    val ribbonSheen: Color,
    val ribbonEdge: Color,
    /** Cool violet halo that separates the plate from the ribbons. */
    val halo: Color,
    val plate: Color,
    val plateBorder: Color,
    val plateShadow: Color,
)

// Sampled from the redesigned welcome reference: cool-white page, deep teal CTA (#006E8B),
// cyan-to-white ribbon gradient and a violet halo around the mark.
internal val SynapseLightBrandPalette = SynapseBrandPalette(
    accent = Color(0xFF006E8B),
    onAccent = Color(0xFFFFFFFF),
    pageTop = Color(0xFFF2F5FE),
    pageMid = Color(0xFFE7EDFA),
    pageBottom = Color(0xFFF7F9FE),
    bloom = Color(0xFFFFFFFF),
    ribbonDeep = Color(0xFF0E6E8B),
    ribbonMid = Color(0xFF2E9FBE),
    ribbonLight = Color(0xFF9FE0F1),
    ribbonSheen = Color(0xFFF0FBFE),
    ribbonEdge = Color(0xFF0A4A5F),
    halo = Color(0xFFB6ADF5),
    plate = Color(0xFFFFFFFF),
    plateBorder = Color(0xFFFFFFFF),
    plateShadow = Color(0xFF5A6A93),
)

// Dark counterpart of the same artwork: identical hue family, luminance lowered so the screen does
// not flash a white page for users on a dark theme, and the CTA keeps its teal identity.
internal val SynapseDarkBrandPalette = SynapseBrandPalette(
    accent = Color(0xFF0A7A97),
    onAccent = Color(0xFFFFFFFF),
    pageTop = Color(0xFF0F151C),
    pageMid = Color(0xFF111C26),
    pageBottom = Color(0xFF0B1015),
    bloom = Color(0xFF12384C),
    ribbonDeep = Color(0xFF0B5E77),
    ribbonMid = Color(0xFF1E8CA8),
    ribbonLight = Color(0xFF3FB4D2),
    ribbonSheen = Color(0xFF7FD4E8),
    ribbonEdge = Color(0xFF072E3C),
    halo = Color(0xFF3A3472),
    plate = Color(0xFF16202A),
    plateBorder = Color(0xFF3A4A5C),
    plateShadow = Color(0xFF000000),
)

internal val LocalSynapseBrandPalette = staticCompositionLocalOf { SynapseLightBrandPalette }

object SynapseBrand {
    /** Brand palette for the active theme; provided by [SynapseTheme]. */
    val palette: SynapseBrandPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalSynapseBrandPalette.current
}

/**
 * Geometry for the welcome hero.
 *
 * My design decision (not in M3): the hero proportion, plate size/corner and mark size are brand
 * artwork metrics rather than Material 3 tokens, so they live with the rest of the brand tokens and
 * are expressed as fractions wherever the value has to follow the available space.
 */
object WelcomeHeroMetrics {
    /** Copy and action column cap so tablets and foldables keep a phone-like line length. */
    val ContentMaxWidth = 520.dp

    /** Below this width the heading steps down a type scale to stay on one line. */
    val CompactWidth = 360.dp

    /** Share of the available height owned by the hero artwork. */
    const val HeroHeightFraction = 0.48f
    val HeroMinHeight = 240.dp
    val HeroMaxHeight = 400.dp

    /** Vertical centre of the logo plate inside the hero, as a share of the hero height. */
    const val LogoCentreFraction = 0.56f

    val PlateSize = 168.dp
    val PlateCorner = 52.dp
    val PlateElevation = 18.dp

    /**
     * ic_launcher_foreground keeps its mark inside the adaptive-icon safe zone: the visible glyph
     * only fills ~45% x ~53% of the drawable bounds. Rendering the drawable at the plate size would
     * therefore leave the mark looking undersized, so it is drawn larger than the plate and the
     * plate clips it. The mark itself still lands well inside the plate.
     */
    val MarkSize = 236.dp
}

/**
 * Geometry for the compact sign-in hero.
 *
 * Deliberately much shorter than the welcome hero: the form is the primary focus of this screen, so
 * the artwork is a branded band that keeps the heading and the first field above the fold on a
 * typical phone, while still reading as the same illustration as the welcome screen.
 */
object SignInHeroMetrics {
    /** Height of the artwork band above the heading. */
    val Height = 176.dp

    /** Vertical centre of the logo plate inside the band, as a share of the band height. */
    const val LogoCentreFraction = 0.5f

    val PlateSize = 96.dp
    val PlateCorner = 30.dp
    val PlateElevation = 12.dp
    val MarkSize = 134.dp
}

