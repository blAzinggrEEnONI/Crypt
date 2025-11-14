package com.example.crypt.domain.service

import android.util.Log
import com.example.crypt.domain.model.CryptError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling service for the Crypt application.
 * Provides consistent error logging, user notification, and recovery suggestions.
 */
@Singleton
class ErrorHandler @Inject constructor() {
    
    companion object {
        private const val TAG = "CryptErrorHandler"
    }
    
    private val _errorEvents = MutableSharedFlow<ErrorEvent>()
    val errorEvents: SharedFlow<ErrorEvent> = _errorEvents.asSharedFlow()
    
    /**
     * Handles an error by logging it and optionally notifying the user.
     * @param error The CryptError to handle
     * @param context Additional context about where the error occurred
     * @param notifyUser Whether to emit a user notification event
     */
    suspend fun handleError(
        error: CryptError,
        context: String = "",
        notifyUser: Boolean = true
    ) {
        // Log the error for debugging
        logError(error, context)
        
        // Emit error event for UI handling if requested
        if (notifyUser) {
            _errorEvents.emit(
                ErrorEvent(
                    error = error,
                    context = context,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Handles a generic exception by converting it to a CryptError.
     * @param exception The exception to handle
     * @param context Additional context about where the error occurred
     * @param notifyUser Whether to emit a user notification event
     */
    suspend fun handleException(
        exception: Throwable,
        context: String = "",
        notifyUser: Boolean = true
    ) {
        val cryptError = CryptError.fromException(exception)
        handleError(cryptError, context, notifyUser)
    }
    
    /**
     * Logs an error with appropriate log level based on severity.
     */
    private fun logError(error: CryptError, context: String) {
        val logMessage = buildString {
            append("CryptError: ${error.message}")
            if (context.isNotEmpty()) {
                append(" | Context: $context")
            }
            append(" | Recoverable: ${error.isRecoverable}")
        }
        
        when {
            !error.isRecoverable -> Log.e(TAG, logMessage)
            error is CryptError.UnexpectedError -> Log.w(TAG, logMessage, error.throwable)
            else -> Log.i(TAG, logMessage)
        }
    }
    
    /**
     * Gets a user-friendly error message with recovery suggestions.
     */
    fun getUserFriendlyMessage(error: CryptError): String {
        return CryptError.getErrorMessageWithSuggestion(error)
    }
    
    /**
     * Determines if an error should trigger an immediate app restart.
     */
    fun shouldRestartApp(error: CryptError): Boolean {
        return when (error) {
            is CryptError.KeystoreUnavailable,
            is CryptError.DatabaseCorrupted -> true
            else -> false
        }
    }
    
    /**
     * Determines if an error should trigger authentication re-prompt.
     */
    fun shouldReAuthenticate(error: CryptError): Boolean {
        return when (error) {
            is CryptError.AppLocked,
            is CryptError.DecryptionFailed -> true
            else -> false
        }
    }
    
    /**
     * Gets retry delay in milliseconds for recoverable errors.
     */
    fun getRetryDelay(error: CryptError, attemptCount: Int): Long {
        return when (error) {
            is CryptError.PinAuthenticationFailed -> {
                // Exponential backoff for PIN attempts
                minOf(1000L * (1L shl attemptCount), 30000L)
            }
            is CryptError.DatabaseError -> {
                // Short delay for database operations
                1000L
            }
            is CryptError.EncryptionFailed,
            is CryptError.PasswordGenerationFailed -> {
                // Immediate retry for these operations
                0L
            }
            else -> 2000L // Default 2 second delay
        }
    }
    
    /**
     * Checks if an error is worth retrying automatically.
     */
    fun shouldAutoRetry(error: CryptError, attemptCount: Int): Boolean {
        if (!error.isRecoverable || attemptCount >= 3) {
            return false
        }
        
        return when (error) {
            is CryptError.DatabaseError,
            is CryptError.EncryptionFailed,
            is CryptError.KeyGenerationFailed -> true
            is CryptError.PinAuthenticationFailed -> attemptCount < 1 // Only one auto-retry for PIN
            else -> false
        }
    }
}

/**
 * Represents an error event that occurred in the application.
 */
data class ErrorEvent(
    val error: CryptError,
    val context: String,
    val timestamp: Long
) {
    /**
     * Gets a formatted timestamp string.
     */
    val formattedTime: String
        get() = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
}