package com.example.crypt.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for EncryptionService.
 * These tests require a device/emulator to access Android Keystore.
 */
@RunWith(AndroidJUnit4::class)
class EncryptionServiceTest {
    
    private lateinit var keystoreManager: KeystoreManager
    private lateinit var encryptionService: EncryptionService
    
    @Before
    fun setUp() {
        keystoreManager = KeystoreManager()
        encryptionService = EncryptionService(keystoreManager)
        // Clean up any existing test keys
        keystoreManager.deleteMasterKey()
    }
    
    @After
    fun tearDown() {
        // Clean up test keys
        keystoreManager.deleteMasterKey()
    }
    
    @Test
    fun initializeKeystore_returnsTrue() = runBlocking {
        val initialized = encryptionService.initializeKeystore()
        assertTrue("Keystore initialization should succeed", initialized)
    }
    
    @Test
    fun encrypt_producesNonEmptyResult() = runBlocking {
        val plaintext = "Hello, World!"
        val encrypted = encryptionService.encrypt(plaintext)
        
        assertNotNull("Encrypted result should not be null", encrypted)
        assertTrue("Encrypted result should not be empty", encrypted.isNotEmpty())
        assertNotEquals("Encrypted result should differ from plaintext", plaintext, encrypted)
    }
    
    @Test
    fun encrypt_producesUniqueResults() = runBlocking {
        val plaintext = "Test message"
        
        val encrypted1 = encryptionService.encrypt(plaintext)
        val encrypted2 = encryptionService.encrypt(plaintext)
        
        assertNotEquals(
            "Multiple encryptions of same plaintext should produce different results (due to unique IVs)",
            encrypted1,
            encrypted2
        )
    }
    
    @Test
    fun decrypt_recoversOriginalPlaintext() = runBlocking {
        val originalText = "This is a secret message!"
        
        val encrypted = encryptionService.encrypt(originalText)
        val decrypted = encryptionService.decrypt(encrypted)
        
        assertEquals("Decrypted text should match original", originalText, decrypted)
    }
    
    @Test
    fun encryptDecrypt_roundtripWithVariousInputs() = runBlocking {
        val testCases = listOf(
            "",
            "a",
            "Hello, World!",
            "This is a longer message with special characters: !@#$%^&*()",
            "Unicode test: 🔒🔑💻",
            "Multi-line\ntext\nwith\nnewlines",
            "Very long text: " + "x".repeat(1000)
        )
        
        for (testCase in testCases) {
            val encrypted = encryptionService.encrypt(testCase)
            val decrypted = encryptionService.decrypt(encrypted)
            assertEquals("Roundtrip failed for: '$testCase'", testCase, decrypted)
        }
    }
    
    @Test
    fun decrypt_failsWithInvalidData() = runBlocking {
        val invalidInputs = listOf(
            "",
            "invalid",
            "not_base64!",
            "dGVzdA==", // Valid base64 but too short
        )
        
        for (invalidInput in invalidInputs) {
            try {
                encryptionService.decrypt(invalidInput)
                fail("Decryption should have failed for invalid input: '$invalidInput'")
            } catch (e: SecurityException) {
                // Expected behavior
                assertTrue("Should throw SecurityException", true)
            }
        }
    }
    
    @Test
    fun decrypt_failsWithTamperedData() = runBlocking {
        val plaintext = "Original message"
        val encrypted = encryptionService.encrypt(plaintext)
        
        // Tamper with the encrypted data by changing one character
        val tamperedEncrypted = encrypted.dropLast(1) + "X"
        
        try {
            encryptionService.decrypt(tamperedEncrypted)
            fail("Decryption should have failed with tampered data")
        } catch (e: SecurityException) {
            // Expected behavior - authentication should fail
            assertTrue("Should throw SecurityException for tampered data", true)
        }
    }
    
    @Test
    fun isValidEncryptedDataFormat_validatesCorrectly() = runBlocking {
        val plaintext = "Test message"
        val encrypted = encryptionService.encrypt(plaintext)
        
        assertTrue("Valid encrypted data should pass validation", 
            encryptionService.isValidEncryptedDataFormat(encrypted))
        
        assertFalse("Empty string should fail validation",
            encryptionService.isValidEncryptedDataFormat(""))
        
        assertFalse("Invalid base64 should fail validation",
            encryptionService.isValidEncryptedDataFormat("not_base64!"))
        
        assertFalse("Too short data should fail validation",
            encryptionService.isValidEncryptedDataFormat("dGVzdA=="))
    }
    
    @Test
    fun encrypt_handlesEmptyString() = runBlocking {
        val encrypted = encryptionService.encrypt("")
        val decrypted = encryptionService.decrypt(encrypted)
        
        assertEquals("Empty string roundtrip should work", "", decrypted)
    }
    
    @Test
    fun encrypt_handlesLargeData() = runBlocking {
        val largeText = "x".repeat(10000) // 10KB of data
        
        val encrypted = encryptionService.encrypt(largeText)
        val decrypted = encryptionService.decrypt(encrypted)
        
        assertEquals("Large data roundtrip should work", largeText, decrypted)
    }
}