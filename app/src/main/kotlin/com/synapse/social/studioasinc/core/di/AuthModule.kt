package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyPlatformManager
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
import com.synapse.social.studioasinc.shared.domain.repository.UserRepository
import com.synapse.social.studioasinc.shared.domain.usecase.auth.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideIsPostOwnerUseCase(getCurrentUserIdUseCase: GetCurrentUserIdUseCase): com.synapse.social.studioasinc.shared.domain.usecase.post.IsPostOwnerUseCase {
        return com.synapse.social.studioasinc.shared.domain.usecase.post.IsPostOwnerUseCase(getCurrentUserIdUseCase)
    }

    @Provides
    @Singleton
    fun provideValidateEmailUseCase(): ValidateEmailUseCase {
        return ValidateEmailUseCase()
    }

    @Provides
    @Singleton
    fun provideValidatePasswordUseCase(): ValidatePasswordUseCase {
        return ValidatePasswordUseCase()
    }

    @Provides
    @Singleton
    fun provideValidateUsernameUseCase(): ValidateUsernameUseCase {
        return ValidateUsernameUseCase()
    }

    @Provides
    @Singleton
    fun provideCalculatePasswordStrengthUseCase(): CalculatePasswordStrengthUseCase {
        return CalculatePasswordStrengthUseCase()
    }

    @Provides
    @Singleton
    fun provideCheckUsernameAvailabilityUseCase(userRepository: UserRepository): CheckUsernameAvailabilityUseCase {
        return CheckUsernameAvailabilityUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideSignInUseCase(authRepository: AuthRepository): SignInUseCase {
        return SignInUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideSignUpUseCase(authRepository: AuthRepository): SignUpUseCase {
        return SignUpUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideSendPasswordResetUseCase(authRepository: AuthRepository): SendPasswordResetUseCase {
        return SendPasswordResetUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideResetPasswordUseCase(authRepository: AuthRepository): ResetPasswordUseCase {
        return ResetPasswordUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideResendVerificationEmailUseCase(authRepository: AuthRepository): ResendVerificationEmailUseCase {
        return ResendVerificationEmailUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideHandleOAuthCallbackUseCase(authRepository: AuthRepository): HandleOAuthCallbackUseCase {
        return HandleOAuthCallbackUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideGetOAuthUrlUseCase(authRepository: AuthRepository): GetOAuthUrlUseCase {
        return GetOAuthUrlUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideSignInWithOAuthUseCase(authRepository: AuthRepository): SignInWithOAuthUseCase {
        return SignInWithOAuthUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideSignInWithGoogleIdTokenUseCase(authRepository: AuthRepository): SignInWithGoogleIdTokenUseCase {
        return SignInWithGoogleIdTokenUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideRefreshSessionUseCase(authRepository: AuthRepository): RefreshSessionUseCase {
        return RefreshSessionUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideIsEmailVerifiedUseCase(authRepository: AuthRepository): IsEmailVerifiedUseCase {
        return IsEmailVerifiedUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideGetCurrentUserIdUseCase(authRepository: AuthRepository): GetCurrentUserIdUseCase {
        return GetCurrentUserIdUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideLoadPasskeysUseCase(
        passkeyRepository: PasskeyRepository
    ): LoadPasskeysUseCase {
        return LoadPasskeysUseCase(passkeyRepository)
    }

    @Provides
    @Singleton
    fun provideAddPasskeyUseCase(
        passkeyRepository: PasskeyRepository,
        passkeyPlatformManager: PasskeyPlatformManager
    ): AddPasskeyUseCase {
        return AddPasskeyUseCase(
            passkeyRepository,
            passkeyPlatformManager
        )
    }

    @Provides
    @Singleton
    fun provideRemovePasskeyUseCase(
        passkeyRepository: PasskeyRepository
    ): RemovePasskeyUseCase {
        return RemovePasskeyUseCase(passkeyRepository)
    }

    @Provides
    @Singleton
    fun provideSignInWithPasskeyUseCase(
        authRepository: AuthRepository,
        passkeyRepository: PasskeyRepository,
        passkeyPlatformManager: PasskeyPlatformManager
    ): SignInWithPasskeyUseCase {
        return SignInWithPasskeyUseCase(
            authRepository,
            passkeyRepository,
            passkeyPlatformManager
        )
    }

    @Provides
    @Singleton
    fun provideSupabaseAuthenticationService(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService {
        return com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService.getInstance(context)
    }
}