package com.example.crypt.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for KeystoreManager.
 * These tests require a device/emulator to access Android Keystore.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreManagerTest {
    
    private lateinit var keystoreManager: KeystoreManager
    
    @Before
    fun setUp() {
        keystoreManager = KeystoreManager()
        // Clean up any existing test keys
        keystoreManager.deleteMasterKey()
    }
    
    @After
    fun tearDown() {
        // Clean up test keys
        keystoreManager.deleteMasterKey()
    }
    
    @Test
    fun isHardwareBackedSecurityAvailable_returnsBoolean() {
        // Test that the method returns a boolean value
        val isAvailable = keystoreManager.isHardwareBackedSecurityAvailable()
        // We can't assert true/false as it depends on device, but should not throw
        assertTrue("Should return a boolean", isAvailable is Boolean)
    }
    
    @Test
    fun getMasterKey_generatesKeyOnFirstCall() {
        // Verify no key exists initially
        assertFalse("Master key should not exist initially", keystoreManager.hasMasterKey())
        
        // Get master key (should generate new one)
        val masterKey = keystoreManager.getMasterKey()
        
        // Verify key was created
        assertNotNull("Master key should not be null", masterKey)
        assertTrue("Master key should exist after generation", keystoreManager.hasMasterKey())
        assertEquals("Key algorithm should be AES", "AES", masterKey.algorithm)
    }
    
    @Test
    fun getMasterKey_returnsSameKeyOnSubsequentCalls() {
        // Generate first key
        val firstKey = keystoreManager.getMasterKey()
        
        // Get key again
        val secondKey = keystoreManager.getMasterKey()
        
        // Verify they are the same key (same encoded form)
        assertArrayEquals(
            "Keys should be identical",
            firstKey.encoded,
            secondKey.encoded
        )
    }
    
    @Test
    fun hasMasterKey_returnsFalseInitially() {
        assertFalse("Should return false when no key exists", keystoreManager.hasMasterKey())
    }
    
    @Test
    fun hasMasterKey_returnsTrueAfterKeyGeneration() {
        // Generate key
        keystoreManager.getMasterKey()
        
        // Verify key exists
        assertTrue("Should return true after key generation", keystoreManager.hasMasterKey())
    }
    
    @Test
    fun deleteMasterKey_removesExistingKey() {
        // Generate key
        keystoreManager.getMasterKey()
        assertTrue("Key should exist", keystoreManager.hasMasterKey())
        
        // Delete key
        val deleted = keystoreManager.deleteMasterKey()
        
        // Verify deletion
        assertTrue("Delete operation should return true", deleted)
        assertFalse("Key should no longer exist", keystoreManager.hasMasterKey())
    }
    
    @Test
    fun deleteMasterKey_returnsFalseWhenNoKey() {
        // Try to delete non-existent key
        val deleted = keystoreManager.deleteMasterKey()
        
        // Should return false
        assertFalse("Delete should return false when no key exists", deleted)
    }
}