package com.example.crypt.domain.model

/**
 * Represents the current authentication state of the application.
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    object Authenticated : AuthState()
    data class AuthError(val message: String) : AuthState()
}