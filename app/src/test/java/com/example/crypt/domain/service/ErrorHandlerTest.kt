package com.example.crypt.domain.service

import com.example.crypt.domain.model.CryptError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ErrorHandler service.
 */
class ErrorHandlerTest {
    
    private val errorHandler = ErrorHandler()
    
    @Test
    fun testHandleError_biometricNotAvailable() {
        val error = CryptError.BiometricNotAvailable("No sensor")
        
        assertTrue(error.isRecoverable)
        assertFalse(error.message.isEmpty())
        assertFalse(error.userMessage.isEmpty())
    }
    
    @Test
    fun testHandleError_pinAuthenticationFailed_lessThanMaxAttempts() {
        val error = CryptError.PinAuthenticationFailed(attempts = 1)
        
        assertTrue(error.isRecoverable)
        assertTrue(error.userMessage.contains("Incorrect PIN"))
    }
    
    @Test
    fun testHandleError_pinAuthenticationFailed_maxAttempts() {
        val error = CryptError.PinAuthenticationFailed(attempts = 5)
        
        assertFalse(error.isRecoverable)
        assertTrue(error.userMessage.contains("Too many"))
    }
    
    @Test
    fun testHandleError_decryptionFailed_notRecoverable() {
        val error = CryptError.DecryptionFailed("Corrupted data")
        
        assertFalse(error.isRecoverable)
        assertTrue(error.message.contains("Decryption failed"))
    }
    
    @Test
    fun testHandleError_keystoreUnavailable_notRecoverable() {
        val error = CryptError.KeystoreUnavailable
        
        assertFalse(error.isRecoverable)
        assertTrue(error.userMessage.contains("not available"))
    }
    
    @Test
    fun testHandleError_encryptionFailed_recoverable() {
        val error = CryptError.EncryptionFailed("Temporary error")
        
        assertTrue(error.isRecoverable)
        assertTrue(error.userMessage.contains("encrypt data"))
    }
    
    @Test
    fun testHandleError_unexpectedError() {
        val throwable = Exception("Test exception")
        val error = CryptError.UnexpectedError(throwable)
        
        assertTrue(error.isRecoverable)
        assertNotNull(error.throwable)
    }
    
    @Test
    fun testErrorFromException_securityException() {
        val exception = SecurityException("Encryption failed")
        val error = CryptError.fromException(exception)
        
        assertTrue(error is CryptError.EncryptionFailed || 
                   error is CryptError.UnexpectedError)
    }
    
    @Test
    fun testErrorFromException_illegalArgumentException() {
        val exception = IllegalArgumentException("Invalid input")
        val error = CryptError.fromException(exception)
        
        assertNotNull(error)
        assertTrue(error.isRecoverable)
    }
    
    @Test
    fun testErrorUserMessages_consistent() {
        val errors = listOf(
            CryptError.BiometricNotAvailable(),
            CryptError.PinNotSetup,
            CryptError.KeystoreUnavailable,
            CryptError.EncryptionFailed(),
            CryptError.DecryptionFailed()
        )
        
        for (error in errors) {
            assertFalse("UserMessage should not be empty", error.userMessage.isEmpty())
            assertFalse("Message should not be empty", error.message.isEmpty())
            assertTrue("UserMessage should be different from message", 
                      error.userMessage != error.message)
        }
    }
}
