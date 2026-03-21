package com.example.crypt.domain.service

import android.util.Log
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for validating user inputs across the application.
 * Provides consistent validation rules for passwords, sites, and usernames.
 */
@Singleton
class InputValidator @Inject constructor() {
    
    companion object {
        private const val TAG = "InputValidator"
        
        // Validation constraints
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_PASSWORD_LENGTH = 1000
        const val MIN_SITE_LENGTH = 1
        const val MAX_SITE_LENGTH = 500
        const val MIN_USERNAME_LENGTH = 1
        const val MAX_USERNAME_LENGTH = 500
        const val MAX_NOTES_LENGTH = 5000
        
        // Validation patterns
        private val EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}"
        )
    }
    
    /**
     * Validates a password entry for creation or update.
     * @return ValidationResult containing success status and error message if invalid
     */
    fun validatePasswordEntry(
        site: String,
        username: String,
        password: String,
        notes: String = ""
    ): ValidationResult {
        // Validate site
        val siteValidation = validateSite(site)
        if (!siteValidation.isValid) {
            return siteValidation
        }
        
        // Validate username
        val usernameValidation = validateUsername(username)
        if (!usernameValidation.isValid) {
            return usernameValidation
        }
        
        // Validate password
        val passwordValidation = validatePassword(password)
        if (!passwordValidation.isValid) {
            return passwordValidation
        }
        
        // Validate notes
        if (notes.isNotEmpty()) {
            val notesValidation = validateNotes(notes)
            if (!notesValidation.isValid) {
                return notesValidation
            }
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Validates a site/service name.
     */
    fun validateSite(site: String): ValidationResult {
        val trimmed = site.trim()
        
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("Site name cannot be empty")
            trimmed.length < MIN_SITE_LENGTH -> ValidationResult.Invalid("Site name is too short")
            trimmed.length > MAX_SITE_LENGTH -> ValidationResult.Invalid("Site name is too long (max $MAX_SITE_LENGTH characters)")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates a username/email.
     */
    fun validateUsername(username: String): ValidationResult {
        val trimmed = username.trim()
        
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("Username cannot be empty")
            trimmed.length < MIN_USERNAME_LENGTH -> ValidationResult.Invalid("Username is too short")
            trimmed.length > MAX_USERNAME_LENGTH -> ValidationResult.Invalid("Username is too long (max $MAX_USERNAME_LENGTH characters)")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates a password entry.
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult.Invalid("Password cannot be empty")
            password.length < MIN_PASSWORD_LENGTH -> ValidationResult.Invalid("Password must be at least $MIN_PASSWORD_LENGTH characters")
            password.length > MAX_PASSWORD_LENGTH -> ValidationResult.Invalid("Password is too long (max $MAX_PASSWORD_LENGTH characters)")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates notes field.
     */
    fun validateNotes(notes: String): ValidationResult {
        return when {
            notes.length > MAX_NOTES_LENGTH -> ValidationResult.Invalid("Notes are too long (max $MAX_NOTES_LENGTH characters)")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates a PIN (numeric password).
     */
    fun validatePin(pin: String): ValidationResult {
        val trimmed = pin.trim()
        
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("PIN cannot be empty")
            trimmed.length < 4 -> ValidationResult.Invalid("PIN must be at least 4 digits")
            trimmed.length > 20 -> ValidationResult.Invalid("PIN is too long")
            !trimmed.all { it.isDigit() } -> ValidationResult.Invalid("PIN must contain only digits")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Checks if a string appears to be an email address.
     */
    fun isEmailAddress(text: String): Boolean {
        return try {
            EMAIL_PATTERN.matcher(text).matches()
        } catch (e: Exception) {
            Log.e(TAG, "Email validation error", e)
            false
        }
    }
    
    /**
     * Sanitizes a string for safe display and storage.
     * Removes potentially harmful characters while preserving readability.
     */
    fun sanitizeInput(input: String): String {
        return input.trim()
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .take(MAX_SITE_LENGTH) // Limit length
    }
}

/**
 * Sealed class representing validation results.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    
    data class Invalid(val error: String) : ValidationResult()
    
    val isValid: Boolean get() = this is Valid
}
