package com.example.crypt.domain.model

/**
 * Represents different loading states for UI operations.
 * Provides consistent loading state management across the application.
 */
sealed class LoadingState {
    /**
     * Initial state - no operation has started.
     */
    object Idle : LoadingState()
    
    /**
     * Operation is in progress.
     * @param message Optional message to display to user
     * @param progress Optional progress percentage (0-100)
     */
    data class Loading(
        val message: String? = null,
        val progress: Int? = null
    ) : LoadingState()
    
    /**
     * Operation completed successfully.
     * @param message Optional success message
     */
    data class Success(
        val message: String? = null
    ) : LoadingState()
    
    /**
     * Operation failed with an error.
     * @param error The error that occurred
     */
    data class Error(
        val error: CryptError
    ) : LoadingState()
    
    /**
     * Check if currently loading.
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Check if in error state.
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Check if successful.
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Check if idle (no operation).
     */
    val isIdle: Boolean
        get() = this is Idle
    
    /**
     * Get error if in error state, null otherwise.
     */
    val errorOrNull: CryptError?
        get() = (this as? Error)?.error
    
    /**
     * Get loading message if in loading state, null otherwise.
     */
    val loadingMessage: String?
        get() = (this as? Loading)?.message
    
    /**
     * Get success message if in success state, null otherwise.
     */
    val successMessage: String?
        get() = (this as? Success)?.message
    
    /**
     * Get progress if in loading state with progress, null otherwise.
     */
    val loadingProgress: Int?
        get() = (this as? Loading)?.progress
}

/**
 * Common loading messages for different operations.
 */
object LoadingMessages {
    const val AUTHENTICATING = "Authenticating..."
    const val SETTING_UP_PIN = "Setting up PIN..."
    const val GENERATING_PASSWORD = "Generating password..."
    const val SAVING_PASSWORD = "Saving password..."
    const val LOADING_PASSWORDS = "Loading passwords..."
    const val ENCRYPTING_DATA = "Encrypting data..."
    const val DECRYPTING_DATA = "Decrypting data..."
    const val DELETING_PASSWORD = "Deleting password..."
    const val UPDATING_PASSWORD = "Updating password..."
    const val INITIALIZING_SECURITY = "Initializing security..."
    const val CHECKING_BIOMETRICS = "Checking biometric availability..."
    const val PREPARING_VAULT = "Preparing vault..."
}

/**
 * Extension functions for easier LoadingState handling.
 */
fun LoadingState.onLoading(action: (String?, Int?) -> Unit): LoadingState {
    if (this is LoadingState.Loading) {
        action(message, progress)
    }
    return this
}

fun LoadingState.onSuccess(action: (String?) -> Unit): LoadingState {
    if (this is LoadingState.Success) {
        action(message)
    }
    return this
}

fun LoadingState.onError(action: (CryptError) -> Unit): LoadingState {
    if (this is LoadingState.Error) {
        action(error)
    }
    return this
}

fun LoadingState.onIdle(action: () -> Unit): LoadingState {
    if (this is LoadingState.Idle) {
        action()
    }
    return this
}