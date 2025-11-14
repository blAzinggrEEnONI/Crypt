package com.example.crypt.data.repository

import com.example.crypt.data.database.PasswordEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for password management operations.
 * Handles encryption/decryption and data transformation between domain and storage layers.
 */
interface PasswordRepository {
    
    /**
     * Get all password entries as a reactive Flow.
     * Passwords remain encrypted until explicitly decrypted.
     */
    fun getAllEntries(): Flow<List<PasswordEntry>>
    
    /**
     * Search password entries by site or username.
     * @param query Search query string
     * @return Flow of matching entries
     */
    fun searchEntries(query: String): Flow<List<PasswordEntry>>
    
    /**
     * Get a specific password entry by ID.
     * @param id Entry ID
     * @return PasswordEntry or null if not found
     */
    suspend fun getEntryById(id: Long): PasswordEntry?
    
    /**
     * Save a new password entry with encryption.
     * @param site Website or service name
     * @param username Username or email
     * @param password Plain text password (will be encrypted)
     * @param notes Optional notes (will be encrypted if provided)
     * @return ID of the created entry
     */
    suspend fun saveEntry(
        site: String,
        username: String,
        password: String,
        notes: String? = null
    ): Long
    
    /**
     * Update an existing password entry.
     * @param entry The entry to update
     * @param newPassword New password (if provided, will be encrypted)
     * @param newNotes New notes (if provided, will be encrypted)
     */
    suspend fun updateEntry(
        entry: PasswordEntry,
        newPassword: String? = null,
        newNotes: String? = null
    )
    
    /**
     * Delete a password entry.
     * @param entry The entry to delete
     */
    suspend fun deleteEntry(entry: PasswordEntry)
    
    /**
     * Delete a password entry by ID.
     * @param id Entry ID
     * @return true if entry was deleted, false if not found
     */
    suspend fun deleteEntryById(id: Long): Boolean
    
    /**
     * Decrypt a password from an entry.
     * @param entry The password entry
     * @return Decrypted password string
     * @throws SecurityException if decryption fails
     */
    suspend fun decryptPassword(entry: PasswordEntry): String
    
    /**
     * Decrypt notes from an entry.
     * @param entry The password entry
     * @return Decrypted notes string or null if no notes
     * @throws SecurityException if decryption fails
     */
    suspend fun decryptNotes(entry: PasswordEntry): String?
    
    /**
     * Get total count of password entries.
     * @return Number of stored entries
     */
    suspend fun getEntryCount(): Int
    
    /**
     * Get entries created within a specific time range.
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of entries in the time range
     */
    suspend fun getEntriesInTimeRange(startTime: Long, endTime: Long): List<PasswordEntry>
}