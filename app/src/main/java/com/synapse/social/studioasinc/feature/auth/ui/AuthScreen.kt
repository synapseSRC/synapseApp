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
import com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.SignInViewModel
import com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.SignUpViewModel

@Composable
fun AuthScreen(
    signInViewModel: SignInViewModel,
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
                navController.navigate("signIn") {
                    popUpTo("signIn") { inclusive = true }
                }
            }
            is AuthNavigationEvent.NavigateToSignUp -> navController.navigate("signUp")
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

    // Collect Activity-scoped SignInViewModel events at the top level
    // so deep link callbacks (NavigateToMain) are not missed when SignInScreen is not composed.
    LaunchedEffect(signInViewModel) {
        signInViewModel.navigationEvent.collect { handleNavigationEvent(it) }
    }

    LoadingOverlay(
        isLoading = false
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
                val state by signInViewModel.uiState.collectAsState()

                SignInScreen(
                    state = state as? AuthUiState.SignIn ?: AuthUiState.SignIn(),
                    onEmailChanged = signInViewModel::onEmailChanged,
                    onPasswordChanged = signInViewModel::onPasswordChanged,
                    onSignInClick = signInViewModel::onSignInClick,
                    onForgotPasswordClick = signInViewModel::onForgotPasswordClick,
                    onToggleModeClick = signInViewModel::onToggleModeClick,
                    onOAuthClick = signInViewModel::onOAuthClick,
                    onDismissError = signInViewModel::onDismissError
                )
            }

            composable("signUp") {
                val viewModel = hiltViewModel<SignUpViewModel>()
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { handleNavigationEvent(it) }
                }

                SignUpScreen(
                    state = state as? AuthUiState.SignUp ?: AuthUiState.SignUp(),
                    onEmailChanged = viewModel::onEmailChanged,
                    onPasswordChanged = viewModel::onPasswordChanged,
                    onUsernameChanged = viewModel::onUsernameChanged,
                    onSignUpClick = viewModel::onSignUpClick,
                    onToggleModeClick = viewModel::onToggleModeClick,
                    onOAuthClick = viewModel::onOAuthClick,
                    onDismissSuccessDialog = viewModel::onDismissSuccessDialog,
                    onDismissError = viewModel::onDismissError
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
