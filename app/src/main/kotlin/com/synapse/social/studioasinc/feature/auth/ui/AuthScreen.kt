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
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.EmailVerificationViewModel
import com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.ForgotPasswordViewModel
import com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.ResetPasswordViewModel
import com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onInitiateGoogleSignIn: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val navController = rememberNavController()
    val reducedMotion = AnimationUtil.rememberReducedMotion()
    val context = androidx.compose.ui.platform.LocalContext.current

    val handleNavigationEvent: (AuthNavigationEvent) -> Unit = { event ->
        when (event) {
            is AuthNavigationEvent.NavigateToMain -> onNavigateToMain()
            is AuthNavigationEvent.NavigateToSignIn -> {
                navController.navigate("authMain") {
                    popUpTo("authMain") { inclusive = true }
                }
            }
            is AuthNavigationEvent.NavigateToSignUp -> {
                navController.navigate("authMain") {
                    popUpTo("authMain") { inclusive = true }
                }
            }
            is AuthNavigationEvent.NavigateToEmailVerification -> {
                val encodedEmail = android.net.Uri.encode(event.email)
                navController.navigate("emailVerification/$encodedEmail")
            }
            is AuthNavigationEvent.NavigateToForgotPassword -> navController.navigate("forgotPassword")
            is AuthNavigationEvent.NavigateToResetPassword -> navController.navigate("resetPassword")
            is AuthNavigationEvent.NavigateBack -> navController.popBackStack()
            is AuthNavigationEvent.OpenUrl -> {
                val uri = android.net.Uri.parse(event.url)
                val intent = androidx.browser.customtabs.CustomTabsIntent.Builder().build()
                intent.launchUrl(context, uri)
            }
            is AuthNavigationEvent.InitiateGoogleSignIn -> {
                onInitiateGoogleSignIn()
            }
        }
    }

    // Collect Activity-scoped AuthViewModel events at the top level
    // so deep link callbacks (NavigateToMain) are not missed when screens transition.
    LaunchedEffect(authViewModel) {
        authViewModel.navigationEvent.collect { handleNavigationEvent(it) }
    }

    LoadingOverlay(
        isLoading = false
    ) {
        NavHost(
            navController = navController,
            startDestination = "authMain",
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
            composable("authMain") {
                val state by authViewModel.uiState.collectAsState()

                UnifiedAuthContent(
                    state = state,
                    authViewModel = authViewModel
                )
            }

            composable(
                route = "emailVerification/{email}",
                arguments = listOf(androidx.navigation.navArgument("email") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                val viewModel = hiltViewModel<EmailVerificationViewModel>()
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(email) {
                    viewModel.initEmail(email)
                }

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { handleNavigationEvent(it) }
                }

                EmailVerificationScreen(
                    state = state as? AuthUiState.EmailVerification ?: AuthUiState.EmailVerification(email = email),
                    onResendClick = viewModel::onResendVerificationEmail,
                    onBackToSignInClick = viewModel::onBackToSignInClick
                )
            }

            composable("forgotPassword") {
                val viewModel = hiltViewModel<ForgotPasswordViewModel>()
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { handleNavigationEvent(it) }
                }

                ForgotPasswordScreen(
                    state = state as? AuthUiState.ForgotPassword ?: AuthUiState.ForgotPassword(),
                    onEmailChanged = viewModel::onEmailChanged,
                    onSendResetLinkClick = viewModel::onSubmitNewPassword,
                    onBackClick = viewModel::onBackToSignInClick
                )
            }

            composable("resetPassword") {
                val viewModel = hiltViewModel<ResetPasswordViewModel>()
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { handleNavigationEvent(it) }
                }

                ResetPasswordScreen(
                    state = state as? AuthUiState.ResetPassword ?: AuthUiState.ResetPassword(),
                    onPasswordChanged = viewModel::onPasswordChanged,
                    onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
                    onResetPasswordClick = viewModel::onSubmitNewPassword
                )
            }
        }
    }
}

@Composable
private fun UnifiedAuthContent(
    state: AuthUiState,
    authViewModel: AuthViewModel
) {
    val isSignUpMode = state is AuthUiState.SignUp

    if (state is AuthUiState.SignUp && state.showSuccessDialog) {
        com.synapse.social.studioasinc.feature.auth.ui.components.UserCreatedDialog(
            onDismiss = authViewModel::onDismissSuccessDialog
        )
    }

    SignInSignUpScreenContent(
        isSignUpMode = isSignUpMode,
        state = state,
        onEmailChanged = authViewModel::onEmailChanged,
        onPasswordChanged = authViewModel::onPasswordChanged,
        onUsernameChanged = authViewModel::onUsernameChanged,
        onSignInClick = authViewModel::onSignInClick,
        onSignUpClick = authViewModel::onSignUpClick,
        onForgotPasswordClick = authViewModel::onForgotPasswordClick,
        onToggleModeClick = authViewModel::onToggleModeClick,
        onOAuthClick = authViewModel::onOAuthClick,
        onDismissError = authViewModel::onDismissGeneralError
    )
}
