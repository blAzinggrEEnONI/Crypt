package com.example.crypt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypt.data.database.PasswordEntry
import com.example.crypt.data.repository.PasswordRepository
import com.example.crypt.domain.model.AuthResult
import com.example.crypt.domain.model.CryptError
import com.example.crypt.domain.service.CryptLogger
import com.example.crypt.domain.service.ErrorHandler
import com.example.crypt.domain.service.InputValidator
import com.example.crypt.domain.service.SecureClipboardManager
import com.example.crypt.domain.usecase.AuthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val authUseCase: AuthUseCase,
    private val secureClipboardManager: SecureClipboardManager,
    private val errorHandler: ErrorHandler,
    private val inputValidator: InputValidator
) : ViewModel() {
    
    companion object {
        private const val TAG = "EntryViewModel"
    }
    
    // Private mutable state
    private val _uiState = MutableStateFlow(EntryUiState())
    private val _editState = MutableStateFlow(EditEntryState())
    
    // Public read-only state
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()
    val editState: StateFlow<EditEntryState> = _editState.asStateFlow()
    
    // Current entry being viewed/edited
    private var currentEntry: PasswordEntry? = null
    
    /**
     * Load a password entry by ID for viewing.
     */
    fun loadEntry(entryId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val entry = passwordRepository.getEntryById(entryId)
                if (entry != null) {
                    currentEntry = entry
                    _uiState.value = _uiState.value.copy(
                        entry = entry,
                        isLoading = false,
                        isPasswordVisible = false,
                        decryptedPassword = null,
                        decryptedNotes = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Password entry not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load entry: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Authenticate and reveal the password for the current entry.
     */
    fun authenticateAndRevealPassword() {
        val entry = currentEntry ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAuthenticating = true)
            
            try {
                // Check if already authenticated
                if (!authUseCase.isAuthenticationRequired()) {
                    decryptAndShowPassword(entry)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticating = false,
                        authenticationRequired = true
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAuthenticating = false,
                    error = "Authentication failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Check if biometric authentication is available.
     */
    fun isBiometricAvailable(): Boolean {
        return authUseCase.isBiometricAvailable()
    }

    /**
     * Authenticate with biometrics.
     */
    suspend fun authenticateWithBiometrics(activity: androidx.fragment.app.FragmentActivity): AuthResult {
        return authUseCase.authenticateWithBiometrics(activity)
    }

    /**
     * Handle authentication result and decrypt password if successful.
     */
    fun handleAuthenticationResult(result: AuthResult) {
        val entry = currentEntry ?: return
        
        viewModelScope.launch {
            when (result) {
                is AuthResult.Success -> {
                    decryptAndShowPassword(entry)
                    _uiState.value = _uiState.value.copy(authenticationRequired = false)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticating = false,
                        authenticationRequired = false,
                        error = "Authentication failed: ${result.message}"
                    )
                }
                AuthResult.UserCancelled -> {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticating = false,
                        authenticationRequired = false
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticating = false,
                        authenticationRequired = false,
                        error = "Authentication unavailable"
                    )
                }
            }
        }
    }
    
    /**
     * Toggle password visibility (requires authentication first).
     */
    fun togglePasswordVisibility() {
        val currentState = _uiState.value
        if (currentState.decryptedPassword != null) {
            _uiState.value = currentState.copy(
                isPasswordVisible = !currentState.isPasswordVisible
            )
        } else {
            authenticateAndRevealPassword()
        }
    }
    
    /**
     * Copy password to clipboard (requires authentication first).
     */
    fun copyPasswordToClipboard() {
        val decrypted = _uiState.value.decryptedPassword
        if (decrypted != null) {
            secureClipboardManager.copyToClipboard("Password", decrypted, isSensitive = true)
        }
    }
    
    /**
     * Copy username to clipboard.
     */
    fun copyUsernameToClipboard() {
        currentEntry?.username?.let { username ->
            secureClipboardManager.copyToClipboard("Username", username, isSensitive = false)
        }
    }
    
    /**
     * Initialize edit mode with current entry data.
     */
    fun startEditing() {
        val entry = currentEntry ?: return
        
        _editState.value = EditEntryState(
            isEditing = true,
            isNewEntry = false,
            site = entry.site,
            username = entry.username,
            password = "", // Don't pre-fill password for security
            notes = "", // Will be decrypted when needed
            originalEntry = entry
        )
        
        // Decrypt notes if they exist
        if (!entry.notes.isNullOrEmpty()) {
            viewModelScope.launch {
                try {
                    val decryptedNotes = passwordRepository.decryptNotes(entry)
                    _editState.value = _editState.value.copy(notes = decryptedNotes ?: "")
                } catch (e: Exception) {
                    _editState.value = _editState.value.copy(
                        error = "Failed to decrypt notes: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Initialize create mode for new entry.
     */
    fun startCreating() {
        _editState.value = EditEntryState(
            isEditing = true,
            isNewEntry = true,
            site = "",
            username = "",
            password = "",
            notes = "",
            originalEntry = null
        )
    }
    
    /**
     * Update edit form field values.
     */
    fun updateEditField(field: EditField, value: String) {
        val currentState = _editState.value
        _editState.value = when (field) {
            EditField.SITE -> currentState.copy(site = value, hasUnsavedChanges = true)
            EditField.USERNAME -> currentState.copy(username = value, hasUnsavedChanges = true)
            EditField.PASSWORD -> currentState.copy(password = value, hasUnsavedChanges = true)
            EditField.NOTES -> currentState.copy(notes = value, hasUnsavedChanges = true)
        }
        
        // Clear validation errors when user starts typing
        clearFieldError(field)
    }
    
    /**
     * Validate edit form and return validation errors.
     */
    fun validateForm(): Map<EditField, String> {
        val state = _editState.value
        val errors = mutableMapOf<EditField, String>()
        
        if (state.site.isBlank()) {
            errors[EditField.SITE] = "Site name is required"
        }
        
        if (state.username.isBlank()) {
            errors[EditField.USERNAME] = "Username is required"
        }
        
        if (state.password.isBlank()) {
            errors[EditField.PASSWORD] = "Password is required"
        }
        
        _editState.value = state.copy(validationErrors = errors)
        return errors
    }
    
    /**
     * Save the current edit form (create or update).
     */
    fun saveEntry() {
        val state = _editState.value
        val validationErrors = validateForm()
        
        if (validationErrors.isNotEmpty()) {
            return
        }
        
        viewModelScope.launch {
            _editState.value = state.copy(isSaving = true, error = null)
            
            try {
                if (state.isNewEntry) {
                    // Create new entry
                    val entryId = passwordRepository.saveEntry(
                        site = state.site.trim(),
                        username = state.username.trim(),
                        password = state.password,
                        notes = state.notes.takeIf { it.isNotBlank() }
                    )
                    
                    _editState.value = state.copy(
                        isSaving = false,
                        saveSuccess = true,
                        savedEntryId = entryId
                    )
                } else {
                    // Update existing entry
                    val originalEntry = state.originalEntry!!
                    val updatedEntry = originalEntry.copy(
                        site = state.site.trim(),
                        username = state.username.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    passwordRepository.updateEntry(
                        entry = updatedEntry,
                        newPassword = state.password.takeIf { it.isNotBlank() },
                        newNotes = state.notes.takeIf { it.isNotBlank() }
                    )
                    
                    // Reload the entry to reflect changes
                    loadEntry(originalEntry.id)
                    
                    _editState.value = state.copy(
                        isSaving = false,
                        saveSuccess = true,
                        hasUnsavedChanges = false
                    )
                }
            } catch (e: Exception) {
                _editState.value = state.copy(
                    isSaving = false,
                    error = "Failed to save entry: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Cancel editing and discard changes.
     */
    fun cancelEditing() {
        _editState.value = EditEntryState()
    }
    
    /**
     * Delete the current entry.
     */
    fun deleteEntry() {
        val entry = currentEntry ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            
            try {
                passwordRepository.deleteEntry(entry)
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    deleteSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = "Failed to delete entry: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear any error messages.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        _editState.value = _editState.value.copy(error = null)
    }
    
    /**
     * Clear validation error for a specific field.
     */
    private fun clearFieldError(field: EditField) {
        val currentErrors = _editState.value.validationErrors.toMutableMap()
        currentErrors.remove(field)
        _editState.value = _editState.value.copy(validationErrors = currentErrors)
    }
    
    /**
     * Decrypt and show password for the given entry.
     */
    private suspend fun decryptAndShowPassword(entry: PasswordEntry) {
        try {
            val decryptedPassword = passwordRepository.decryptPassword(entry)
            val decryptedNotes = if (!entry.notes.isNullOrEmpty()) {
                passwordRepository.decryptNotes(entry)
            } else null
            
            _uiState.value = _uiState.value.copy(
                isAuthenticating = false,
                isPasswordVisible = true,
                decryptedPassword = decryptedPassword,
                decryptedNotes = decryptedNotes
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isAuthenticating = false,
                error = "Failed to decrypt password: ${e.message}"
            )
        }
    }
}

/**
 * UI state for entry viewing screen.
 */
data class EntryUiState(
    val entry: PasswordEntry? = null,
    val isLoading: Boolean = false,
    val isAuthenticating: Boolean = false,
    val isDeleting: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val decryptedPassword: String? = null,
    val decryptedNotes: String? = null,
    val authenticationRequired: Boolean = false,
    val deleteSuccess: Boolean = false,
    val error: String? = null
)

/**
 * UI state for entry editing screen.
 */
data class EditEntryState(
    val isEditing: Boolean = false,
    val isNewEntry: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val site: String = "",
    val username: String = "",
    val password: String = "",
    val notes: String = "",
    val originalEntry: PasswordEntry? = null,
    val savedEntryId: Long? = null,
    val validationErrors: Map<EditField, String> = emptyMap(),
    val error: String? = null
)

/**
 * Enum for edit form fields.
 */
enum class EditField {
    SITE,
    USERNAME,
    PASSWORD,
    NOTES
}