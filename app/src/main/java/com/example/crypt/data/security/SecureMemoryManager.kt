package com.example.crypt.data.security

import android.app.Activity
import android.view.WindowManager
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages secure memory operations for sensitive data handling.
 * Provides utilities for clearing sensitive data from memory and protecting against memory dumps.
 */
@Singleton
class SecureMemoryManager @Inject constructor() {
    
    companion object {
        private const val SECURE_FLAG = WindowManager.LayoutParams.FLAG_SECURE
    }
    
    /**
     * Securely clears a CharArray by overwriting with random data.
     * @param data CharArray to clear
     */
    fun clearCharArray(data: CharArray) {
        if (data.isNotEmpty()) {
            // Overwrite with random characters multiple times
            val random = Random()
            repeat(3) {
                for (i in data.indices) {
                    data[i] = random.nextInt(65536).toChar()
                }
            }
            // Final pass with zeros
            Arrays.fill(data, '\u0000')
        }
    }
    
    /**
     * Securely clears a ByteArray by overwriting with random data.
     * @param data ByteArray to clear
     */
    fun clearByteArray(data: ByteArray) {
        if (data.isNotEmpty()) {
            // Overwrite with random bytes multiple times
            val random = Random()
            repeat(3) {
                random.nextBytes(data)
            }
            // Final pass with zeros
            Arrays.fill(data, 0.toByte())
        }
    }
    
    /**
     * Creates a secure copy of a string as CharArray for processing.
     * The returned CharArray should be cleared after use with clearCharArray().
     * @param input String to convert
     * @return CharArray copy of the string
     */
    fun createSecureCharArray(input: String): CharArray {
        return input.toCharArray()
    }
    
    /**
     * Securely converts CharArray to String and immediately clears the CharArray.
     * @param data CharArray to convert
     * @return String representation
     */
    fun secureCharArrayToString(data: CharArray): String {
        val result = String(data)
        clearCharArray(data)
        return result
    }
    
    /**
     * Enables memory dump protection for an Activity by setting FLAG_SECURE.
     * This prevents screenshots and screen recording of sensitive content.
     * @param activity Activity to protect
     */
    fun enableMemoryDumpProtection(activity: Activity) {
        try {
            activity.window?.setFlags(SECURE_FLAG, SECURE_FLAG)
        } catch (e: Exception) {
            // Log but don't crash - some devices may not support this
            android.util.Log.w("SecureMemoryManager", "Failed to set FLAG_SECURE", e)
        }
    }
    
    /**
     * Disables memory dump protection for an Activity.
     * @param activity Activity to unprotect
     */
    fun disableMemoryDumpProtection(activity: Activity) {
        try {
            activity.window?.clearFlags(SECURE_FLAG)
        } catch (e: Exception) {
            // Log but don't crash
            android.util.Log.w("SecureMemoryManager", "Failed to clear FLAG_SECURE", e)
        }
    }
    
    /**
     * Performs garbage collection to help clear unreferenced sensitive data.
     * This is a hint to the JVM and may not immediately clear memory.
     */
    fun requestGarbageCollection() {
        System.gc()
        System.runFinalization()
        System.gc()
    }
    
    /**
     * Creates a secure wrapper for sensitive string data that automatically clears itself.
     * @param sensitiveData The sensitive string to wrap
     * @return SecureString wrapper
     */
    fun createSecureString(sensitiveData: String): SecureString {
        return SecureString(sensitiveData)
    }
}

/**
 * A wrapper class for sensitive string data that automatically clears itself from memory.
 * Implements AutoCloseable for use with try-with-resources pattern.
 */
class SecureString(sensitiveData: String) : AutoCloseable {
    private var data: CharArray? = sensitiveData.toCharArray()
    private var isCleared = false
    
    /**
     * Gets the sensitive data as a string.
     * @return The sensitive string data
     * @throws IllegalStateException if the data has been cleared
     */
    fun getValue(): String {
        if (isCleared || data == null) {
            throw IllegalStateException("Secure string has been cleared")
        }
        return String(data!!)
    }
    
    /**
     * Gets the sensitive data as a CharArray.
     * The returned array is a copy and should be cleared after use.
     * @return Copy of the sensitive data as CharArray
     * @throws IllegalStateException if the data has been cleared
     */
    fun getCharArray(): CharArray {
        if (isCleared || data == null) {
            throw IllegalStateException("Secure string has been cleared")
        }
        return data!!.copyOf()
    }
    
    /**
     * Checks if the secure string has been cleared.
     * @return true if cleared, false otherwise
     */
    fun isCleared(): Boolean {
        return isCleared
    }
    
    /**
     * Manually clears the sensitive data from memory.
     */
    fun clear() {
        data?.let { charArray ->
            // Overwrite with random data multiple times
            val random = Random()
            repeat(3) {
                for (i in charArray.indices) {
                    charArray[i] = random.nextInt(65536).toChar()
                }
            }
            // Final pass with zeros
            Arrays.fill(charArray, '\u0000')
        }
        data = null
        isCleared = true
    }
    
    /**
     * AutoCloseable implementation - clears data when closed.
     */
    override fun close() {
        clear()
    }
    
    /**
     * Finalizer to ensure data is cleared even if close() is not called.
     */
    protected fun finalize() {
        if (!isCleared) {
            clear()
        }
    }
    
    override fun toString(): String {
        return if (isCleared) "[CLEARED]" else "[SECURE_STRING]"
    }
}