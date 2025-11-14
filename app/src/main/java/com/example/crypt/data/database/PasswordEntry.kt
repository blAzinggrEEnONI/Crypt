package com.example.crypt.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a password entry in the encrypted database.
 * Passwords are stored encrypted while metadata (site, username) remains plaintext for search/display.
 */
@Entity(tableName = "password_entries")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Website or service name (plaintext for search and display)
     */
    val site: String,
    
    /**
     * Username or email address (plaintext for display)
     */
    val username: String,
    
    /**
     * AES-256-GCM encrypted password stored as Base64 string
     * Format: IV(12 bytes) + EncryptedData + AuthTag(16 bytes)
     */
    val encryptedPassword: String,
    
    /**
     * Optional encrypted notes field
     */
    val notes: String? = null,
    
    /**
     * Timestamp when the entry was created
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Timestamp when the entry was last updated
     */
    val updatedAt: Long = System.currentTimeMillis()
)