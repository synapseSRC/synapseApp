package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing


@Composable
private fun EmptyFavouritesCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Text(
                text = stringResource(R.string.no_favourites_yet),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(Spacing.Small))
            
            Text(
                text = stringResource(R.string.add_contacts_favourites),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_favourites_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            item {
                Spacer(modifier = Modifier.height(Spacing.Small))
                EmptyFavouritesCard()
            }

            item {
                SettingsSection(title = stringResource(R.string.manage_favourites)) {
                    SettingsNavigationItem(
                        title = stringResource(R.string.add_to_favourites),
                        subtitle = stringResource(R.string.select_contacts_favourites),
                        imageVector = Icons.Filled.Add,
                        position = SettingsItemPosition.Top,
                        onClick = { }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = stringResource(R.string.reorder_favourites),
                        subtitle = stringResource(R.string.change_order_favourites),
                        imageVector = Icons.Filled.Reorder,
                        position = SettingsItemPosition.Middle,
                        onClick = { }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = stringResource(R.string.remove_from_favourites),
                        subtitle = stringResource(R.string.remove_contacts_favourites),
                        imageVector = Icons.Filled.Delete,
                        position = SettingsItemPosition.Bottom,
                        onClick = { }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.display_options)) {
                    SettingsToggleItem(
                        title = stringResource(R.string.show_favourites_chat_list),
                        subtitle = stringResource(R.string.display_favourite_contacts_top),
                        imageVector = Icons.Filled.Chat,
                        checked = true,
                        onCheckedChange = { },
                        position = SettingsItemPosition.Top
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = stringResource(R.string.show_favourite_badge),
                        subtitle = stringResource(R.string.display_star_icon),
                        imageVector = Icons.Filled.Favorite,
                        checked = true,
                        onCheckedChange = { },
                        position = SettingsItemPosition.Bottom
                    )
                }
            }
        }
    }
}
