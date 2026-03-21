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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        // IMPORTANT: Move heavy initialization to background thread to prevent Main Thread blocking
        // and allow the keyboard (IME) to function correctly.
        viewModelScope.launch {
            val bioAvailable = withContext(Dispatchers.IO) { authUseCase.isBiometricAvailable() }
            val pinSetup = withContext(Dispatchers.IO) { authUseCase.isPinSetup() }
            
            _biometricAvailable.value = bioAvailable
            _isPinSetup.value = pinSetup
        }

        // Observe authentication state from AuthUseCase
        viewModelScope.launch {
            authUseCase.isAuthenticated.collect { isAuthenticated ->
                if (isAuthenticated) {
                    _authState.value = AuthState.Authenticated
                    autoLockUseCase.onAuthenticationSuccess()
                    _navigationEvent.emit(NavigationEvent.NavigateToMain)
                } else {
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
                        if (_authState.value is AuthState.Authenticated) {
                            _authState.value = AuthState.Unauthenticated
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Initiates biometric authentication with proper error handling and loading states.
     */
    fun authenticateWithBiometrics(activity: FragmentActivity) {
        if (_loadingState.value.isLoading) return

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
     * Initiates PIN authentication using secure string handling.
     */
    fun authenticateWithPin(pin: String) {
        if (_loadingState.value.isLoading) return

        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            val error = CryptError.ValidationError("PIN", "must be 4 digits")
            _authState.value = AuthState.AuthError(error.userMessage)
            _loadingState.value = LoadingState.Error(error)
            return
        }

        _authState.value = AuthState.Authenticating
        _loadingState.value = LoadingState.Loading(LoadingMessages.AUTHENTICATING)

        viewModelScope.launch {
            secureMemoryManager.createSecureString(pin).use { securePin ->
                try {
                    val result = withContext(Dispatchers.IO) {
                        authUseCase.authenticateWithPin(securePin.getValue())
                    }
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
     * Sets up a new PIN for the user.
     */
    fun setupPin(pin: String) {
        if (_loadingState.value.isLoading) return

        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            val error = CryptError.ValidationError("PIN", "must be 4 digits")
            _authState.value = AuthState.AuthError(error.userMessage)
            _loadingState.value = LoadingState.Error(error)
            return
        }

        _authState.value = AuthState.Authenticating
        _loadingState.value = LoadingState.Loading(LoadingMessages.SETTING_UP_PIN)

        viewModelScope.launch {
            secureMemoryManager.createSecureString(pin).use { securePin ->
                try {
                    val success = withContext(Dispatchers.IO) {
                        authUseCase.setupPin(securePin.getValue())
                    }
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

    fun isAuthenticationRequired(): Boolean {
        return authUseCase.isAuthenticationRequired()
    }

    fun logout() {
        autoLockUseCase.lockApp()
        _authState.value = AuthState.Unauthenticated
    }

    fun clearError() {
        if (_authState.value is AuthState.AuthError) {
            _authState.value = AuthState.Unauthenticated
        }
        if (_loadingState.value.isError) {
            _loadingState.value = LoadingState.Idle
        }
    }

    fun refreshBiometricAvailability() {
        viewModelScope.launch {
            _biometricAvailable.value = withContext(Dispatchers.IO) { authUseCase.isBiometricAvailable() }
        }
    }

    private suspend fun handleAuthResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                _loadingState.value = LoadingState.Success("Authentication successful")
            }
            is AuthResult.Error -> {
                val error = CryptError.PinAuthenticationFailed()
                _authState.value = AuthState.AuthError(error.userMessage)
                _loadingState.value = LoadingState.Error(error)
            }
            is AuthResult.BiometricUnavailable -> {
                _biometricAvailable.value = false
                _authState.value = AuthState.AuthError("Biometrics unavailable")
                _loadingState.value = LoadingState.Idle
            }
            else -> {
                _authState.value = AuthState.Unauthenticated
                _loadingState.value = LoadingState.Idle
            }
        }
    }

    fun onUserInteraction() {
        autoLockUseCase.resetInactivityTimer()
    }

    fun getAppLockState(): StateFlow<AppLockState> = autoLockUseCase.observeAppState()

    override fun onCleared() {
        super.onCleared()
        secureMemoryManager.requestGarbageCollection()
    }
}

/**
 * Navigation events emitted by the AuthViewModel.
 */
sealed class NavigationEvent {
    object NavigateToMain : NavigationEvent()
    data class ShowMessage(val message: String) : NavigationEvent()
}
