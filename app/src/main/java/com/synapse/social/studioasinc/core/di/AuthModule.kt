package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
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
    fun provideGeneratePasskeyRequestJsonUseCase(): GeneratePasskeyRequestJsonUseCase {
        return GeneratePasskeyRequestJsonUseCase()
    }

    @Provides
    @Singleton
    fun provideLoadPasskeysUseCase(
        authRepository: AuthRepository,
        passkeyRepository: com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
    ): com.synapse.social.studioasinc.domain.usecase.auth.LoadPasskeysUseCase {
        return com.synapse.social.studioasinc.domain.usecase.auth.LoadPasskeysUseCase(authRepository, passkeyRepository)
    }

    @Provides
    @Singleton
    fun provideAddPasskeyUseCase(
        authRepository: AuthRepository,
        passkeyRepository: com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository,
        generatePasskeyRequestJsonUseCase: GeneratePasskeyRequestJsonUseCase,
        passkeyManager: com.synapse.social.studioasinc.domain.repository.PasskeyManager,
        deviceInfoProvider: com.synapse.social.studioasinc.domain.repository.DeviceInfoProvider
    ): com.synapse.social.studioasinc.domain.usecase.auth.AddPasskeyUseCase {
        return com.synapse.social.studioasinc.domain.usecase.auth.AddPasskeyUseCase(
            authRepository,
            passkeyRepository,
            generatePasskeyRequestJsonUseCase,
            passkeyManager,
            deviceInfoProvider
        )
    }

    @Provides
    @Singleton
    fun provideRemovePasskeyUseCase(
        passkeyRepository: com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
    ): com.synapse.social.studioasinc.domain.usecase.auth.RemovePasskeyUseCase {
        return com.synapse.social.studioasinc.domain.usecase.auth.RemovePasskeyUseCase(passkeyRepository)
    }
}
