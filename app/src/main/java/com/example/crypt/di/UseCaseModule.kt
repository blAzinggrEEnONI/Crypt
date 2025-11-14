package com.example.crypt.di

import android.content.Context
import com.example.crypt.domain.usecase.AuthUseCase
import com.example.crypt.domain.usecase.AutoLockUseCase
import com.example.crypt.domain.usecase.GeneratePasswordUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing domain layer use case dependencies.
 * Configures proper scoping for use case instances and their dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    
    /**
     * Provides the GeneratePasswordUseCase for cryptographically secure password generation.
     * Scoped as Singleton for consistent password generation behavior.
     */
    @Provides
    @Singleton
    fun provideGeneratePasswordUseCase(): GeneratePasswordUseCase {
        return GeneratePasswordUseCase()
    }
    
    /**
     * Provides the AuthUseCase for biometric and PIN authentication.
     * Scoped as Singleton to maintain authentication state across the app.
     */
    @Provides
    @Singleton
    fun provideAuthUseCase(
        @ApplicationContext context: Context
    ): AuthUseCase {
        return AuthUseCase(context)
    }
    
    /**
     * Provides the AutoLockUseCase for session management and automatic locking.
     * Scoped as Singleton to maintain consistent app lock state and lifecycle management.
     */
    @Provides
    @Singleton
    fun provideAutoLockUseCase(
        authUseCase: AuthUseCase
    ): AutoLockUseCase {
        return AutoLockUseCase(authUseCase)
    }
}