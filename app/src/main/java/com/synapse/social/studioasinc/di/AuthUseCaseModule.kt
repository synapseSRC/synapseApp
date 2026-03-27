package com.synapse.social.studioasinc.di

import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateEmailUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidatePasswordUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateUsernameUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateSignInCredentialsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateSignUpCredentialsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateResetPasswordEmailUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateNewPasswordUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthUseCaseModule {

    @Provides
    @Singleton
    fun provideValidateSignInCredentialsUseCase(
        validateEmailUseCase: ValidateEmailUseCase,
        validatePasswordUseCase: ValidatePasswordUseCase
    ): ValidateSignInCredentialsUseCase {
        return ValidateSignInCredentialsUseCase(validateEmailUseCase, validatePasswordUseCase)
    }

    @Provides
    @Singleton
    fun provideValidateSignUpCredentialsUseCase(
        validateEmailUseCase: ValidateEmailUseCase,
        validatePasswordUseCase: ValidatePasswordUseCase,
        validateUsernameUseCase: ValidateUsernameUseCase
    ): ValidateSignUpCredentialsUseCase {
        return ValidateSignUpCredentialsUseCase(validateEmailUseCase, validatePasswordUseCase, validateUsernameUseCase)
    }

    @Provides
    @Singleton
    fun provideValidateResetPasswordEmailUseCase(
        validateEmailUseCase: ValidateEmailUseCase
    ): ValidateResetPasswordEmailUseCase {
        return ValidateResetPasswordEmailUseCase(validateEmailUseCase)
    }

    @Provides
    @Singleton
    fun provideValidateNewPasswordUseCase(
        validatePasswordUseCase: ValidatePasswordUseCase
    ): ValidateNewPasswordUseCase {
        return ValidateNewPasswordUseCase(validatePasswordUseCase)
    }
}
