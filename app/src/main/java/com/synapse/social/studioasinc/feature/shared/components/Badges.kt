package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Verified",
        modifier = modifier
            .size(16.dp)
            .padding(start = Spacing.ExtraSmall),
        tint = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun GenderBadge(
    gender: String,
    modifier: Modifier = Modifier
) {
    val icon = when (gender.lowercase()) {
        "male" -> Icons.Filled.Male
        "female" -> Icons.Filled.Female
        else -> null
    }

    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = gender,
            modifier = modifier
                .size(16.dp)
                .padding(start = Spacing.ExtraSmall),
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VerifiedBadgePreview() {
    SynapseTheme {
        VerifiedBadge()
    }
}
