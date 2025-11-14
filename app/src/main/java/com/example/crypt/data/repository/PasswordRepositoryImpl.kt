package com.example.crypt.data.repository

import com.example.crypt.data.database.CryptDatabase
import com.example.crypt.data.database.PasswordEntry
import com.example.crypt.data.security.EncryptionService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PasswordRepository that handles encryption/decryption
 * and data transformation between domain and storage layers.
 */
@Singleton
class PasswordRepositoryImpl @Inject constructor(
    private val database: CryptDatabase,
    private val encryptionService: EncryptionService
) : PasswordRepository {
    
    private val passwordDao = database.passwordDao()
    
    override fun getAllEntries(): Flow<List<PasswordEntry>> {
        return passwordDao.getAllEntries()
    }
    
    override fun searchEntries(query: String): Flow<List<PasswordEntry>> {
        return passwordDao.searchEntries(query)
    }
    
    override suspend fun getEntryById(id: Long): PasswordEntry? {
        return passwordDao.getEntryById(id)
    }
    
    override suspend fun saveEntry(
        site: String,
        username: String,
        password: String,
        notes: String?
    ): Long {
        return try {
            // Encrypt the password
            val encryptedPassword = encryptionService.encrypt(password)
            
            // Encrypt notes if provided
            val encryptedNotes = notes?.let { encryptionService.encrypt(it) }
            
            // Create the entry
            val entry = PasswordEntry(
                site = site.trim(),
                username = username.trim(),
                encryptedPassword = encryptedPassword,
                notes = encryptedNotes,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Insert and return the ID
            passwordDao.insertEntry(entry)
        } catch (e: Exception) {
            throw SecurityException("Failed to save password entry", e)
        }
    }
    
    override suspend fun updateEntry(
        entry: PasswordEntry,
        newPassword: String?,
        newNotes: String?
    ) {
        try {
            var updatedEntry = entry.copy(updatedAt = System.currentTimeMillis())
            
            // Encrypt new password if provided
            if (newPassword != null) {
                val encryptedPassword = encryptionService.encrypt(newPassword)
                updatedEntry = updatedEntry.copy(encryptedPassword = encryptedPassword)
            }
            
            // Encrypt new notes if provided
            if (newNotes != null) {
                val encryptedNotes = if (newNotes.isBlank()) {
                    null
                } else {
                    encryptionService.encrypt(newNotes)
                }
                updatedEntry = updatedEntry.copy(notes = encryptedNotes)
            }
            
            passwordDao.updateEntry(updatedEntry)
        } catch (e: Exception) {
            throw SecurityException("Failed to update password entry", e)
        }
    }
    
    override suspend fun deleteEntry(entry: PasswordEntry) {
        try {
            passwordDao.deleteEntry(entry)
        } catch (e: Exception) {
            throw SecurityException("Failed to delete password entry", e)
        }
    }
    
    override suspend fun deleteEntryById(id: Long): Boolean {
        return try {
            val deletedCount = passwordDao.deleteEntryById(id)
            deletedCount > 0
        } catch (e: Exception) {
            throw SecurityException("Failed to delete password entry", e)
        }
    }
    
    override suspend fun decryptPassword(entry: PasswordEntry): String {
        return try {
            encryptionService.decrypt(entry.encryptedPassword)
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt password", e)
        }
    }
    
    override suspend fun decryptNotes(entry: PasswordEntry): String? {
        return try {
            entry.notes?.let { encryptedNotes ->
                encryptionService.decrypt(encryptedNotes)
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt notes", e)
        }
    }
    
    override suspend fun getEntryCount(): Int {
        return try {
            passwordDao.getEntryCount()
        } catch (e: Exception) {
            throw SecurityException("Failed to get entry count", e)
        }
    }
    
    override suspend fun getEntriesInTimeRange(startTime: Long, endTime: Long): List<PasswordEntry> {
        return try {
            passwordDao.getEntriesInTimeRange(startTime, endTime)
        } catch (e: Exception) {
            throw SecurityException("Failed to get entries in time range", e)
        }
    }
    
    /**
     * Validates that an entry has valid encrypted data format.
     * @param entry The password entry to validate
     * @return true if entry has valid encrypted data, false otherwise
     */
    private fun isValidEntry(entry: PasswordEntry): Boolean {
        return try {
            // Check if encrypted password format is valid
            val isPasswordValid = encryptionService.isValidEncryptedDataFormat(entry.encryptedPassword)
            
            // Check if encrypted notes format is valid (if notes exist)
            val isNotesValid = entry.notes?.let { 
                encryptionService.isValidEncryptedDataFormat(it) 
            } ?: true
            
            isPasswordValid && isNotesValid
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Performs a test encryption/decryption cycle to verify the encryption service.
     * This can be used for health checks or initialization validation.
     * @return true if encryption service is working correctly, false otherwise
     */
    suspend fun testEncryptionService(): Boolean {
        return try {
            val testData = "test_password_${System.currentTimeMillis()}"
            val encrypted = encryptionService.encrypt(testData)
            val decrypted = encryptionService.decrypt(encrypted)
            testData == decrypted
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clears sensitive data from memory.
     * This should be called when the app is locked or goes to background.
     */
    fun clearSensitiveData() {
        // In a real implementation, we would clear any cached decrypted data
        // For now, this is a placeholder for future memory security measures
        System.gc() // Suggest garbage collection to clear any lingering data
    }
}