package com.example.crypt.ui.viewmodel

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypt.data.security.SecureMemoryManager
import com.example.crypt.domain.model.*
import com.example.crypt.domain.service.ErrorHandler
import com.example.crypt.domain.usecase.AuthUseCase
import com.example.crypt.domain.usecase.AutoLockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for handling authentication flow state management.
 * Integrates with AuthUseCase and AutoLockUseCase to manage authentication state
 * and navigation after successful authentication.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCase: AuthUseCase,
    private val autoLockUseCase: AutoLockUseCase,
    private val secureMemoryManager: SecureMemoryManager,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private val _biometricAvailable = MutableStateFlow(false)
    val biometricAvailable: StateFlow<Boolean> = _biometricAvailable.asStateFlow()

    private val _isPinSetup = MutableStateFlow(false)
    val isPinSetup: StateFlow<Boolean> = _isPinSetup.asStateFlow()

    init {
        // Initialize biometric availability and PIN setup status
        _biometricAvailable.value = authUseCase.isBiometricAvailable()
        _isPinSetup.value = authUseCase.isPinSetup()

        // Observe authentication state from AuthUseCase
        viewModelScope.launch {
            authUseCase.isAuthenticated.collect { isAuthenticated ->
                if (isAuthenticated) {
                    _authState.value = AuthState.Authenticated
                    // Start auto-lock timer when authenticated
                    autoLockUseCase.onAuthenticationSuccess()
                    // Navigate to main app
                    _navigationEvent.emit(NavigationEvent.NavigateToMain)
                } else {
                    // Only set to unauthenticated if we'''re not in an error state
                    if (_authState.value !is AuthState.AuthError) {
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            }
        }

        // Observe app lock state to handle auto-lock
        viewModelScope.launch {
            autoLockUseCase.observeAppState().collect { lockState ->
                when (lockState) {
                    is AppLockState.Locked -> {
                        // App was locked, reset auth state
                        if (_authState.value is AuthState.Authenticated) {
                            _authState.value = AuthState.Unauthenticated
                        }
                    }
                    is AppLockState.AutoLocking -> {
                        // App is about to lock, could show countdown UI
                        // For now, we'''ll handle this in the main app screens
                    }
                    is AppLockState.Unlocked -> {
                        // App is unlocked and active
                    }
                }
            }
        }
    }

    /**
     * Initiates biometric authentication with proper error handling and loading states.
     * @param activity The FragmentActivity required for BiometricPrompt
     */
    fun authenticateWithBiometrics(activity: FragmentActivity) {
        if (_loadingState.value.isLoading) {
            return
        }

        _authState.value = AuthState.Authenticating
        _loadingState.value = LoadingState.Loading(LoadingMessages.AUTHENTICATING)

        viewModelScope.launch {
            try {
                val result = authUseCase.authenticateWithBiometrics(activity)
                handleAuthResult(result)
            } catch (e: Exception) {
                val error = CryptError.fromException(e)
                errorHandler.handleError(error, "Biometric authentication")
                _authState.value = AuthState.AuthError(error.userMessage)
                _loadingState.value = LoadingState.Error(error)
            }
        }
    }

    /**
     * Initiates PIN authentication using secure string handling with comprehensive error handling.
     * @param pin The PIN entered by the user
     */
    fun authenticateWithPin(pin: String) {
        if (_loadingState.value.isLoading) {
            return
        }

        // Validate PIN format
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            val error = CryptError.ValidationError("PIN", "must be 4 digits")
            _authState.value = AuthState.AuthError(error.userMessage)
            _loadingState.value = LoadingState.Error(error)
            return
        }

        _authState.value = AuthState.Authenticating
        _loadingState.value = LoadingState.Loading(LoadingMessages.AUTHENTICATING)

        viewModelScope.launch {
            // Use secure string handling for PIN
            secureMemoryManager.createSecureString(pin).use { securePin ->
                try {
                    val result = authUseCase.authenticateWithPin(securePin.getValue())
                    handleAuthResult(result)
                } catch (e: Exception) {
                    val error = CryptError.fromException(e)
                    errorHandler.handleError(error, "PIN authentication")
                    _authState.value = AuthState.AuthError(error.userMessage)
                    _loadingState.value = LoadingState.Error(error)
                }
            }
        }
    }

    /**
     * Sets up a new PIN for the user using secure string handling with comprehensive error handling.
     * @param pin The new PIN to set up
     */
    fun setupPin(pin: String) {
        if (_loadingState.value.isLoading) {
            return
        }

        // Validate PIN format
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            val error = CryptError.ValidationError("PIN", "must be 4 digits")
            _authState.value = AuthState.AuthError(error.userMessage)
            _loadingState.value = LoadingState.Error(error)
            return
        }

        _authState.value = AuthState.Authenticating
        _loadingState.value = LoadingState.Loading(LoadingMessages.SETTING_UP_PIN)

        viewModelScope.launch {
            // Use secure string handling for PIN setup
            secureMemoryManager.createSecureString(pin).use { securePin ->
                try {
                    val success = authUseCase.setupPin(securePin.getValue())
                    if (success) {
                        _isPinSetup.value = true
                        _authState.value = AuthState.Unauthenticated
                        _loadingState.value = LoadingState.Success("PIN set up successfully")
                        _navigationEvent.emit(NavigationEvent.ShowMessage("PIN set up successfully"))
                    } else {
                        val error = CryptError.PinSetupFailed("Setup operation failed")
                        errorHandler.handleError(error, "PIN setup")
                        _authState.value = AuthState.AuthError(error.userMessage)
                        _loadingState.value = LoadingState.Error(error)
                    }
                } catch (e: Exception) {
                    val error = CryptError.fromException(e)
                    errorHandler.handleError(error, "PIN setup")
                    _authState.value = AuthState.AuthError(error.userMessage)
                    _loadingState.value = LoadingState.Error(error)
                }
            }
        }
    }

    /**
     * Checks if authentication is required.
     * @return true if user needs to authenticate, false if already authenticated
     */
    fun isAuthenticationRequired(): Boolean {
        return authUseCase.isAuthenticationRequired()
    }

    /**
     * Logs out the user and locks the app.
     */
    fun logout() {
        autoLockUseCase.lockApp()
        _authState.value = AuthState.Unauthenticated
    }

    /**
     * Clears any error state and resets to unauthenticated.
     */
    fun clearError() {
        if (_authState.value is AuthState.AuthError) {
            _authState.value = AuthState.Unauthenticated
        }
        if (_loadingState.value.isError) {
            _loadingState.value = LoadingState.Idle
        }
    }

    /**
     * Gets the current error for display purposes.
     */
    fun getCurrentError(): CryptError? {
        return _loadingState.value.errorOrNull
    }

    /**
     * Gets user-friendly error message with suggestions.
     */
    fun getErrorMessageWithSuggestion(): String? {
        return getCurrentError()?.let { error ->
            errorHandler.getUserFriendlyMessage(error)
        }
    }

    /**
     * Refreshes the biometric availability status.
     * Should be called when returning from settings or when biometric setup changes.
     */
    fun refreshBiometricAvailability() {
        _biometricAvailable.value = authUseCase.isBiometricAvailable()
    }

    /**
     * Handles the result of authentication attempts with proper error handling.
     */
    private suspend fun handleAuthResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                _loadingState.value = LoadingState.Success("Authentication successful")
                // Success is handled by the AuthUseCase isAuthenticated flow
                // The state will be updated automatically
            }
            is AuthResult.Error -> {
                val error = CryptError.PinAuthenticationFailed()
                errorHandler.handleError(error, "Authentication result")
                _authState.value = AuthState.AuthError(error.userMessage)
                _loadingState.value = LoadingState.Error(error)
            }
            is AuthResult.BiometricUnavailable -> {
                val error = CryptError.BiometricNotAvailable("Hardware not available")
                errorHandler.handleError(error, "Biometric check")
                _authState.value = AuthState.AuthError(error.userMessage)
                _loadingState.value = LoadingState.Error(error)
                _biometricAvailable.value = false
            }
            is AuthResult.UserCancelled -> {
                _authState.value = AuthState.Unauthenticated
                _loadingState.value = LoadingState.Idle
            }
            is AuthResult.AuthenticationRequired -> {
                _authState.value = AuthState.Unauthenticated
                _loadingState.value = LoadingState.Idle
            }
        }
    }

    /**
     * Called when user interacts with the app to reset auto-lock timer.
     */
    fun onUserInteraction() {
        autoLockUseCase.resetInactivityTimer()
    }

    /**
     * Gets the current app lock state.
     */
    fun getAppLockState(): StateFlow<AppLockState> {
        return autoLockUseCase.observeAppState()
    }

    override fun onCleared() {
        super.onCleared()
        // Request garbage collection to help clear sensitive data
        secureMemoryManager.requestGarbageCollection()
        // Cleanup is handled by AutoLockUseCase lifecycle
    }
}

/**
 * Navigation events emitted by the AuthViewModel.
 */
sealed class NavigationEvent {
    object NavigateToMain : NavigationEvent()
    data class ShowMessage(val message: String) : NavigationEvent()
}
