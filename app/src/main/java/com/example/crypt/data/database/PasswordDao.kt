package com.example.crypt.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for password entries.
 * Provides reactive queries using Flow for UI updates.
 */
@Dao
interface PasswordDao {
    
    /**
     * Get all password entries ordered by site name.
     * Returns Flow for reactive UI updates.
     */
    @Query("SELECT * FROM password_entries ORDER BY site ASC")
    fun getAllEntries(): Flow<List<PasswordEntry>>
    
    /**
     * Get all password entries matching search query in site or username.
     * Case-insensitive search for better user experience.
     */
    @Query("""
        SELECT * FROM password_entries 
        WHERE site LIKE '%' || :query || '%' 
        OR username LIKE '%' || :query || '%' 
        ORDER BY site ASC
    """)
    fun searchEntries(query: String): Flow<List<PasswordEntry>>
    
    /**
     * Get a specific password entry by ID.
     * Returns null if entry doesn't exist.
     */
    @Query("SELECT * FROM password_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): PasswordEntry?
    
    /**
     * Insert a new password entry.
     * Returns the auto-generated ID of the inserted entry.
     */
    @Insert
    suspend fun insertEntry(entry: PasswordEntry): Long
    
    /**
     * Update an existing password entry.
     * Updates the updatedAt timestamp automatically via the entity.
     */
    @Update
    suspend fun updateEntry(entry: PasswordEntry)
    
    /**
     * Delete a password entry.
     */
    @Delete
    suspend fun deleteEntry(entry: PasswordEntry)
    
    /**
     * Delete a password entry by ID.
     * Returns the number of entries deleted (0 or 1).
     */
    @Query("DELETE FROM password_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long): Int
    
    /**
     * Get count of all password entries.
     * Useful for statistics and empty state handling.
     */
    @Query("SELECT COUNT(*) FROM password_entries")
    suspend fun getEntryCount(): Int
    
    /**
     * Get entries created within a specific time range.
     * Useful for backup and sync operations.
     */
    @Query("SELECT * FROM password_entries WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    suspend fun getEntriesInTimeRange(startTime: Long, endTime: Long): List<PasswordEntry>
}