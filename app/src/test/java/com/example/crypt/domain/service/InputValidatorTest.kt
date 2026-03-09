package com.example.crypt.domain.service

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for InputValidator service.
 * Tests input validation rules and constraints.
 */
class InputValidatorTest {
    
    private lateinit var validator: InputValidator
    
    @Before
    fun setup() {
        validator = InputValidator()
    }
    
    // Site Validation Tests
    @Test
    fun testValidateSite_ValidInput() {
        val result = validator.validateSite("example.com")
        assertTrue(result.isValid)
    }
    
    @Test
    fun testValidateSite_EmptyInput() {
        val result = validator.validateSite("")
        assertFalse(result.isValid)
        assertTrue(result is ValidationResult.Invalid)
    }
    
    @Test
    fun testValidateSite_WithWhitespace() {
        val result = validator.validateSite("  example.com  ")
        assertTrue(result.isValid)
    }
    
    @Test
    fun testValidateSite_TooLong() {
        val longSite = "a".repeat(InputValidator.MAX_SITE_LENGTH + 1)
        val result = validator.validateSite(longSite)
        assertFalse(result.isValid)
    }
    
    // Username Validation Tests
    @Test
    fun testValidateUsername_ValidInput() {
        val result = validator.validateUsername("user@example.com")
        assertTrue(result.isValid)
    }
    
    @Test
    fun testValidateUsername_EmptyInput() {
        val result = validator.validateUsername("")
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidateUsername_TooLong() {
        val longUsername = "a".repeat(InputValidator.MAX_USERNAME_LENGTH + 1)
        val result = validator.validateUsername(longUsername)
        assertFalse(result.isValid)
    }
    
    // Password Validation Tests
    @Test
    fun testValidatePassword_ValidInput() {
        val result = validator.validatePassword("SecurePass123!")
        assertTrue(result.isValid)
    }
    
    @Test
    fun testValidatePassword_EmptyInput() {
        val result = validator.validatePassword("")
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidatePassword_TooShort() {
        val result = validator.validatePassword("short")
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidatePassword_MinimumLength() {
        val result = validator.validatePassword("a".repeat(InputValidator.MIN_PASSWORD_LENGTH))
        assertTrue(result.isValid)
    }
    
    @Test
    fun testValidatePassword_TooLong() {
        val longPassword = "a".repeat(InputValidator.MAX_PASSWORD_LENGTH + 1)
        val result = validator.validatePassword(longPassword)
        assertFalse(result.isValid)
    }
    
    // PIN Validation Tests
    @Test
    fun testValidatePin_ValidInput() {
        val result = validator.validatePin("1234")
        assertTrue(result.isValid)
    }
    
    @Test
    fun testValidatePin_EmptyInput() {
        val result = validator.validatePin("")
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidatePin_TooShort() {
        val result = validator.validatePin("123")
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidatePin_NonNumeric() {
        val result = validator.validatePin("12ab")
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidatePin_WithWhitespace() {
        val result = validator.validatePin("  1234  ")
        assertTrue(result.isValid)
    }
    
    // Email Detection Tests
    @Test
    fun testIsEmailAddress_ValidEmail() {
        assertTrue(validator.isEmailAddress("user@example.com"))
    }
    
    @Test
    fun testIsEmailAddress_InvalidEmail() {
        assertFalse(validator.isEmailAddress("not-an-email"))
    }
    
    @Test
    fun testIsEmailAddress_UsernameOnly() {
        assertFalse(validator.isEmailAddress("username"))
    }
    
    // Notes Validation Tests
    @Test
    fun testValidateNotes_ValidInput() {
        val result = validator.validateNotes("This is a note")
        assertTrue(result.isValid)
    }
    
    @Test
    fun testValidateNotes_EmptyInput() {
        val result = validator.validateNotes("")
        assertTrue(result.isValid)
    }
    
    @Test
    fun testValidateNotes_TooLong() {
        val longNotes = "a".repeat(InputValidator.MAX_NOTES_LENGTH + 1)
        val result = validator.validateNotes(longNotes)
        assertFalse(result.isValid)
    }
    
    // Full Entry Validation Tests
    @Test
    fun testValidatePasswordEntry_AllValid() {
        val result = validator.validatePasswordEntry(
            site = "example.com",
            username = "user@example.com",
            password = "SecurePassword123!",
            notes = "This is a test note"
        )
        assertTrue(result.isValid)
    }
    
    @Test
    fun testValidatePasswordEntry_InvalidSite() {
        val result = validator.validatePasswordEntry(
            site = "",
            username = "user@example.com",
            password = "SecurePassword123!",
            notes = "This is a test note"
        )
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidatePasswordEntry_InvalidPassword() {
        val result = validator.validatePasswordEntry(
            site = "example.com",
            username = "user@example.com",
            password = "short",
            notes = "This is a test note"
        )
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidatePasswordEntry_InvalidNotes() {
        val result = validator.validatePasswordEntry(
            site = "example.com",
            username = "user@example.com",
            password = "SecurePassword123!",
            notes = "a".repeat(InputValidator.MAX_NOTES_LENGTH + 1)
        )
        assertFalse(result.isValid)
    }
    
    // Sanitization Tests
    @Test
    fun testSanitizeInput_RemovesExtraWhitespace() {
        val result = validator.sanitizeInput("  hello   world  ")
        assertEquals("hello world", result)
    }
    
    @Test
    fun testSanitizeInput_TrimsInput() {
        val result = validator.sanitizeInput("  example.com  ")
        assertEquals("example.com", result)
    }
    
    @Test
    fun testSanitizeInput_RespectMaxLength() {
        val longInput = "a".repeat(InputValidator.MAX_SITE_LENGTH + 100)
        val result = validator.sanitizeInput(longInput)
        assertTrue(result.length <= InputValidator.MAX_SITE_LENGTH)
    }
}
