package com.synapse.social.studioasinc.feature.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileEvent
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileScreen
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileViewModel
import com.synapse.social.studioasinc.feature.profile.editprofile.media.ProfileMediaScreen
import com.synapse.social.studioasinc.feature.profile.personalinfo.PersonalInformationScreen
import com.synapse.social.studioasinc.presentation.editprofile.social.SocialLinksScreen
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.PhotoHistoryScreen
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.PhotoType
import com.synapse.social.studioasinc.feature.profile.editprofile.appearance.ProfileAppearanceScreen
import com.synapse.social.studioasinc.feature.profile.editprofile.interestsskills.InterestsSkillsScreen
import com.synapse.social.studioasinc.ui.settings.SelectRegionScreen
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileEditActivity : AppCompatActivity() {

    private val viewModel: EditProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("ProfileEditActivity", "onCreate called")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        try {
            android.util.Log.d("ProfileEditActivity", "Setting up content")
            setContent {
                SynapseTheme {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "edit_profile") {
                        composable("edit_profile") {
                            EditProfileScreen(
                                viewModel = viewModel,
                                onNavigateBack = { finish() },
                                onNavigateToRegionSelection = { _ ->
                                    navController.navigate("edit_profile_location")
                                },
                                onNavigateToProfileMedia = {
                                    navController.navigate("profile_media")
                                },
                                onNavigateToPhotoHistory = { type ->
                                    navController.navigate("photo_history/$type")
                                },
                                onNavigateToInterestsSkills = {
                                    navController.navigate("interests_skills")
                                },
                                onNavigateToWorkEducation = {
                                    navController.navigate("work_education")
                                },
                                onNavigateToPersonalInfo = {
                                    navController.navigate("personal_information")
                                },
                                onNavigateToSocialLinks = {
                                    navController.navigate("edit_profile_social_links")
                                },
                                onNavigateToProfileAppearance = {
                                    navController.navigate("profile_appearance")
                                },
                                onNavigateToProfessionalProfile = {
                                    navController.navigate("professional_profile")
                                },
                                onNavigateToFeaturedContent = {
                                    navController.navigate("featured_content")
                                }
                            )
                        }

                        composable("featured_content") {
                            com.synapse.social.studioasinc.feature.profile.editprofile.featured.FeaturedContentScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("profile_media") {
                            ProfileMediaScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPhotoHistory = { type ->
                                    navController.navigate("photo_history/$type")
                                }
                            )
                        }

                        composable("profile_appearance") {
                            ProfileAppearanceScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("edit_profile_location") {
                            val locationViewModel: com.synapse.social.studioasinc.feature.profile.editprofile.location.LocationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

                            val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
                            val selectedRegion = savedStateHandle?.get<String>("selected_region")

                            androidx.compose.runtime.LaunchedEffect(selectedRegion) {
                                selectedRegion?.let { region ->
                                    locationViewModel.onEvent(com.synapse.social.studioasinc.feature.profile.editprofile.location.LocationEvent.RegionSelected(region))
                                    savedStateHandle.remove<String>("selected_region")
                                }
                            }

                            com.synapse.social.studioasinc.feature.profile.editprofile.location.LocationScreen(
                                viewModel = locationViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToRegionSelection = { currentRegion ->
                                    val encodedRegion = java.net.URLEncoder.encode(currentRegion, "UTF-8")
                                    navController.navigate("select_region?currentRegion=$encodedRegion")
                                }
                            )
                        }

                        composable("professional_profile") {
                            com.synapse.social.studioasinc.feature.profile.editprofile.professional.ProfessionalProfileScreen(
                                onBackClick = { navController.popBackStack() },
                                onNavigateToBusinessPlatform = {
                                    startActivity(android.content.Intent(this@ProfileEditActivity, com.synapse.social.studioasinc.feature.settings.SettingsActivity::class.java).apply {
                                        putExtra("destination", "settings_business_platform")
                                    })
                                }
                            )
                        }

                        composable("interests_skills") {
                            InterestsSkillsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("work_education") {
                            com.synapse.social.studioasinc.feature.profile.editprofile.workeducation.WorkEducationScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("personal_information") {
                            PersonalInformationScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("edit_profile_social_links") {
                            SocialLinksScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "select_region?currentRegion={currentRegion}",
                            arguments = listOf(navArgument("currentRegion") {
                                type = NavType.StringType
                                defaultValue = ""
                            })
                        ) { backStackEntry ->
                            val currentRegion = backStackEntry.arguments?.getString("currentRegion") ?: ""
                            SelectRegionScreen(
                                currentRegion = currentRegion,
                                onRegionSelected = { region ->
                                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_region", region)
                                    navController.popBackStack()
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            route = "photo_history/{type}",
                            arguments = listOf(navArgument("type") {
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            val typeStr = backStackEntry.arguments?.getString("type") ?: "PROFILE"
                            val photoType = try {
                                 PhotoType.valueOf(typeStr)
                            } catch (e: Exception) {
                                 PhotoType.PROFILE
                            }

                            PhotoHistoryScreen(
                                type = photoType,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
            android.util.Log.d("ProfileEditActivity", "Content setup completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("ProfileEditActivity", "Error in onCreate", e)
            throw e
        }
    }
}
