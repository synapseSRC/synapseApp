package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
                onNavigateToBlockedUsers = {
                    navController.navigate(SettingsDestination.ROUTE_BLOCKED_CONTACTS)
                },
                onNavigateToMutedUsers = {

                },
                onNavigateToActiveSessions = {

                }
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

                }
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
                    android.widget.Toast.makeText(currentContext, "Scheduled Posts coming soon", android.widget.Toast.LENGTH_SHORT).show()
                },
                onNavigateToContentCalendar = {
                    android.widget.Toast.makeText(currentContext, "Content Calendar coming soon", android.widget.Toast.LENGTH_SHORT).show()
                },
                onNavigateToBrandPartnerships = {
                    android.widget.Toast.makeText(currentContext, "Brand Partnerships coming soon", android.widget.Toast.LENGTH_SHORT).show()
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
            AccessibilityScreen(
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
                text = "Chat feature not implemented",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
