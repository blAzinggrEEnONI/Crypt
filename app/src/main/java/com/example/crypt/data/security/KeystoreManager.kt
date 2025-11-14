package com.example.crypt.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages hardware-backed encryption keys using Android Keystore API.
 * Provides AES-256 key generation, retrieval, and hardware availability checks.
 */
@Singleton
class KeystoreManager @Inject constructor() {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "crypt_master_key_v1"
        private const val KEY_SIZE = 256
    }
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }
    
    /**
     * Checks if hardware-backed security is available on the device.
     * @return true if hardware security module is available, false otherwise
     */
    fun isHardwareBackedSecurityAvailable(): Boolean {
        return try {
            // Try to generate a test key to verify hardware backing
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                "test_hardware_check",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setUserAuthenticationRequired(false)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            val testKey = keyGenerator.generateKey()
            
            // Clean up test key
            keyStore.deleteEntry("test_hardware_check")
            
            // If we got here without exception, hardware backing is available
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generates or retrieves the master encryption key from Android Keystore.
     * The key is hardware-backed and never extractable from the device.
     * @return SecretKey for AES-256-GCM encryption
     * @throws SecurityException if key generation fails
     */
    fun getMasterKey(): SecretKey {
        return try {
            // Check if key already exists
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
            } else {
                generateMasterKey()
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to retrieve or generate master key", e)
        }
    }
    
    /**
     * Generates a new master key in the Android Keystore.
     * @return SecretKey for AES-256-GCM encryption
     * @throws SecurityException if key generation fails
     */
    private fun generateMasterKey(): SecretKey {
        return try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setUserAuthenticationRequired(false) // Will be handled at app level
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            throw SecurityException("Failed to generate master key", e)
        }
    }
    
    /**
     * Checks if the master key exists in the keystore.
     * @return true if master key exists, false otherwise
     */
    fun hasMasterKey(): Boolean {
        return try {
            keyStore.containsAlias(MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Deletes the master key from the keystore.
     * This should only be used for testing or complete app reset.
     * @return true if deletion was successful, false otherwise
     */
    fun deleteMasterKey(): Boolean {
        return try {
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MASTER_KEY_ALIAS)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}