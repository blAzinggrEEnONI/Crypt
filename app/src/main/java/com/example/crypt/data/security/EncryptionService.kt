package com.example.crypt.data.security

import android.util.Base64
import android.util.Log
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for AES-256-GCM encryption and decryption operations.
 * Uses hardware-backed keys from KeystoreManager for secure data encryption.
 * Implements secure memory handling to protect sensitive data.
 */
@Singleton
class EncryptionService @Inject constructor(
    private val keystoreManager: KeystoreManager,
    private val secureMemoryManager: SecureMemoryManager
) {
    
    companion object {
        private const val TAG = "EncryptionService"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12 // 96 bits for GCM
        private const val TAG_LENGTH = 16 // 128 bits for authentication tag
        private const val GCM_TAG_LENGTH = 128 // bits
    }
    
    private val secureRandom = SecureRandom()
    
    /**
     * Encrypts plaintext using AES-256-GCM with hardware-backed key.
     * Implements secure memory handling to protect sensitive data.
     * @param plaintext The text to encrypt
     * @return Base64 encoded string containing IV + encrypted data + authentication tag
     * @throws SecurityException if encryption fails
     */
    suspend fun encrypt(plaintext: String): String {
        var plaintextBytes: ByteArray? = null
        var iv: ByteArray? = null
        var encryptedData: ByteArray? = null
        var combined: ByteArray? = null
        
        return try {
            Log.d(TAG, "Starting encryption...")
            val masterKey = keystoreManager.getMasterKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Generate random IV
            iv = ByteArray(IV_LENGTH)
            secureRandom.nextBytes(iv)
            
            // Initialize cipher for encryption
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, gcmParameterSpec)
            
            // Convert plaintext to bytes securely
            plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
            
            // Encrypt the plaintext
            encryptedData = cipher.doFinal(plaintextBytes)
            
            // Combine IV + encrypted data (which includes auth tag)
            combined = ByteArray(IV_LENGTH + encryptedData.size)
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH)
            System.arraycopy(encryptedData, 0, combined, IV_LENGTH, encryptedData.size)
            
            Log.d(TAG, "Encryption successful")
            // Return Base64 encoded result
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}", e)
            throw SecurityException("Encryption failed: ${e.message}", e)
        } finally {
            // Securely clear sensitive data from memory
            plaintextBytes?.let { secureMemoryManager.clearByteArray(it) }
            iv?.let { secureMemoryManager.clearByteArray(it) }
            encryptedData?.let { secureMemoryManager.clearByteArray(it) }
            combined?.let { secureMemoryManager.clearByteArray(it) }
            
            // Request garbage collection to help clear unreferenced data
            secureMemoryManager.requestGarbageCollection()
        }
    }
    
    /**
     * Decrypts Base64 encoded encrypted data using AES-256-GCM.
     * Implements secure memory handling to protect sensitive data.
     * @param encryptedData Base64 encoded string containing IV + encrypted data + auth tag
     * @return Decrypted plaintext string
     * @throws SecurityException if decryption fails or authentication fails
     */
    suspend fun decrypt(encryptedData: String): String {
        var combined: ByteArray? = null
        var iv: ByteArray? = null
        var encryptedBytes: ByteArray? = null
        var decryptedBytes: ByteArray? = null
        
        return try {
            Log.d(TAG, "Starting decryption...")
            val masterKey = keystoreManager.getMasterKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Decode Base64 data
            combined = Base64.decode(encryptedData, Base64.NO_WRAP)
            
            // Validate minimum length (IV + at least some encrypted data + auth tag)
            if (combined.size < IV_LENGTH + TAG_LENGTH) {
                throw SecurityException("Invalid encrypted data format")
            }
            
            // Extract IV
            iv = ByteArray(IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
            
            // Extract encrypted data (includes auth tag)
            encryptedBytes = ByteArray(combined.size - IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.size)
            
            // Initialize cipher for decryption
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, gcmParameterSpec)
            
            // Decrypt and verify authentication tag
            decryptedBytes = cipher.doFinal(encryptedBytes)
            
            Log.d(TAG, "Decryption successful")
            // Convert to string and return
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}", e)
            throw SecurityException("Decryption failed: ${e.message}", e)
        } finally {
            // Securely clear sensitive data from memory
            combined?.let { secureMemoryManager.clearByteArray(it) }
            iv?.let { secureMemoryManager.clearByteArray(it) }
            encryptedBytes?.let { secureMemoryManager.clearByteArray(it) }
            decryptedBytes?.let { secureMemoryManager.clearByteArray(it) }
            
            // Request garbage collection to help clear unreferenced data
            secureMemoryManager.requestGarbageCollection()
        }
    }
    
    /**
     * Initializes the keystore and verifies encryption capability.
     * @return true if initialization successful, false otherwise
     */
    suspend fun initializeKeystore(): Boolean {
        return try {
            // Check hardware availability
            val isHardwareBacked = keystoreManager.isHardwareBackedSecurityAvailable()
            
            // Get or generate master key
            keystoreManager.getMasterKey()
            
            // Test encryption/decryption roundtrip
            val testData = "test_encryption_${System.currentTimeMillis()}"
            val encrypted = encrypt(testData)
            val decrypted = decrypt(encrypted)
            
            // Verify roundtrip success
            testData == decrypted && isHardwareBacked
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generates a cryptographically secure random IV.
     * @return ByteArray of IV_LENGTH bytes
     */
    private fun generateIV(): ByteArray {
        val iv = ByteArray(IV_LENGTH)
        secureRandom.nextBytes(iv)
        return iv
    }
    
    /**
     * Verifies that the encrypted data format is valid.
     * @param encryptedData Base64 encoded encrypted data
     * @return true if format is valid, false otherwise
     */
    fun isValidEncryptedDataFormat(encryptedData: String): Boolean {
        return try {
            val decoded = Base64.decode(encryptedData, Base64.NO_WRAP)
            decoded.size >= IV_LENGTH + TAG_LENGTH
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Creates a secure string wrapper for sensitive data that automatically clears itself.
     * @param sensitiveData The sensitive string to wrap
     * @return SecureString wrapper that should be used with try-with-resources
     */
    fun createSecureString(sensitiveData: String): SecureString {
        return secureMemoryManager.createSecureString(sensitiveData)
    }
    
    /**
     * Encrypts data from a SecureString, automatically clearing the source.
     * @param secureString SecureString containing the data to encrypt
     * @return Base64 encoded encrypted string
     * @throws SecurityException if encryption fails
     */
    suspend fun encryptSecureString(secureString: SecureString): String {
        return secureString.use { secure ->
            encrypt(secure.getValue())
        }
    }
    
    /**
     * Decrypts data and returns it as a SecureString for safe handling.
     * @param encryptedData Base64 encoded encrypted data
     * @return SecureString containing the decrypted data
     * @throws SecurityException if decryption fails
     */
    suspend fun decryptToSecureString(encryptedData: String): SecureString {
        val decrypted = decrypt(encryptedData)
        return createSecureString(decrypted)
    }
}
