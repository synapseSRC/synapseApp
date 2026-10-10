package com.synapse.social.studioasinc.shared.theme

data class SynapseColorScheme(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long,
    val success: Long,
    val onSuccess: Long,
    val successContainer: Long,
    val onSuccessContainer: Long,
    val warning: Long,
    val onWarning: Long,
    val warningContainer: Long,
    val onWarningContainer: Long,
    val info: Long,
    val onInfo: Long,
    val infoContainer: Long,
    val onInfoContainer: Long,
    val statusOnline: Long,
    val statusOffline: Long,
    val statusRead: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
    val outlineVariant: Long,
    val scrim: Long,
    val inverseSurface: Long,
    val inverseOnSurface: Long,
    val inversePrimary: Long,
    val surfaceTint: Long,
    val surfaceContainerLowest: Long,
    val surfaceContainerLow: Long,
    val surfaceContainer: Long,
    val surfaceContainerHigh: Long,
    val surfaceContainerHighest: Long
)

object SynapseColors {
    // Brand identity - Synapse Blue
    val Blue = 0xFF1976D2
    val DarkBlue = 0xFF00497D
    val LightBlue = 0xFFBBDEFB

    // Status colors
    val StatusOnline = 0xFF2E7D32
    val StatusRead = 0xFF0288D1
    val StatusOffline = 0xFF757575

    // Chat Preset Harmonized Color Tokens
    val OceanDarkContainer = 0xFF00497D
    val OceanLightContainer = 0xFFE0F2FE
    val OceanDarkOnContainer = 0xFFC2E8FF
    val OceanLightOnContainer = 0xFF001D33

    val ForestDarkContainer = 0xFF00391A
    val ForestLightContainer = 0xFFE8F5E9
    val ForestDarkOnContainer = 0xFF86F8AC
    val ForestLightOnContainer = 0xFF00210C

    val SunsetDarkContainer = 0xFF5C1D00
    val SunsetLightContainer = 0xFFFBE9E7
    val SunsetDarkOnContainer = 0xFFFFDBCF
    val SunsetLightOnContainer = 0xFF380B00

    val MonochromeDarkContainer = 0xFF424242
    val MonochromeLightContainer = 0xFFE0E0E0
    val MonochromeDarkOnContainer = 0xFFF5F5F5
    val MonochromeLightOnContainer = 0xFF212121

    // Interaction colors
    val InteractionIconDefault = 0xFF657786
    val InteractionLikeActive = 0xFFE0245E
    val InteractionRepostActive = 0xFF17BF63

    val Light = SynapseColorScheme(
        primary = 0xFF0061A4,
        onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFD1E4FF,
        onPrimaryContainer = 0xFF001D36,
        secondary = 0xFF535F70,
        onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFD7E3F8,
        onSecondaryContainer = 0xFF101C2B,
        tertiary = 0xFF6B5778,
        onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFF2DAFF,
        onTertiaryContainer = 0xFF251431,
        error = 0xFFBA1A1A,
        onError = 0xFFFFFFFF,
        errorContainer = 0xFFFFDAD6,
        onErrorContainer = 0xFF410002,
        success = 0xFF1B6D33,
        onSuccess = 0xFFFFFFFF,
        successContainer = 0xFFA7F5B1,
        onSuccessContainer = 0xFF00210B,
        warning = 0xFF7D5300,
        onWarning = 0xFFFFFFFF,
        warningContainer = 0xFFFFDEAE,
        onWarningContainer = 0xFF281800,
        info = 0xFF006399,
        onInfo = 0xFFFFFFFF,
        infoContainer = 0xFFCEE5FF,
        onInfoContainer = 0xFF001D32,
        statusOnline = 0xFF2E7D32,
        statusOffline = 0xFF757575,
        statusRead = 0xFF0288D1,
        background = 0xFFF8F9FF,
        onBackground = 0xFF191C20,
        surface = 0xFFF8F9FF,
        onSurface = 0xFF191C20,
        surfaceVariant = 0xFFDFE2EC,
        onSurfaceVariant = 0xFF43474E,
        outline = 0xFF73777F,
        outlineVariant = 0xFFC3C6CF,
        scrim = 0xFF000000,
        inverseSurface = 0xFF2E3036,
        inverseOnSurface = 0xFFF0F0F7,
        inversePrimary = 0xFF9ECAFF,
        surfaceTint = 0xFF0061A4,
        surfaceContainerLowest = 0xFFFFFFFF,
        surfaceContainerLow = 0xFFF2F3FA,
        surfaceContainer = 0xFFECEEF6,
        surfaceContainerHigh = 0xFFE6E8F0,
        surfaceContainerHighest = 0xFFE0E2EC
    )

    val Dark = SynapseColorScheme(
        primary = 0xFF9ECAFF,
        onPrimary = 0xFF003258,
        primaryContainer = 0xFF00497D,
        onPrimaryContainer = 0xFFD1E4FF,
        secondary = 0xFFBBC7DB,
        onSecondary = 0xFF253140,
        secondaryContainer = 0xFF3B4858,
        onSecondaryContainer = 0xFFD7E3F8,
        tertiary = 0xFFD6BEE4,
        onTertiary = 0xFF3B2947,
        tertiaryContainer = 0xFF533F5F,
        onTertiaryContainer = 0xFFF2DAFF,
        error = 0xFFFFB4AB,
        onError = 0xFF690005,
        errorContainer = 0xFF93000A,
        onErrorContainer = 0xFFFFDAD6,
        success = 0xFF8CDB96,
        onSuccess = 0xFF003914,
        successContainer = 0xFF005321,
        onSuccessContainer = 0xFFA7F5B1,
        warning = 0xFFFBB849,
        onWarning = 0xFF422B00,
        warningContainer = 0xFF5F3E00,
        onWarningContainer = 0xFFFFDEAE,
        info = 0xFF96CCFF,
        onInfo = 0xFF003353,
        infoContainer = 0xFF004B75,
        onInfoContainer = 0xFFCEE5FF,
        statusOnline = 0xFF81C784,
        statusOffline = 0xFFBDBDBD,
        statusRead = 0xFF4FC3F7,
        background = 0xFF111318,
        onBackground = 0xFFE1E2E8,
        surface = 0xFF111318,
        onSurface = 0xFFE1E2E8,
        surfaceVariant = 0xFF43474E,
        onSurfaceVariant = 0xFFC3C6CF,
        outline = 0xFF8D9199,
        outlineVariant = 0xFF43474E,
        scrim = 0xFF000000,
        inverseSurface = 0xFFE1E2E8,
        inverseOnSurface = 0xFF2E3036,
        inversePrimary = 0xFF0061A4,
        surfaceTint = 0xFF9ECAFF,
        surfaceContainerLowest = 0xFF0C0E13,
        surfaceContainerLow = 0xFF191C20,
        surfaceContainer = 0xFF1D2024,
        surfaceContainerHigh = 0xFF282A2F,
        surfaceContainerHighest = 0xFF33353A
    )
}
