package com.synapse.social.studioasinc.feature.auth.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.synapse.social.studioasinc.feature.auth.ui.components.LoadingOverlay
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onNavigateToMain: () -> Unit
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val reducedMotion = AnimationUtil.rememberReducedMotion()
    val context = androidx.compose.ui.platform.LocalContext.current


    var lastContentState by remember { mutableStateOf<AuthUiState>(AuthUiState.Initial) }


    if (uiState !is AuthUiState.Loading) {
        lastContentState = uiState
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is AuthNavigationEvent.NavigateToMain -> onNavigateToMain()
                is AuthNavigationEvent.NavigateToSignIn -> {
                    navController.navigate("signIn") {
                        popUpTo("signIn") { inclusive = true }
                    }
                }
                is AuthNavigationEvent.NavigateToSignUp -> navController.navigate("signUp")
                is AuthNavigationEvent.NavigateToEmailVerification -> navController.navigate("emailVerification")
                is AuthNavigationEvent.NavigateToForgotPassword -> navController.navigate("forgotPassword")
                is AuthNavigationEvent.NavigateToResetPassword -> navController.navigate("resetPassword")
                is AuthNavigationEvent.NavigateBack -> navController.popBackStack()
                is AuthNavigationEvent.OpenUrl -> {
                    val uri = android.net.Uri.parse(event.url)
                    val intent = androidx.browser.customtabs.CustomTabsIntent.Builder().build()
                    intent.launchUrl(context, uri)
                }
            }
        }
    }

    LoadingOverlay(
        isLoading = uiState is AuthUiState.Loading
    ) {
        NavHost(
            navController = navController,
            startDestination = "signIn",
            enterTransition = {
                if (reducedMotion) androidx.compose.animation.EnterTransition.None
                else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
            },
            exitTransition = {
                if (reducedMotion) androidx.compose.animation.ExitTransition.None
                else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
            },
            popEnterTransition = {
                if (reducedMotion) androidx.compose.animation.EnterTransition.None
                else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
            },
            popExitTransition = {
                if (reducedMotion) androidx.compose.animation.ExitTransition.None
                else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
            }
        ) {
            composable("signIn") {
                val state = lastContentState
                if (state is AuthUiState.SignIn) {
                    SignInScreen(
                        state = state,
                        onEmailChanged = viewModel::onEmailChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onSignInClick = {
                             viewModel.onSignInClick()
                        },
                        onForgotPasswordClick = viewModel::onForgotPasswordClick,
                        onToggleModeClick = viewModel::onToggleModeClick,
                        onOAuthClick = viewModel::onOAuthClick
                    )
                }
            }

            composable("signUp") {
                val state = lastContentState
                if (state is AuthUiState.SignUp) {
                    SignUpScreen(
                        state = state,
                        onEmailChanged = viewModel::onEmailChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onUsernameChanged = viewModel::onUsernameChanged,
                        onSignUpClick = {
                             viewModel.onSignUpClick()
                        },
                        onToggleModeClick = viewModel::onToggleModeClick,
                        onOAuthClick = viewModel::onOAuthClick
                    )
                }
            }

            composable("emailVerification") {
                val state = lastContentState
                if (state is AuthUiState.EmailVerification) {
                    EmailVerificationScreen(
                        state = state,
                        onResendClick = viewModel::onResendVerificationEmail,
                        onBackToSignInClick = viewModel::onBackToSignInClick
                    )
                }
            }

            composable("forgotPassword") {
                val state = lastContentState
                if (state is AuthUiState.ForgotPassword) {
                    ForgotPasswordScreen(
                        state = state,
                        onEmailChanged = viewModel::onEmailChanged,
                        onSendResetLinkClick = {
                             viewModel.onSubmitNewPassword()
                        },
                        onBackClick = viewModel::onBackToSignInClick
                    )
                }
            }

            composable("resetPassword") {
                 val state = lastContentState
                 if (state is AuthUiState.ResetPassword) {
                     ResetPasswordScreen(
                         state = state,
                         onPasswordChanged = viewModel::onPasswordChanged,
                         onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
                         onResetPasswordClick = {


                             viewModel.onSubmitNewPassword()
                         }
                     )
                 }
            }
        }
    }
}
