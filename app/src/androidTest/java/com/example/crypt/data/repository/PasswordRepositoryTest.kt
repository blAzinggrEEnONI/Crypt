package com.example.crypt.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.crypt.data.database.CryptDatabase
import com.example.crypt.data.security.EncryptionService
import com.example.crypt.data.security.KeystoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.sqlcipher.database.SupportFactory
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/**
 * Integration tests for PasswordRepository with encrypted database.
 * Tests repository operations with encryption/decryption.
 */
@RunWith(AndroidJUnit4::class)
class PasswordRepositoryTest {
    
    private lateinit var database: CryptDatabase
    private lateinit var repository: PasswordRepository
    private lateinit var keystoreManager: KeystoreManager
    private lateinit var encryptionService: EncryptionService
    
    @Before
    fun setup() {
        keystoreManager = KeystoreManager()
        encryptionService = EncryptionService(keystoreManager)
        
        val testKey = createTestDatabaseKey()
        val supportFactory = SupportFactory(testKey)
        
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CryptDatabase::class.java
        )
            .openHelperFactory(supportFactory)
            .allowMainThreadQueries()
            .build()
        
        repository = PasswordRepositoryImpl(database, encryptionService)
    }    
   
 @After
    fun teardown() {
        database.close()
        keystoreManager.deleteMasterKey()
    }
    
    @Test
    fun saveAndRetrieveEntry() = runTest {
        // Given
        val site = "example.com"
        val username = "testuser"
        val password = "securepassword123"
        val notes = "Important account notes"
        
        // When
        val entryId = repository.saveEntry(site, username, password, notes)
        val retrievedEntry = repository.getEntryById(entryId)
        
        // Then
        assertNotNull(retrievedEntry)
        assertEquals(site, retrievedEntry!!.site)
        assertEquals(username, retrievedEntry.username)
        
        // Verify password decryption
        val decryptedPassword = repository.decryptPassword(retrievedEntry)
        assertEquals(password, decryptedPassword)
        
        // Verify notes decryption
        val decryptedNotes = repository.decryptNotes(retrievedEntry)
        assertEquals(notes, decryptedNotes)
    }
    
    @Test
    fun updateEntryPassword() = runTest {
        // Given
        val entryId = repository.saveEntry("test.com", "user", "oldpass", "notes")
        val entry = repository.getEntryById(entryId)!!
        val newPassword = "newpassword123"
        
        // When
        repository.updateEntry(entry, newPassword = newPassword)
        val updatedEntry = repository.getEntryById(entryId)!!
        
        // Then
        val decryptedPassword = repository.decryptPassword(updatedEntry)
        assertEquals(newPassword, decryptedPassword)
        assertTrue(updatedEntry.updatedAt > entry.updatedAt)
    }
    
    @Test
    fun searchEntries() = runTest {
        // Given
        repository.saveEntry("google.com", "user@gmail.com", "pass1")
        repository.saveEntry("facebook.com", "user@fb.com", "pass2")
        repository.saveEntry("twitter.com", "twitteruser", "pass3")
        
        // When
        val googleResults = repository.searchEntries("google").first()
        val gmailResults = repository.searchEntries("gmail").first()
        
        // Then
        assertEquals(1, googleResults.size)
        assertEquals("google.com", googleResults[0].site)
        
        assertEquals(1, gmailResults.size)
        assertEquals("user@gmail.com", gmailResults[0].username)
    }
    
    private fun createTestDatabaseKey(): ByteArray {
        val testSalt = "TestRepositorySalt2024"
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(testSalt.toByteArray())
        return digest.digest()
    }
}