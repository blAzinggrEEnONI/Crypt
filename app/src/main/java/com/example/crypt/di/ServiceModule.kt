package com.example.crypt.di

import android.content.Context
import com.example.crypt.domain.service.ErrorHandler
import com.example.crypt.domain.service.InputValidator
import com.example.crypt.domain.service.SecureClipboardManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing domain layer services.
 * Configures cross-cutting concerns like error handling, validation, and clipboard management.
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    /**
     * Provides the ErrorHandler for centralized error management.
     * Scoped as Singleton to maintain consistent error handling across the app.
     */
    @Provides
    @Singleton
    fun provideErrorHandler(): ErrorHandler {
        return ErrorHandler()
    }
    
    /**
     * Provides the InputValidator for input validation and sanitization.
     * Scoped as Singleton for consistent validation rules across the app.
     */
    @Provides
    @Singleton
    fun provideInputValidator(): InputValidator {
        return InputValidator()
    }
    
    /**
     * Provides the SecureClipboardManager for clipboard operations.
     * Scoped as Singleton to maintain clipboard state and auto-clear functionality.
     */
    @Provides
    @Singleton
    fun provideSecureClipboardManager(
        @ApplicationContext context: Context
    ): SecureClipboardManager {
        return SecureClipboardManager(context)
    }
}
