package com.example.crypt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypt.data.repository.PasswordRepository
import com.example.crypt.domain.model.PasswordGenerationConfig
import com.example.crypt.domain.service.CryptLogger
import com.example.crypt.domain.service.ErrorHandler
import com.example.crypt.domain.service.InputValidator
import com.example.crypt.domain.service.SecureClipboardManager
import com.example.crypt.domain.usecase.GeneratePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the password generation screen.
 * Manages password generation configuration, clipboard operations,
 * and saving generated passwords to the vault with validation.
 */
@HiltViewModel
class GenerateViewModel @Inject constructor(
    private val generatePasswordUseCase: GeneratePasswordUseCase,
    private val passwordRepository: PasswordRepository,
    private val secureClipboardManager: SecureClipboardManager,
    private val errorHandler: ErrorHandler,
    private val inputValidator: InputValidator
) : ViewModel() {
    
    companion object {
        private const val TAG = "GenerateViewModel"
    }
    
    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()
    
    init {
        // Generate an initial password when the screen loads
        generatePassword()
    }
    
    /**
     * Generates a new password based on current configuration.
     */
    fun generatePassword() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isGenerating = true,
                    message = null,
                    isError = false
                )
                
                val password = generatePasswordUseCase.execute(_uiState.value.config)
                
                _uiState.value = _uiState.value.copy(
                    generatedPassword = password,
                    isGenerating = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    message = "Failed to generate password: ${e.message}",
                    isError = true
                )
            }
        }
    }

    /**
     * Copy generated password to clipboard.
     */
    fun copyPasswordToClipboard() {
        val password = _uiState.value.generatedPassword
        if (password.isNotEmpty()) {
            secureClipboardManager.copyToClipboard("Generated Password", password)
        }
    }
    
    /**
     * Updates the password length configuration.
     */
    fun updateLength(length: Int) {
        val clampedLength = length.coerceIn(8, 64)
        val newConfig = _uiState.value.config.copy(length = clampedLength)
        
        try {
            // Validate the new configuration
            PasswordGenerationConfig(
                length = newConfig.length,
                includeUppercase = newConfig.includeUppercase,
                includeLowercase = newConfig.includeLowercase,
                includeNumbers = newConfig.includeNumbers,
                includeSymbols = newConfig.includeSymbols,
                excludeAmbiguous = newConfig.excludeAmbiguous
            )
            
            _uiState.value = _uiState.value.copy(
                config = newConfig,
                message = null,
                isError = false
            )
            
        } catch (e: IllegalArgumentException) {
            _uiState.value = _uiState.value.copy(
                message = e.message,
                isError = true
            )
        }
    }
    
    /**
     * Updates the include uppercase letters setting.
     */
    fun updateIncludeUppercase(include: Boolean) {
        updateCharacterSetOption { it.copy(includeUppercase = include) }
    }
    
    /**
     * Updates the include lowercase letters setting.
     */
    fun updateIncludeLowercase(include: Boolean) {
        updateCharacterSetOption { it.copy(includeLowercase = include) }
    }
    
    /**
     * Updates the include numbers setting.
     */
    fun updateIncludeNumbers(include: Boolean) {
        updateCharacterSetOption { it.copy(includeNumbers = include) }
    }
    
    /**
     * Updates the include symbols setting.
     */
    fun updateIncludeSymbols(include: Boolean) {
        updateCharacterSetOption { it.copy(includeSymbols = include) }
    }
    
    /**
     * Updates the exclude ambiguous characters setting.
     */
    fun updateExcludeAmbiguous(exclude: Boolean) {
        val newConfig = _uiState.value.config.copy(excludeAmbiguous = exclude)
        
        try {
            // Validate the new configuration
            PasswordGenerationConfig(
                length = newConfig.length,
                includeUppercase = newConfig.includeUppercase,
                includeLowercase = newConfig.includeLowercase,
                includeNumbers = newConfig.includeNumbers,
                includeSymbols = newConfig.includeSymbols,
                excludeAmbiguous = newConfig.excludeAmbiguous
            )
            
            _uiState.value = _uiState.value.copy(
                config = newConfig,
                message = null,
                isError = false
            )
            
        } catch (e: IllegalArgumentException) {
            _uiState.value = _uiState.value.copy(
                message = e.message,
                isError = true
            )
        }
    }
    
    /**
     * Saves the currently generated password to the vault with validation.
     */
    fun savePassword(site: String, username: String, notes: String) {
        val password = _uiState.value.generatedPassword
        
        if (password.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                message = "No password to save. Generate a password first.",
                isError = true
            )
            return
        }
        
        // Validate input before attempting to save
        val validation = inputValidator.validatePasswordEntry(site, username, password, notes)
        if (!validation.isValid) {
            val errorMsg = (validation as com.example.crypt.domain.service.ValidationResult.Invalid).error
            CryptLogger.w(TAG, "Validation failed: $errorMsg")
            _uiState.value = _uiState.value.copy(
                message = errorMsg,
                isError = true
            )
            return
        }
        
        viewModelScope.launch {
            try {
                CryptLogger.d(TAG, "Saving generated password for site: ${site.trim()}")
                _uiState.value = _uiState.value.copy(
                    isSaving = true,
                    message = null,
                    isError = false
                )
                
                val entryId = passwordRepository.saveEntry(
                    site = site.trim(),
                    username = username.trim(),
                    password = password,
                    notes = notes.trim().takeIf { it.isNotEmpty() }
                )
                
                CryptLogger.logDbOperation(TAG, "INSERT generated password", 1)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = "Password saved to vault successfully!",
                    isError = false
                )
                
                // Generate a new password after saving
                generatePassword()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = "Failed to save password: ${e.message}",
                    isError = true
                )
            }
        }
    }
    
    /**
     * Shows a message to the user (typically for clipboard operations).
     */
    fun showMessage(message: String, isError: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            message = message,
            isError = isError
        )
        
        // Clear the message after a delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // 3 seconds
            if (_uiState.value.message == message) {
                _uiState.value = _uiState.value.copy(message = null)
            }
        }
    }
    
    /**
     * Clears any displayed message.
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, isError = false)
    }
    
    /**
     * Resets the configuration to default values.
     */
    fun resetToDefaults() {
        _uiState.value = _uiState.value.copy(
            config = PasswordGenerationConfig(),
            message = "Configuration reset to defaults",
            isError = false
        )
        generatePassword()
    }
    
    /**
     * Gets the current character sets that would be used for generation.
     * Useful for displaying to the user what characters are available.
     */
    fun getCurrentCharacterSets(): Map<String, String> {
        return generatePasswordUseCase.getCharacterSets(_uiState.value.config)
    }
    
    /**
     * Helper function to update character set options with validation.
     */
    private fun updateCharacterSetOption(update: (PasswordGenerationConfig) -> PasswordGenerationConfig) {
        val newConfig = update(_uiState.value.config)
        
        try {
            // Validate the new configuration
            PasswordGenerationConfig(
                length = newConfig.length,
                includeUppercase = newConfig.includeUppercase,
                includeLowercase = newConfig.includeLowercase,
                includeNumbers = newConfig.includeNumbers,
                includeSymbols = newConfig.includeSymbols,
                excludeAmbiguous = newConfig.excludeAmbiguous
            )
            
            _uiState.value = _uiState.value.copy(
                config = newConfig,
                message = null,
                isError = false
            )
            
        } catch (e: IllegalArgumentException) {
            _uiState.value = _uiState.value.copy(
                message = e.message ?: "At least one character set must be enabled",
                isError = true
            )
        }
    }
}

/**
 * UI state for the password generation screen.
 */
data class GenerateUiState(
    val config: PasswordGenerationConfig = PasswordGenerationConfig(),
    val generatedPassword: String = "",
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
) {
    /**
     * Indicates if the current configuration is valid for password generation.
     */
    val isConfigurationValid: Boolean
        get() = config.includeUppercase || config.includeLowercase || 
                config.includeNumbers || config.includeSymbols
    
    /**
     * Gets a summary of the current configuration for display purposes.
     */
    val configurationSummary: String
        get() {
            val sets = mutableListOf<String>()
            if (config.includeUppercase) sets.add("A-Z")
            if (config.includeLowercase) sets.add("a-z")
            if (config.includeNumbers) sets.add("0-9")
            if (config.includeSymbols) sets.add("Symbols")
            
            val setsText = sets.joinToString(", ")
            val ambiguousText = if (config.excludeAmbiguous) " (no ambiguous)" else ""
            
            return "${config.length} chars: $setsText$ambiguousText"
        }
}