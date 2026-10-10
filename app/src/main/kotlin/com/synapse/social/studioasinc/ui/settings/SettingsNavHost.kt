package com.synapse.social.studioasinc.ui.settings
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.feature.blocking.BlockingViewModel
import com.synapse.social.studioasinc.feature.blocking.ui.BlockedContactsScreen



@Composable
fun SettingsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = SettingsDestination.ROUTE_HUB,
    onBackClick: () -> Unit = {},
    onNavigateToProfileEdit: () -> Unit = {},
    onNavigateToChatPrivacy: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsRepository = SettingsRepositoryImpl.getInstance(context)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { SettingsAnimations.enterTransition },
        exitTransition = { SettingsAnimations.exitTransition },
        popEnterTransition = { SettingsAnimations.popEnterTransition },
        popExitTransition = { SettingsAnimations.popExitTransition }
    ) {

        composable(route = SettingsDestination.ROUTE_HUB) {
            val viewModel: SettingsHubViewModel = hiltViewModel()
            SettingsHubScreen(
                viewModel = viewModel,
                onBackClick = onBackClick,
                onNavigateToCategory = { destination ->
                    navController.navigate(destination.route)
                }
            )
        }

        composable(route = SettingsDestination.ROUTE_CHAT_SETTINGS) {
            val viewModel: ChatSettingsViewModel = hiltViewModel()
            ChatSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = SettingsDestination.ROUTE_CHAT_FOLDERS) {
            val viewModel: ChatFoldersViewModel = hiltViewModel()
            ChatFoldersScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_ACCOUNT) {
            val viewModel: AccountSettingsViewModel = hiltViewModel()
            AccountSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditProfile = onNavigateToProfileEdit,
                onLogout = onLogout,
                onNavigateToRequestAccountInfo = {
                    navController.navigate(SettingsDestination.ROUTE_REQUEST_ACCOUNT_INFO)
                },
                onNavigateToBusinessPlatform = {
                    navController.navigate(SettingsDestination.ROUTE_BUSINESS_PLATFORM)
                },
                onNavigateToChangeNumber = {
                    navController.navigate(SettingsDestination.ROUTE_CHANGE_NUMBER)
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_REQUEST_ACCOUNT_INFO) {
            val viewModel: RequestAccountInfoViewModel = viewModel()
            RequestAccountInfoScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }



        composable(route = SettingsDestination.ROUTE_PRIVACY) {
            val viewModel: PrivacySecurityViewModel = viewModel()
            PrivacySecurityScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAppLock = {
                    navController.navigate(SettingsDestination.ROUTE_APP_LOCK)
                },
                onNavigateToBlockedUsers = {
                    navController.navigate(SettingsDestination.ROUTE_BLOCKED_CONTACTS)
                },
                onNavigateToMutedUsers = {

                },
                onNavigateToActiveSessions = {

                },
                onNavigateToLastSeen = {
                    navController.navigate(SettingsDestination.ROUTE_PRIVACY_LAST_SEEN)
                },
                onNavigateToProfilePhoto = {
                    navController.navigate(SettingsDestination.ROUTE_PRIVACY_PROFILE_PHOTO)
                },
                onNavigateToAbout = {
                    navController.navigate(SettingsDestination.ROUTE_PRIVACY_ABOUT)
                },
                onNavigateToStatus = {
                    navController.navigate(SettingsDestination.ROUTE_PRIVACY_STATUS)
                },
                onNavigateToGroups = {
                    navController.navigate(SettingsDestination.ROUTE_PRIVACY_GROUPS)
                },
                onNavigateToDisappearingMessages = {
                    navController.navigate(SettingsDestination.ROUTE_PRIVACY_DISAPPEARING_MESSAGES)
                },
                onNavigateToPrivacyCheckup = {
                    navController.navigate(SettingsDestination.ROUTE_PRIVACY_CHECKUP)
                }
            )
        }

        composable(route = SettingsDestination.ROUTE_PRIVACY_CHECKUP) {
            PrivacyCheckupScreen(
                onNavigateToCategory = { categoryId ->
                    navController.navigate(SettingsDestination.createPrivacyCheckupCategoryRoute(categoryId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = SettingsDestination.ROUTE_PRIVACY_CHECKUP_CATEGORY,
            arguments = listOf(
                androidx.navigation.navArgument("categoryId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            PrivacyCheckupCategoryScreen(
                categoryId = categoryId,
                onNavigateToGroups = { navController.navigate(SettingsDestination.ROUTE_PRIVACY_GROUPS) },
                onNavigateToBlockedContacts = { navController.navigate(SettingsDestination.ROUTE_BLOCKED_CONTACTS) },
                onNavigateToProfilePhoto = { navController.navigate(SettingsDestination.ROUTE_PRIVACY_PROFILE_PHOTO) },
                onNavigateToLastSeen = { navController.navigate(SettingsDestination.ROUTE_PRIVACY_LAST_SEEN) },
                onNavigateToAbout = { navController.navigate(SettingsDestination.ROUTE_PRIVACY_ABOUT) },
                onNavigateToStatus = { navController.navigate(SettingsDestination.ROUTE_PRIVACY_STATUS) },
                onNavigateToAppLock = { navController.navigate(SettingsDestination.ROUTE_APP_LOCK) },
                onNavigateToDisappearingMessages = { navController.navigate(SettingsDestination.ROUTE_PRIVACY_DISAPPEARING_MESSAGES) },
                onNavigateToAccountInfo = { navController.navigate(SettingsDestination.ROUTE_REQUEST_ACCOUNT_INFO) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = SettingsDestination.ROUTE_APP_LOCK) {
            val parentEntry = androidx.compose.runtime.remember(it) {
                navController.getBackStackEntry(SettingsDestination.ROUTE_PRIVACY)
            }
            val viewModel: PrivacySecurityViewModel = viewModel(parentEntry)
            AppLockScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = SettingsDestination.ROUTE_PRIVACY_LAST_SEEN) {
            val parentEntry = androidx.compose.runtime.remember(it) {
                navController.getBackStackEntry(SettingsDestination.ROUTE_PRIVACY)
            }
            val viewModel: PrivacySecurityViewModel = viewModel(parentEntry)
            val privacySettings by viewModel.privacySettings.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()

            PrivacySelectionScreen(
                title = stringResource(R.string.settings_last_seen_title),
                subtitle = stringResource(R.string.settings_last_seen_subtitle),
                options = getStandardPrivacyOptions(excludedCount = privacySettings.lastSeenExcludedUserIds.size) { LastSeenVisibility.valueOf(it) },
                selectedOption = privacySettings.lastSeenVisibility,
                onOptionSelected = { viewModel.setLastSeenVisibility(it) },
                onNavigateToExceptionSelector = {
                    navController.navigate(SettingsDestination.createPrivacyExceptionSelectorRoute("last_seen"))
                },
                onNavigateBack = { navController.popBackStack() },
                isLoading = isLoading
            )
        }

        composable(route = SettingsDestination.ROUTE_PRIVACY_GROUPS) {
            val parentEntry = androidx.compose.runtime.remember(it) {
                navController.getBackStackEntry(SettingsDestination.ROUTE_PRIVACY)
            }
            val viewModel: PrivacySecurityViewModel = viewModel(parentEntry)
            val privacySettings by viewModel.privacySettings.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()

            val options = GroupPrivacy.values().map { groupPrivacy ->
                PrivacyOptionItem(
                    title = groupPrivacy.displayName(),
                    subtitle = when (groupPrivacy) {
                        GroupPrivacy.EVERYONE -> "Anyone on Synapse can add you to groups."
                        GroupPrivacy.MY_CONTACTS -> "Only your contacts can add you to groups."
                        GroupPrivacy.MY_CONTACTS_EXCEPT -> "Your contacts can add you to groups, except excluded contacts."
                        GroupPrivacy.NOBODY -> "Nobody can add you to groups automatically."
                    },
                    value = groupPrivacy,
                    isExceptionOption = groupPrivacy == GroupPrivacy.MY_CONTACTS_EXCEPT
                )
            }

            PrivacySelectionScreen(
                title = stringResource(R.string.settings_groups_title),
                subtitle = stringResource(R.string.settings_groups_subtitle),
                options = options,
                selectedOption = privacySettings.groupPrivacy,
                onOptionSelected = { viewModel.setGroupPrivacy(it) },
                onNavigateToExceptionSelector = {
                    navController.navigate(SettingsDestination.createPrivacyExceptionSelectorRoute("group"))
                },
                onNavigateBack = { navController.popBackStack() },
                isLoading = isLoading
            )
        }

        composable(route = SettingsDestination.ROUTE_PRIVACY_DISAPPEARING_MESSAGES) {
            var selectedTimer by remember { mutableStateOf("Off") }

            val timerOptions = listOf(
                PrivacyOptionItem(
                    title = "24 hours",
                    subtitle = "Messages in new chats disappear after 24 hours.",
                    value = "24h"
                ),
                PrivacyOptionItem(
                    title = "7 days",
                    subtitle = "Messages in new chats disappear after 7 days.",
                    value = "7d"
                ),
                PrivacyOptionItem(
                    title = "90 days",
                    subtitle = "Messages in new chats disappear after 90 days.",
                    value = "90d"
                ),
                PrivacyOptionItem(
                    title = "Off",
                    subtitle = "Messages in new chats will not disappear automatically.",
                    value = "Off"
                )
            )

            PrivacySelectionScreen(
                title = stringResource(R.string.settings_disappearing_messages_title),
                subtitle = stringResource(R.string.settings_disappearing_messages_subtitle),
                options = timerOptions,
                selectedOption = selectedTimer,
                onOptionSelected = { selectedTimer = it },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = SettingsDestination.ROUTE_PRIVACY_EXCEPTION_SELECTOR,
            arguments = listOf(
                androidx.navigation.navArgument("settingKey") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val settingKey = backStackEntry.arguments?.getString("settingKey") ?: ""
            val viewModel: PrivacyExceptionSelectorViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(settingKey) {
                viewModel.initialize(settingKey)
            }

            val screenTitle = when (settingKey) {
                "last_seen" -> stringResource(R.string.settings_last_seen_title)
                "profile_photo" -> stringResource(R.string.settings_profile_photo_title)
                "about" -> stringResource(R.string.settings_about_privacy_title)
                "status" -> stringResource(R.string.settings_status_title)
                "group" -> stringResource(R.string.settings_groups_title)
                else -> stringResource(R.string.settings_privacy_t)
            }

            PrivacyExceptionSelectorScreen(
                title = screenTitle,
                uiState = uiState,
                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onToggleUserSelection = { viewModel.toggleUserSelection(it) },
                onSaveClicked = {
                    viewModel.saveSelection {
                        navController.popBackStack()
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = SettingsDestination.ROUTE_PRIVACY_PROFILE_PHOTO) {
            val parentEntry = androidx.compose.runtime.remember(it) {
                navController.getBackStackEntry(SettingsDestination.ROUTE_PRIVACY)
            }
            val viewModel: PrivacySecurityViewModel = viewModel(parentEntry)
            val privacySettings by viewModel.privacySettings.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()

            PrivacySelectionScreen(
                title = stringResource(R.string.settings_profile_photo_title),
                subtitle = stringResource(R.string.settings_profile_photo_subtitle),
                options = getStandardPrivacyOptions(excludedCount = privacySettings.profilePhotoExcludedUserIds.size) { ProfilePhotoVisibility.valueOf(it) },
                selectedOption = privacySettings.profilePhotoVisibility,
                onOptionSelected = { viewModel.setProfilePhotoVisibility(it) },
                onNavigateToExceptionSelector = {
                    navController.navigate(SettingsDestination.createPrivacyExceptionSelectorRoute("profile_photo"))
                },
                onNavigateBack = { navController.popBackStack() },
                isLoading = isLoading
            )
        }

        composable(route = SettingsDestination.ROUTE_PRIVACY_ABOUT) {
            val parentEntry = androidx.compose.runtime.remember(it) {
                navController.getBackStackEntry(SettingsDestination.ROUTE_PRIVACY)
            }
            val viewModel: PrivacySecurityViewModel = viewModel(parentEntry)
            val privacySettings by viewModel.privacySettings.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()

            PrivacySelectionScreen(
                title = stringResource(R.string.settings_about_privacy_title),
                subtitle = stringResource(R.string.settings_about_subtitle),
                options = getStandardPrivacyOptions(excludedCount = privacySettings.aboutExcludedUserIds.size) { AboutVisibility.valueOf(it) },
                selectedOption = privacySettings.aboutVisibility,
                onOptionSelected = { viewModel.setAboutVisibility(it) },
                onNavigateToExceptionSelector = {
                    navController.navigate(SettingsDestination.createPrivacyExceptionSelectorRoute("about"))
                },
                onNavigateBack = { navController.popBackStack() },
                isLoading = isLoading
            )
        }

        composable(route = SettingsDestination.ROUTE_PRIVACY_STATUS) {
            val parentEntry = androidx.compose.runtime.remember(it) {
                navController.getBackStackEntry(SettingsDestination.ROUTE_PRIVACY)
            }
            val viewModel: PrivacySecurityViewModel = viewModel(parentEntry)
            val privacySettings by viewModel.privacySettings.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()

            PrivacySelectionScreen(
                title = stringResource(R.string.settings_status_title),
                subtitle = stringResource(R.string.settings_status_subtitle),
                options = getStandardPrivacyOptions(excludedCount = privacySettings.statusExcludedUserIds.size) { StatusVisibility.valueOf(it) },
                selectedOption = privacySettings.statusVisibility,
                onOptionSelected = { viewModel.setStatusVisibility(it) },
                onNavigateToExceptionSelector = {
                    navController.navigate(SettingsDestination.createPrivacyExceptionSelectorRoute("status"))
                },
                onNavigateBack = { navController.popBackStack() },
                isLoading = isLoading
            )
        }


        composable(route = SettingsDestination.ROUTE_APPEARANCE) {
            val viewModel: AppearanceViewModel = viewModel()
            AppearanceScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChatCustomization = {

                },
                onNavigateToFont = {
                    navController.navigate(SettingsDestination.ROUTE_FONT)
                }
            )
        }

        composable(route = SettingsDestination.ROUTE_FONT) {
            val viewModel: FontViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
            FontScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_BACKUP_RESTORE) {
            val viewModel: SettingsBackupViewModel = hiltViewModel()
            SettingsBackupScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = SettingsDestination.ROUTE_NOTIFICATIONS) {
            val viewModel: NotificationSettingsViewModel = hiltViewModel()
            NotificationSettingsScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }




        composable(route = SettingsDestination.ROUTE_STORAGE) {
            val viewModel: StorageDataViewModel = viewModel(
                factory = StorageDataViewModelFactory(settingsRepository)
            )
            StorageDataScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                navController = navController
            )
        }


        composable(route = SettingsDestination.ROUTE_MANAGE_STORAGE) {
            val viewModel: ManageStorageViewModel = hiltViewModel()
            ManageStorageScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_NETWORK_USAGE) {
            val viewModel: NetworkUsageViewModel = hiltViewModel()
            NetworkUsageScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_BUSINESS_PLATFORM) {
            val viewModel: BusinessPlatformViewModel = hiltViewModel()
            val currentContext = LocalContext.current
            BusinessPlatformScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToScheduledPosts = {
                    navController.navigate(SettingsDestination.ROUTE_SCHEDULED_POSTS)
                },
                onNavigateToContentCalendar = {
                    android.widget.Toast.makeText(currentContext, "Content Calendar coming soon", android.widget.Toast.LENGTH_SHORT).show()
                },
                onNavigateToBrandPartnerships = {
                    android.widget.Toast.makeText(currentContext, "Brand Partnerships coming soon", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }

        composable(route = SettingsDestination.ROUTE_SCHEDULED_POSTS) {
            val viewModel: ScheduledPostsViewModel = hiltViewModel()
            ScheduledPostsScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = SettingsDestination.ROUTE_CHANGE_NUMBER) {
            val viewModel: ChangeNumberViewModel = hiltViewModel()
            ChangeNumberScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_STORAGE_PROVIDER) {
            val viewModel: SettingsViewModel = hiltViewModel()
            StorageProviderScreen(
                navController = navController,
                viewModel = viewModel
            )
        }


        composable(route = SettingsDestination.ROUTE_LANGUAGE) {
            val viewModel: LanguageRegionViewModel = viewModel()
            LanguageRegionScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_ABOUT) {
            val viewModel: AboutSupportViewModel = viewModel()
            AboutSupportScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToLicenses = {
                    navController.navigate(SettingsDestination.ROUTE_LICENSES)
                },
                viewModel = viewModel
            )
        }


        composable(route = SettingsDestination.ROUTE_LICENSES) {
            LicensesScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }



        composable(route = SettingsDestination.ROUTE_FLAGS) {
            val viewModel: FlagsViewModel = hiltViewModel()
            FlagsScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = SettingsDestination.ROUTE_API_KEY) {
            val viewModel: ApiKeySettingsViewModel = hiltViewModel()
            ApiKeySettingsScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_SYNAPSE_PLUS) {
            SynapsePlusScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_AVATAR) {
            val viewModel: AvatarViewModel = hiltViewModel()
            AvatarScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_FAVOURITES) {
            FavouritesScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_ACCESSIBILITY) {
            val viewModel: AccessibilityViewModel = hiltViewModel()
            AccessibilityScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_BLOCKED_CONTACTS) {
            val viewModel: BlockingViewModel = hiltViewModel()
            BlockedContactsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }


        composable(route = SettingsDestination.ROUTE_SEARCH) {
            com.synapse.social.studioasinc.feature.settings.search.SettingsSearchScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSetting = { route -> navController.navigate(route) }
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatPlaceholderScreen(
    title: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.settings_chat_not_implemented),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
