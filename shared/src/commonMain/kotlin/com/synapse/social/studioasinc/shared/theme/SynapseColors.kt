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
    val surfaceContainer: Long,
    val surfaceContainerLow: Long,
    val surfaceContainerHigh: Long,
    val surfaceContainerHighest: Long
)

object SynapseColors {
    val Blue = 0xFF1976D2
    val DarkBlue = 0xFF00497D
    val LightBlue = 0xFFBBDEFB

    val StatusOnline = 0xFF4CAF50
    val StatusRead = 0xFF4FC3F7
    val StatusOffline = 0xFF9E9E9E

    val ForestBubbleBackground = 0xFFE8F5E9
    val ForestBubbleText = 0xFF1B5E20
    val SunsetBubbleBackground = 0xFFFBE9E7
    val SunsetBubbleText = 0xFFBF360C
    val SunsetAccent = 0xFFFF5722

    val InteractionIconDefault = 0xFF657786
    val InteractionLikeActive = 0xFFE0245E
    val InteractionRepostActive = 0xFF17BF63

    val AccentOrange = 0xFFFFA500
    val AccentBlue = 0xFF2196F3
    val AccentYellow = 0xFFFFC107

    val Gray200 = 0xFFE0E0E0
    val Gray300 = 0xFFBDBDBD
    val Gray500 = 0xFF9E9E9E
    val Gray700 = 0xFF616161
    val Gray900 = 0xFF212121

    val ChatPresetColor1 = 0xFFE8F5E9
    val ChatPresetColor2 = 0xFFFBE9E7
    val ChatPresetColor3 = 0xFF1B5E20
    val ChatPresetColor4 = 0xFFBF360C
    val ChatPresetColor5 = 0xFFFF5722
    val ChatPresetColor6 = 0xFF9E9E9E
    val ChatPresetColor7 = 0xFFE0E0E0

    val StoryColorOrange = 0xFFFF9800
    val StoryColorGreen = 0xFF4CAF50
    val StoryColorPurple = 0xFF9C27B0

    val Light = SynapseColorScheme(
        primary = 0xFF6750A4,
        onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFEADDFF,
        onPrimaryContainer = 0xFF21005D,
        secondary = 0xFF625B71,
        onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFE8DEF8,
        onSecondaryContainer = 0xFF1D192B,
        tertiary = 0xFF7D5260,
        onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFFFD8E4,
        onTertiaryContainer = 0xFF31111D,
        error = 0xFFB3261E,
        onError = 0xFFFFFFFF,
        errorContainer = 0xFFF9DEDC,
        onErrorContainer = 0xFF410E0B,
        background = 0xFFFFFBFE,
        onBackground = 0xFF1C1B1F,
        surface = 0xFFFFFBFE,
        onSurface = 0xFF1C1B1F,
        surfaceVariant = 0xFFE7E0EC,
        onSurfaceVariant = 0xFF49454F,
        outline = 0xFF79747E,
        outlineVariant = 0xFFCAC4D0,
        scrim = 0xFF000000,
        inverseSurface = 0xFF313033,
        inverseOnSurface = 0xFFF4EFF4,
        inversePrimary = 0xFFD0BCFF,
        surfaceTint = 0xFF6750A4,
        surfaceContainer = 0xFFF3EDF7,
        surfaceContainerLow = 0xFFF7F2FA,
        surfaceContainerHigh = 0xFFECE6F0,
        surfaceContainerHighest = 0xFFE6E0E9
    )

    val Dark = SynapseColorScheme(
        primary = 0xFFD0BCFF,
        onPrimary = 0xFF381E72,
        primaryContainer = 0xFF4F378B,
        onPrimaryContainer = 0xFFEADDFF,
        secondary = 0xFFCCC2DC,
        onSecondary = 0xFF332D41,
        secondaryContainer = 0xFF4A4458,
        onSecondaryContainer = 0xFFE8DEF8,
        tertiary = 0xFFEFB8C8,
        onTertiary = 0xFF492532,
        tertiaryContainer = 0xFF633B48,
        onTertiaryContainer = 0xFFFFD8E4,
        error = 0xFFF2B8B5,
        onError = 0xFF601410,
        errorContainer = 0xFF8C1D18,
        onErrorContainer = 0xFFF9DEDC,
        background = 0xFF1C1B1F,
        onBackground = 0xFFE6E1E5,
        surface = 0xFF1C1B1F,
        onSurface = 0xFFE6E1E5,
        surfaceVariant = 0xFF2B2930,
        onSurfaceVariant = 0xFFCAC4D0,
        outline = 0xFF938F99,
        outlineVariant = 0xFF49454F,
        scrim = 0xFF000000,
        inverseSurface = 0xFFE6E1E5,
        inverseOnSurface = 0xFF313033,
        inversePrimary = 0xFF6750A4,
        surfaceTint = 0xFFD0BCFF,
        surfaceContainer = 0xFF211F26,
        surfaceContainerLow = 0xFF1D1B20,
        surfaceContainerHigh = 0xFF2B2930,
        surfaceContainerHighest = 0xFF36343B
    )
}
