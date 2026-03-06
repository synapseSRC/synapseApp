package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynapsePlusScreen(
    onBackClick: () -> Unit
) {
    var showUpgradeDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.synapse_plus_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            contentPadding = PaddingValues(bottom = Spacing.Large)
        ) {
            item {
                HeroCard()
            }

            item {
                Text(
                    text = stringResource(R.string.synapse_plus_section_features),
                    style = SettingsTypography.sectionHeader,
                    color = SettingsColors.sectionTitle,
                    modifier = Modifier.padding(bottom = Spacing.Small)
                )
                FeatureGrid()
            }

            item {
                UpgradeButton(onClick = { showUpgradeDialog = true })
            }
        }

        if (showUpgradeDialog) {
            AlertDialog(
                onDismissRequest = { showUpgradeDialog = false },
                title = { Text(stringResource(R.string.synapse_plus_upgrade_dialog_title)) },
                text = { Text(stringResource(R.string.synapse_plus_upgrade_dialog_message)) },
                confirmButton = {
                    TextButton(onClick = { showUpgradeDialog = false }) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            )
        }
    }
}

@Composable
private fun HeroCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.ExtraSmall)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(Spacing.Large),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {

                Icon(
                    painter = painterResource(R.drawable.ic_verified),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(scale),
                    tint = onPrimaryColor
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
                ) {
                    Text(
                        text = stringResource(R.string.synapse_plus_hero_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = onPrimaryColor
                    )
                    Text(
                        text = stringResource(R.string.synapse_plus_hero_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = onPrimaryColor.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureGrid() {
    val features = listOf(
        FeatureItem(
            stringResource(R.string.synapse_plus_feature_verified),
            stringResource(R.string.synapse_plus_feature_verified_desc),
            R.drawable.ic_verified
        ),
        FeatureItem(
            stringResource(R.string.synapse_plus_feature_ad_free),
            stringResource(R.string.synapse_plus_feature_ad_free_desc),
            Icons.Filled.Lock
        ),
        FeatureItem(
            stringResource(R.string.synapse_plus_feature_analytics),
            stringResource(R.string.synapse_plus_feature_analytics_desc),
            Icons.Filled.Info
        ),
        FeatureItem(
            stringResource(R.string.synapse_plus_feature_themes),
            stringResource(R.string.synapse_plus_feature_themes_desc),
            Icons.Filled.Face
        ),
        FeatureItem(
            stringResource(R.string.synapse_plus_feature_support),
            stringResource(R.string.synapse_plus_feature_support_desc),
            Icons.Filled.Star
        ),
        FeatureItem(
            stringResource(R.string.synapse_plus_feature_extended),
            stringResource(R.string.synapse_plus_feature_extended_desc),
            Icons.Filled.Edit
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.SmallMedium)) {
        features.chunked(2).forEach { rowFeatures ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.SmallMedium)
            ) {
                rowFeatures.forEach { feature ->
                    FeatureCard(
                        feature = feature,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowFeatures.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class FeatureItem(
    val title: String,
    val subtitle: String,
    val icon: Any
)

@Composable
private fun FeatureCard(
    feature: FeatureItem,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.height(140.dp),
        shape = SettingsShapes.itemShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = SettingsColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {

            Surface(
                shape = SettingsShapes.chipShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (feature.icon) {
                        is ImageVector -> Icon(
                            imageVector = feature.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Spacing.Large)
                        )
                        is Int -> Icon(
                            painter = painterResource(feature.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Spacing.Large)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                Text(
                    text = feature.title,
                    style = SettingsTypography.itemTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UpgradeButton(
    onClick: () -> Unit
) {
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsShapes.itemShape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.synapse_plus_upgrade_button),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = onPrimaryColor
            )
            Text(
                text = stringResource(R.string.synapse_plus_price),
                style = MaterialTheme.typography.bodyMedium,
                color = onPrimaryColor.copy(alpha = 0.9f)
            )
        }
    }
}
