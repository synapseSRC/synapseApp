package com.synapse.social.studioasinc.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.core.util.ImageLoader
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.core.config.SynapseConfig

private object AboutSupportConstants {
    val URL_GITHUB_BUG_REPORT = SynapseConfig.GITHUB_BUG_REPORT_URL
    val URL_APP_WEBSITE = SynapseConfig.APP_WEBSITE_URL
    val URL_GITHUB_REPO = SynapseConfig.GITHUB_BUG_REPORT_URL.substringBefore("/issues")
    val URL_DONATE = "https://buymeacoffee.com"
    val URL_DISCORD = "https://discord.gg"
    val URL_TELEGRAM = "https://t.me"
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSupportScreen(
    onBackClick: () -> Unit,
    onNavigateToLicenses: () -> Unit = {},
    viewModel: AboutSupportViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val appVersion by viewModel.appVersion.collectAsState()
    val buildNumber by viewModel.buildNumber.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()


    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.space16),
            contentPadding = PaddingValues(vertical = Spacing.space16)
        ) {

            item {
                AppInfoHeaderCard(
                    appVersion = appVersion,
                    buildNumber = if (buildNumber.isNotBlank()) buildNumber else "UNIVERSAL"
                )
            }


            item {
                LeadDeveloperCard(
                    onWebsiteClick = { IntentUtils.openUrl(context, AboutSupportConstants.URL_APP_WEBSITE) },
                    onGithubClick = { IntentUtils.openUrl(context, AboutSupportConstants.URL_GITHUB_REPO) },
                    onSocialClick = { IntentUtils.openUrl(context, AboutSupportConstants.URL_APP_WEBSITE) },
                    onCoffeeClick = { IntentUtils.openUrl(context, AboutSupportConstants.URL_DONATE) }
                )
            }


            item {
                SettingsSection(title = "Collaborators") {
                    CollaboratorItem(
                        name = "Adriel O'Connel",
                        role = "Collaborator",
                        avatarRes = R.drawable.ic_launcher_foreground,
                        onCoffeeClick = { IntentUtils.openUrl(context, AboutSupportConstants.URL_DONATE) },
                        onGithubClick = { IntentUtils.openUrl(context, AboutSupportConstants.URL_GITHUB_REPO) },
                        position = SettingsItemPosition.Top
                    )
                    CollaboratorItem(
                        name = "Nyx",
                        role = "Collaborator",
                        avatarRes = R.drawable.ic_launcher_foreground,
                        onCoffeeClick = { IntentUtils.openUrl(context, AboutSupportConstants.URL_DONATE) },
                        onGithubClick = { IntentUtils.openUrl(context, AboutSupportConstants.URL_GITHUB_REPO) },
                        position = SettingsItemPosition.Bottom
                    )
                }
            }


            item {
                SettingsSection(title = "Community & Info") {
                    SettingsClickableItem(
                        title = "Discord server",
                        imageVector = Icons.Filled.Forum,
                        onClick = { IntentUtils.openUrl(context, AboutSupportConstants.URL_DISCORD) },
                        position = SettingsItemPosition.Top
                    )
                    SettingsClickableItem(
                        title = "Telegram channel",
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        onClick = { IntentUtils.openUrl(context, AboutSupportConstants.URL_TELEGRAM) },
                        position = SettingsItemPosition.Middle
                    )
                    SettingsClickableItem(
                        title = "View repository",
                        imageVector = Icons.Filled.Code,
                        onClick = { IntentUtils.openUrl(context, AboutSupportConstants.URL_GITHUB_REPO) },
                        position = SettingsItemPosition.Middle
                    )
                    SettingsNavigationItem(
                        title = "GNU General Public License v3.0",
                        subtitle = "Free, open-source software. You may use, study, share, and improve it",
                        imageVector = Icons.Filled.Info,
                        onClick = {
                            viewModel.navigateToLicenses()
                            onNavigateToLicenses()
                        },
                        position = SettingsItemPosition.Bottom
                    )
                }
            }


            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.space16),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "This project stands with Palestine 🇵🇸",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

}



@Composable
private fun AppInfoHeaderCard(
    appVersion: String,
    buildNumber: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.space20),
            verticalAlignment = Alignment.CenterVertically
        ) {

            SettingsIconBadge(
                imageVector = Icons.Filled.MusicNote,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.space16))


            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space6)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .padding(horizontal = Spacing.space10, vertical = Spacing.space4)
                    ) {
                        Text(
                            text = appVersion.ifBlank { "1.0.0" },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }


                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = Spacing.space10, vertical = Spacing.space4)
                    ) {
                        Text(
                            text = buildNumber.ifBlank { "UNIVERSAL" },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeadDeveloperCard(
    onWebsiteClick: () -> Unit,
    onGithubClick: () -> Unit,
    onSocialClick: () -> Unit,
    onCoffeeClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        color = SettingsColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.space20),
            verticalArrangement = Arrangement.spacedBy(Spacing.space16)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space16)
            ) {

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(SettingsShapes.scallopedShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageLoader.buildImageRequest(
                            context = context,
                            url = "https://i.ibb.co.com/QFBvXr8T/FB-IMG-1787669749645.jpg"
                        ),
                        contentDescription = "Lead Developer",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(SettingsShapes.scallopedShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(
                        text = "Ashik Ahmed",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Lead developer",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space12)
            ) {
                SocialButton(
                    imageVector = Icons.Filled.Language,
                    onClick = onWebsiteClick,
                    modifier = Modifier.weight(1f)
                )
                SocialButton(
                    imageVector = Icons.Filled.Code,
                    onClick = onGithubClick,
                    modifier = Modifier.weight(1f)
                )
                SocialButton(
                    imageVector = Icons.Filled.CameraAlt,
                    onClick = onSocialClick,
                    modifier = Modifier.weight(1f)
                )
            }


            Button(
                onClick = onCoffeeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space12)
            ) {
                Icon(
                    imageVector = Icons.Filled.Coffee,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.space8))
                Text(
                    text = "Buy me a coffee!",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SocialButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = SettingsColors.iconContainerBackground
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = SettingsColors.itemIcon
            )
        }
    }
}

@Composable
private fun CollaboratorItem(
    name: String,
    role: String,
    avatarRes: Int,
    onCoffeeClick: () -> Unit,
    onGithubClick: () -> Unit,
    position: SettingsItemPosition
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsSpacing.itemHorizontalPadding, vertical = Spacing.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = painterResource(avatarRes),
                contentDescription = name,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )

            Spacer(modifier = Modifier.width(Spacing.space12))


            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = SettingsTypography.itemTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = role,
                    style = SettingsTypography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                SmallIconButton(
                    imageVector = Icons.Filled.Coffee,
                    onClick = onCoffeeClick
                )
                SmallIconButton(
                    imageVector = Icons.Filled.Code,
                    onClick = onGithubClick
                )
            }
        }
    }
}

@Composable
private fun SmallIconButton(
    imageVector: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(SettingsColors.iconContainerBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = SettingsColors.itemIcon
        )
    }
}
