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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                HeroCard()
            }

            item {
                Text(
                    text = stringResource(R.string.synapse_plus_section_features),
                    style = SettingsTypography.sectionHeader,
                    color = SettingsColors.sectionTitle,
                    modifier = Modifier.padding(bottom = 8.dp)
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
                title = { Text("Upgrade to Synapse Plus") },
                text = { Text("Payment integration is coming soon. Stay tuned for premium features!") },
                confirmButton = {
                    TextButton(onClick = { showUpgradeDialog = false }) {
                        Text("OK")
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Icon(
                    painter = painterResource(R.drawable.ic_verified),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(scale),
                    tint = Color.White
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.synapse_plus_hero_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.synapse_plus_hero_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        features.chunked(2).forEach { rowFeatures ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                .padding(16.dp),
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
                            modifier = Modifier.size(24.dp)
                        )
                        is Int -> Icon(
                            painter = painterResource(feature.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.synapse_plus_upgrade_button),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = stringResource(R.string.synapse_plus_price),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
