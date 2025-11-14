package com.example.crypt.domain.model

/**
 * Sealed class representing different types of errors that can occur in the Crypt application.
 * Provides user-friendly error messages and categorization for proper error handling.
 */
sealed class CryptError(
    val message: String,
    val userMessage: String,
    val isRecoverable: Boolean = true
) {
    
    // Authentication Errors
    data class BiometricNotAvailable(
        val reason: String = "Biometric authentication is not available"
    ) : CryptError(
        message = "Biometric authentication unavailable: $reason",
        userMessage = "Biometric authentication is not available on this device. Please use PIN authentication.",
        isRecoverable = true
    )
    
    data class BiometricAuthenticationFailed(
        val reason: String = "Authentication failed"
    ) : CryptError(
        message = "Biometric authentication failed: $reason",
        userMessage = "Biometric authentication failed. Please try again or use your PIN.",
        isRecoverable = true
    )
    
    data class PinAuthenticationFailed(
        val attempts: Int = 0
    ) : CryptError(
        message = "PIN authentication failed after $attempts attempts",
        userMessage = if (attempts >= 3) {
            "Too many failed attempts. Please wait before trying again."
        } else {
            "Incorrect PIN. Please try again."
        },
        isRecoverable = attempts < 5
    )
    
    object PinNotSetup : CryptError(
        message = "PIN has not been set up",
        userMessage = "Please set up a PIN to secure your password vault.",
        isRecoverable = true
    )
    
    data class PinSetupFailed(
        val reason: String = "Unknown error"
    ) : CryptError(
        message = "PIN setup failed: $reason",
        userMessage = "Failed to set up PIN. Please try again.",
        isRecoverable = true
    )
    
    // Encryption Errors
    data class EncryptionFailed(
        val reason: String = "Unknown encryption error"
    ) : CryptError(
        message = "Encryption failed: $reason",
        userMessage = "Failed to encrypt data. Your password could not be saved securely.",
        isRecoverable = true
    )
    
    data class DecryptionFailed(
        val reason: String = "Unknown decryption error"
    ) : CryptError(
        message = "Decryption failed: $reason",
        userMessage = "Failed to decrypt password. The data may be corrupted.",
        isRecoverable = false
    )
    
    object KeystoreUnavailable : CryptError(
        message = "Android Keystore is unavailable",
        userMessage = "Secure storage is not available on this device. Your passwords cannot be protected.",
        isRecoverable = false
    )
    
    data class KeyGenerationFailed(
        val reason: String = "Unknown key generation error"
    ) : CryptError(
        message = "Key generation failed: $reason",
        userMessage = "Failed to generate encryption keys. Please restart the app.",
        isRecoverable = true
    )
    
    // Database Errors
    data class DatabaseError(
        val operation: String,
        val reason: String = "Unknown database error"
    ) : CryptError(
        message = "Database $operation failed: $reason",
        userMessage = "Failed to $operation password entry. Please try again.",
        isRecoverable = true
    )
    
    object DatabaseCorrupted : CryptError(
        message = "Database is corrupted",
        userMessage = "Your password vault appears to be corrupted. Please contact support.",
        isRecoverable = false
    )
    
    data class DatabaseMigrationFailed(
        val fromVersion: Int,
        val toVersion: Int
    ) : CryptError(
        message = "Database migration failed from version $fromVersion to $toVersion",
        userMessage = "Failed to update your password vault. Please restart the app.",
        isRecoverable = true
    )
    
    // Password Generation Errors
    data class PasswordGenerationFailed(
        val reason: String = "Unknown generation error"
    ) : CryptError(
        message = "Password generation failed: $reason",
        userMessage = "Failed to generate password. Please try again.",
        isRecoverable = true
    )
    
    data class InvalidPasswordConfiguration(
        val issue: String
    ) : CryptError(
        message = "Invalid password configuration: $issue",
        userMessage = "Invalid password settings: $issue",
        isRecoverable = true
    )
    
    // Network/Storage Errors (though app is offline)
    object StorageSpaceInsufficient : CryptError(
        message = "Insufficient storage space",
        userMessage = "Not enough storage space to save password. Please free up space.",
        isRecoverable = true
    )
    
    data class FileSystemError(
        val operation: String,
        val reason: String = "Unknown file system error"
    ) : CryptError(
        message = "File system $operation failed: $reason",
        userMessage = "Storage operation failed. Please check device storage.",
        isRecoverable = true
    )
    
    // General Application Errors
    data class UnexpectedError(
        val throwable: Throwable
    ) : CryptError(
        message = "Unexpected error: ${throwable.message}",
        userMessage = "An unexpected error occurred. Please restart the app.",
        isRecoverable = true
    )
    
    object AppLocked : CryptError(
        message = "Application is locked due to inactivity",
        userMessage = "App locked for security. Please authenticate to continue.",
        isRecoverable = true
    )
    
    data class ValidationError(
        val field: String,
        val issue: String
    ) : CryptError(
        message = "Validation failed for $field: $issue",
        userMessage = "$field: $issue",
        isRecoverable = true
    )
    
    // Auto-lock Errors
    object AutoLockServiceFailed : CryptError(
        message = "Auto-lock service failed to start",
        userMessage = "Security monitoring is not working properly. Please restart the app.",
        isRecoverable = true
    )
    
    /**
     * Converts a generic exception to a CryptError.
     */
    companion object {
        fun fromException(exception: Throwable): CryptError {
            return when (exception) {
                is SecurityException -> {
                    when {
                        exception.message?.contains("encryption", ignoreCase = true) == true ->
                            EncryptionFailed(exception.message ?: "Security exception")
                        exception.message?.contains("decryption", ignoreCase = true) == true ->
                            DecryptionFailed(exception.message ?: "Security exception")
                        exception.message?.contains("keystore", ignoreCase = true) == true ->
                            KeystoreUnavailable
                        else -> UnexpectedError(exception)
                    }
                }
                is IllegalStateException -> {
                    when {
                        exception.message?.contains("database", ignoreCase = true) == true ->
                            DatabaseError("operation", exception.message ?: "Illegal state")
                        exception.message?.contains("authentication", ignoreCase = true) == true ->
                            PinAuthenticationFailed()
                        else -> UnexpectedError(exception)
                    }
                }
                is IllegalArgumentException -> {
                    ValidationError("input", exception.message ?: "Invalid argument")
                }
                else -> UnexpectedError(exception)
            }
        }
        
        /**
         * Creates a user-friendly error message with recovery suggestions.
         */
        fun getErrorMessageWithSuggestion(error: CryptError): String {
            val suggestion = when (error) {
                is BiometricNotAvailable -> "Try using PIN authentication instead."
                is BiometricAuthenticationFailed -> "Make sure your biometric sensor is clean and try again."
                is PinAuthenticationFailed -> if (error.attempts >= 3) "Wait a few minutes before trying again." else "Double-check your PIN."
                is EncryptionFailed, is DecryptionFailed -> "Restart the app and try again."
                is KeystoreUnavailable -> "This device may not support secure storage."
                is DatabaseError -> "Check available storage space and try again."
                is DatabaseCorrupted -> "You may need to reset the app and lose your data."
                is PasswordGenerationFailed -> "Adjust your password settings and try again."
                is StorageSpaceInsufficient -> "Free up storage space on your device."
                else -> "Try restarting the app."
            }
            
            return "${error.userMessage}\n\n$suggestion"
        }
    }
}