package com.example.crypt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypt.data.database.PasswordEntry
import com.example.crypt.data.repository.PasswordRepository
import com.example.crypt.domain.model.CryptError
import com.example.crypt.domain.model.LoadingState
import com.example.crypt.domain.model.LoadingMessages
import com.example.crypt.domain.service.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the password vault screen.
 * Manages password entry list state, search functionality, and user interactions.
 */
@HiltViewModel
class VaultViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {
    
    // Private mutable state
    private val _searchQuery = MutableStateFlow("")
    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Loading(LoadingMessages.LOADING_PASSWORDS))
    private val _deleteLoadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    
    // Public read-only state
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()
    val deleteLoadingState: StateFlow<LoadingState> = _deleteLoadingState.asStateFlow()
    
    // Combined UI state
    val uiState: StateFlow<VaultUiState> = combine(
        passwordRepository.getAllEntries(),
        _searchQuery,
        _loadingState
    ) { entries, query, loading ->
        VaultUiState(
            allEntries = entries,
            filteredEntries = filterEntries(entries, query),
            searchQuery = query,
            loadingState = loading
        )
    }.catch { throwable ->
        // Handle repository errors
        val error = CryptError.fromException(throwable)
        errorHandler.handleError(error, "Loading vault entries")
        _loadingState.value = LoadingState.Error(error)
        emit(
            VaultUiState(
                allEntries = emptyList(),
                filteredEntries = emptyList(),
                searchQuery = _searchQuery.value,
                loadingState = LoadingState.Error(error)
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultUiState(
            allEntries = emptyList(),
            filteredEntries = emptyList(),
            searchQuery = "",
            loadingState = LoadingState.Loading(LoadingMessages.LOADING_PASSWORDS)
        )
    )
    
    init {
        // Initialize loading state
        viewModelScope.launch {
            try {
                // Wait for initial data load
                passwordRepository.getAllEntries().first()
                _loadingState.value = LoadingState.Success()
            } catch (e: Exception) {
                val error = CryptError.fromException(e)
                errorHandler.handleError(error, "Vault initialization")
                _loadingState.value = LoadingState.Error(error)
            }
        }
    }
    
    /**
     * Update the search query and filter entries accordingly.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Clear the search query.
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    /**
     * Retry loading the vault data after an error.
     */
    fun retryLoading() {
        _loadingState.value = LoadingState.Loading(LoadingMessages.LOADING_PASSWORDS)
        
        viewModelScope.launch {
            try {
                // Force refresh by collecting from repository again
                passwordRepository.getAllEntries().first()
                _loadingState.value = LoadingState.Success()
            } catch (e: Exception) {
                val error = CryptError.fromException(e)
                errorHandler.handleError(error, "Vault reload")
                _loadingState.value = LoadingState.Error(error)
            }
        }
    }
    
    /**
     * Delete a password entry by ID with comprehensive error handling.
     */
    fun deleteEntry(entryId: Long) {
        if (_deleteLoadingState.value.isLoading) {
            return
        }
        
        _deleteLoadingState.value = LoadingState.Loading(LoadingMessages.DELETING_PASSWORD)
        
        viewModelScope.launch {
            try {
                val success = passwordRepository.deleteEntryById(entryId)
                if (success) {
                    _deleteLoadingState.value = LoadingState.Success("Password deleted successfully")
                } else {
                    val error = CryptError.DatabaseError("delete", "Entry not found")
                    errorHandler.handleError(error, "Delete entry")
                    _deleteLoadingState.value = LoadingState.Error(error)
                }
            } catch (e: Exception) {
                val error = CryptError.fromException(e)
                errorHandler.handleError(error, "Delete entry")
                _deleteLoadingState.value = LoadingState.Error(error)
            }
        }
    }
    
    /**
     * Clear delete operation state.
     */
    fun clearDeleteState() {
        _deleteLoadingState.value = LoadingState.Idle
    }
    
    /**
     * Get user-friendly error message for current error state.
     */
    fun getErrorMessage(): String? {
        return _loadingState.value.errorOrNull?.let { error ->
            errorHandler.getUserFriendlyMessage(error)
        }
    }
    
    /**
     * Get user-friendly error message for delete operation.
     */
    fun getDeleteErrorMessage(): String? {
        return _deleteLoadingState.value.errorOrNull?.let { error ->
            errorHandler.getUserFriendlyMessage(error)
        }
    }
    
    /**
     * Get entry count for display purposes.
     */
    fun getEntryCount(): Int {
        return uiState.value.allEntries.size
    }
    
    /**
     * Check if vault is empty.
     */
    fun isVaultEmpty(): Boolean {
        return uiState.value.allEntries.isEmpty() && !uiState.value.isLoading
    }
    
    /**
     * Filter password entries based on search query.
     * Searches in site name and username fields (case-insensitive).
     */
    private fun filterEntries(entries: List<PasswordEntry>, query: String): List<PasswordEntry> {
        if (query.isBlank()) {
            return entries.sortedBy { it.site.lowercase() }
        }
        
        val searchTerm = query.trim().lowercase()
        return entries.filter { entry ->
            entry.site.lowercase().contains(searchTerm) ||
            entry.username.lowercase().contains(searchTerm)
        }.sortedBy { entry ->
            // Sort by relevance: exact matches first, then partial matches
            when {
                entry.site.lowercase() == searchTerm -> 0
                entry.username.lowercase() == searchTerm -> 1
                entry.site.lowercase().startsWith(searchTerm) -> 2
                entry.username.lowercase().startsWith(searchTerm) -> 3
                else -> 4
            }
        }
    }
}

/**
 * UI state data class for the vault screen with comprehensive loading and error states.
 */
data class VaultUiState(
    val allEntries: List<PasswordEntry> = emptyList(),
    val filteredEntries: List<PasswordEntry> = emptyList(),
    val searchQuery: String = "",
    val loadingState: LoadingState = LoadingState.Loading(LoadingMessages.LOADING_PASSWORDS)
) {
    /**
     * Check if search is active.
     */
    val isSearching: Boolean
        get() = searchQuery.isNotBlank()
    
    /**
     * Check if currently loading.
     */
    val isLoading: Boolean
        get() = loadingState.isLoading
    
    /**
     * Check if in error state.
     */
    val hasError: Boolean
        get() = loadingState.isError
    
    /**
     * Get error if in error state.
     */
    val error: CryptError?
        get() = loadingState.errorOrNull
    
    /**
     * Check if there are no results for current search.
     */
    val hasNoSearchResults: Boolean
        get() = isSearching && filteredEntries.isEmpty() && !isLoading
    
    /**
     * Check if vault is completely empty.
     */
    val isVaultEmpty: Boolean
        get() = !isSearching && allEntries.isEmpty() && !isLoading
    
    /**
     * Get display message for current state.
     */
    val displayMessage: String?
        get() = when {
            hasError -> error?.userMessage
            isLoading -> loadingState.loadingMessage
            hasNoSearchResults -> "No entries found for \"$searchQuery\""
            isVaultEmpty -> "Your vault is empty"
            else -> null
        }
    
    /**
     * Get entry count text for display.
     */
    val entryCountText: String
        get() = when {
            isSearching -> "${filteredEntries.size} results"
            else -> "${allEntries.size} entries"
        }
}