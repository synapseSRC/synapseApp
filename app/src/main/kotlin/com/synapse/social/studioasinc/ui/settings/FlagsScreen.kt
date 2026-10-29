package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography
import com.synapse.social.studioasinc.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlagsScreen(
    viewModel: FlagsViewModel,
    onBackClick: () -> Unit
) {
    val navigationPlacement by viewModel.navigationPlacement.collectAsState()
    val messageSuggestionEnabled by viewModel.messageSuggestionEnabled.collectAsState()
    val chatAvatarDisabled by viewModel.chatAvatarDisabled.collectAsState()
    val chatMessagePaginationLimit by viewModel.chatMessagePaginationLimit.collectAsState()
    var paginationLimitInput by remember(chatMessagePaginationLimit) {
        mutableStateOf(chatMessagePaginationLimit.toString())
    }
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = AppColors.screenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_flags_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.screenBackground,
                    scrolledContainerColor = AppColors.cardBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing),
            contentPadding = PaddingValues(vertical = Spacing.space8)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SectionHeader(title = stringResource(R.string.settings_experimental_features_title))

                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.itemSpacing)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = SettingsItemPosition.Top.getShape(),
                            color = AppColors.cardBackground
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = AppSpacing.itemHorizontalPadding,
                                        vertical = AppSpacing.itemVerticalPadding
                                    )
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_navigation_placement_title),
                                    style = AppTypography.itemTitle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(Spacing.space4))
                                Text(
                                    text = stringResource(R.string.settings_navigation_placement_subtitle),
                                    style = AppTypography.itemSubtitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(Spacing.space16))

                                val options = NavigationPlacement.entries
                                val topPlacementLabel = stringResource(R.string.settings_navigation_placement_top)
                                val bottomPlacementLabel = stringResource(R.string.settings_navigation_placement_bottom)
                                ButtonGroup(
                                    overflowIndicator = { menuState ->
                                        ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    expandedRatio = 0f,
                                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    options.forEach { option ->
                                        val label = when (option) {
                                            NavigationPlacement.TOP -> topPlacementLabel
                                            NavigationPlacement.BOTTOM -> bottomPlacementLabel
                                        }
                                        toggleableItem(
                                            checked = option == navigationPlacement,
                                            label = label,
                                            onCheckedChange = { checked ->
                                                if (checked) viewModel.setNavigationPlacement(option)
                                            },
                                            weight = 1f
                                        )
                                    }
                                }
                            }
                        }
                        SettingsToggleItem(
                            title = stringResource(R.string.settings_message_suggestions_title),
                            subtitle = stringResource(R.string.settings_message_suggestions_subtitle),
                            imageVector = Icons.Filled.Build,
                            checked = messageSuggestionEnabled,
                            onCheckedChange = { viewModel.setMessageSuggestionEnabled(it) },
                            position = SettingsItemPosition.Middle
                        )
                        SettingsToggleItem(
                            title = stringResource(R.string.settings_disable_chat_avatars_title),
                            subtitle = stringResource(R.string.settings_disable_chat_avatars_subtitle),
                            imageVector = Icons.Filled.Person,
                            checked = chatAvatarDisabled,
                            onCheckedChange = { viewModel.setChatAvatarDisabled(it) },
                            position = SettingsItemPosition.Bottom
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = stringResource(R.string.settings_performance_section))

                    SettingsCard {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = SettingsItemPosition.Single.getShape(),
                            color = AppColors.cardBackground
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = AppSpacing.itemHorizontalPadding,
                                        vertical = AppSpacing.itemVerticalPadding
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_message_pagination_limit_title),
                                        style = AppTypography.itemTitle,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_message_pagination_limit_subtitle),
                                        style = AppTypography.itemSubtitle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = paginationLimitInput,
                                    onValueChange = { paginationLimitInput = it.filter { c -> c.isDigit() } },
                                    modifier = Modifier.width(Sizes.WidthLarge),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        val value = paginationLimitInput.toIntOrNull()
                                        if (value != null && value > 0) {
                                            viewModel.setChatMessagePaginationLimit(value)
                                        } else {
                                            paginationLimitInput = chatMessagePaginationLimit.toString()
                                        }
                                        focusManager.clearFocus()
                                    }),
                                    textStyle = AppTypography.itemTitle.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
