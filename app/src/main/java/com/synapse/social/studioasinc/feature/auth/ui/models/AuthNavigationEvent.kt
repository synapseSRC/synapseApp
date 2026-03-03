package com.synapse.social.studioasinc.feature.auth.ui.models



sealed class AuthNavigationEvent {


    object NavigateToMain : AuthNavigationEvent()



    object NavigateToSignIn : AuthNavigationEvent()



    object NavigateToSignUp : AuthNavigationEvent()



    object NavigateToEmailVerification : AuthNavigationEvent()



    object NavigateToForgotPassword : AuthNavigationEvent()



    data class NavigateToResetPassword(val token: String) : AuthNavigationEvent()



    object NavigateBack : AuthNavigationEvent()



    data class OpenUrl(val url: String) : AuthNavigationEvent()
}
