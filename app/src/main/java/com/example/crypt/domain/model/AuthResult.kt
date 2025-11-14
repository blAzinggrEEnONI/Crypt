package com.example.crypt.domain.model

/**
 * Represents the result of an authentication attempt.
 */
sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String, val errorCode: AuthErrorCode) : AuthResult()
    object BiometricUnavailable : AuthResult()
    object UserCancelled : AuthResult()
    object AuthenticationRequired : AuthResult()
}

/**
 * Error codes for authentication failures.
 */
enum class AuthErrorCode {
    BIOMETRIC_ERROR,
    PIN_INCORRECT,
    TOO_MANY_ATTEMPTS,
    HARDWARE_UNAVAILABLE,
    NO_BIOMETRIC_ENROLLED,
    UNKNOWN_ERROR
}