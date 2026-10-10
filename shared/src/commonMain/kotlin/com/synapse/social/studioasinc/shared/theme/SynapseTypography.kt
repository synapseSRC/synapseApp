package com.synapse.social.studioasinc.shared.theme

data class SynapseTextStyle(
    val fontSize: Float,
    val lineHeight: Float,
    val letterSpacing: Float,
    val fontWeight: Int
)

object SynapseFontWeights {
    const val Normal = 400
    const val Bold = 700
}

object SynapseTypography {
    val displayLarge = SynapseTextStyle(
        fontSize = 57f,
        lineHeight = 64f,
        letterSpacing = -0.25f,
        fontWeight = SynapseFontWeights.Normal
    )
    val displayMedium = SynapseTextStyle(
        fontSize = 45f,
        lineHeight = 52f,
        letterSpacing = 0f,
        fontWeight = SynapseFontWeights.Normal
    )
    val displaySmall = SynapseTextStyle(
        fontSize = 36f,
        lineHeight = 44f,
        letterSpacing = 0f,
        fontWeight = SynapseFontWeights.Normal
    )
    val headlineLarge = SynapseTextStyle(
        fontSize = 32f,
        lineHeight = 40f,
        letterSpacing = 0f,
        fontWeight = SynapseFontWeights.Normal
    )
    val headlineMedium = SynapseTextStyle(
        fontSize = 28f,
        lineHeight = 36f,
        letterSpacing = 0f,
        fontWeight = SynapseFontWeights.Normal
    )
    val headlineSmall = SynapseTextStyle(
        fontSize = 24f,
        lineHeight = 32f,
        letterSpacing = 0f,
        fontWeight = SynapseFontWeights.Normal
    )
    val titleLarge = SynapseTextStyle(
        fontSize = 22f,
        lineHeight = 28f,
        letterSpacing = 0f,
        fontWeight = SynapseFontWeights.Normal
    )
    val titleMedium = SynapseTextStyle(
        fontSize = 16f,
        lineHeight = 24f,
        letterSpacing = 0.15f,
        fontWeight = SynapseFontWeights.Bold
    )
    val titleSmall = SynapseTextStyle(
        fontSize = 14f,
        lineHeight = 20f,
        letterSpacing = 0.1f,
        fontWeight = SynapseFontWeights.Bold
    )
    val bodyLarge = SynapseTextStyle(
        fontSize = 16f,
        lineHeight = 24f,
        letterSpacing = 0.5f,
        fontWeight = SynapseFontWeights.Normal
    )
    val bodyMedium = SynapseTextStyle(
        fontSize = 14f,
        lineHeight = 20f,
        letterSpacing = 0.25f,
        fontWeight = SynapseFontWeights.Normal
    )
    val bodySmall = SynapseTextStyle(
        fontSize = 12f,
        lineHeight = 16f,
        letterSpacing = 0.4f,
        fontWeight = SynapseFontWeights.Normal
    )
    val labelLarge = SynapseTextStyle(
        fontSize = 14f,
        lineHeight = 20f,
        letterSpacing = 0.1f,
        fontWeight = SynapseFontWeights.Bold
    )
    val labelMedium = SynapseTextStyle(
        fontSize = 12f,
        lineHeight = 16f,
        letterSpacing = 0.5f,
        fontWeight = SynapseFontWeights.Bold
    )
    val labelSmall = SynapseTextStyle(
        fontSize = 11f,
        lineHeight = 16f,
        letterSpacing = 0.5f,
        fontWeight = SynapseFontWeights.Bold
    )
}
