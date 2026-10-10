package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

data class PrivacyOptionItem<T>(
    val value: T,
    val title: String,
    val subtitle: String? = null,
    val isExceptionOption: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PrivacySelectionScreen(
    title: String,
    subtitle: String? = null,
    options: List<PrivacyOptionItem<T>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onNavigateToExceptionSelector: (() -> Unit)? = null,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            if (subtitle != null) {
                item {
                    Text(
                        text = subtitle,
                        style = SettingsTypography.itemSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = SettingsSpacing.itemHorizontalPadding,
                            vertical = Spacing.Small
                        )
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
                ) {
                    options.forEachIndexed { index, option ->
                        val position = when {
                            options.size == 1 -> SettingsItemPosition.Single
                            index == 0 -> SettingsItemPosition.Top
                            index == options.lastIndex -> SettingsItemPosition.Bottom
                            else -> SettingsItemPosition.Middle
                        }

                        val isSelected = option.value == selectedOption
                        val rowDescription = "${option.title}${option.subtitle?.let { ", $it" } ?: ""}"

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = position.getShape(),
                            color = SettingsColors.cardBackground
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled = !isLoading,
                                        role = Role.RadioButton,
                                        onClick = {
                                            onOptionSelected(option.value)
                                            if (option.isExceptionOption && onNavigateToExceptionSelector != null) {
                                                onNavigateToExceptionSelector()
                                            }
                                        }
                                    )
                                    .semantics(mergeDescendants = true) {
                                        role = Role.RadioButton
                                        contentDescription = rowDescription
                                    }
                                    .padding(
                                        horizontal = SettingsSpacing.itemHorizontalPadding,
                                        vertical = SettingsSpacing.itemVerticalPadding
                                    )
                                    .heightIn(min = SettingsSpacing.minTouchTarget),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = option.title,
                                        style = SettingsTypography.itemTitle,
                                        color = if (!isLoading) MaterialTheme.colorScheme.onSurface
                                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                    if (option.subtitle != null) {
                                        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                                        Text(
                                            text = option.subtitle,
                                            style = SettingsTypography.itemSubtitle,
                                            color = if (!isLoading) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(Spacing.Medium))

                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    enabled = !isLoading,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun <T> getStandardPrivacyOptions(
    excludedCount: Int = 0,
    createEnum: (String) -> T
): List<PrivacyOptionItem<T>> {
    val exceptionTitle = if (excludedCount > 0) {
        if (excludedCount == 1) {
            stringResource(R.string.privacy_option_my_contacts_except_count_one, excludedCount)
        } else {
            stringResource(R.string.privacy_option_my_contacts_except_count_other, excludedCount)
        }
    } else {
        stringResource(R.string.privacy_option_my_contacts_except)
    }

    return listOf(
        PrivacyOptionItem(
            value = createEnum("EVERYONE"),
            title = stringResource(R.string.privacy_option_everyone)
        ),
        PrivacyOptionItem(
            value = createEnum("MY_CONTACTS"),
            title = stringResource(R.string.privacy_option_my_contacts)
        ),
        PrivacyOptionItem(
            value = createEnum("MY_CONTACTS_EXCEPT"),
            title = exceptionTitle,
            isExceptionOption = true
        ),
        PrivacyOptionItem(
            value = createEnum("NOBODY"),
            title = stringResource(R.string.privacy_option_nobody)
        )
    )
}
