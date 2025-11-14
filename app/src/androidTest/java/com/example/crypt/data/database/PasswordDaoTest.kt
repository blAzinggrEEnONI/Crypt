package com.example.crypt.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.crypt.data.security.EncryptionService
import com.example.crypt.data.security.KeystoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/**
 * Integration tests for PasswordDao with encrypted database.
 * Tests DAO operations with SQLCipher encryption.
 */
@RunWith(AndroidJUnit4::class)
class PasswordDaoTest {
    
    private lateinit var database: CryptDatabase
    private lateinit var passwordDao: PasswordDao
    private lateinit var keystoreManager: KeystoreManager
    private lateinit var encryptionService: EncryptionService
    
    @Before
    fun setup() {
        // Initialize keystore manager and encryption service
        keystoreManager = KeystoreManager()
        encryptionService = EncryptionService(keystoreManager)
        
        // Create test database key
        val testKey = createTestDatabaseKey()
        val supportFactory = SupportFactory(testKey)
        
        // Create in-memory encrypted database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CryptDatabase::class.java
        )
            .openHelperFactory(supportFactory)
            .allowMainThreadQueries() // Only for testing
            .build()
        
        passwordDao = database.passwordDao()
    }
    
    @After
    fun teardown() {
        database.close()
        // Clean up test keys
        keystoreManager.deleteMasterKey()
    }
    
    @Test
    fun insertAndRetrieveEntry() = runTest {
        // Given
        val testEntry = PasswordEntry(
            site = "example.com",
            username = "testuser",
            encryptedPassword = encryptionService.encrypt("testpassword"),
            notes = encryptionService.encrypt("test notes"),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // When
        val insertedId = passwordDao.insertEntry(testEntry)
        val retrievedEntry = passwordDao.getEntryById(insertedId)
        
        // Then
        assertNotNull(retrievedEntry)
        assertEquals(testEntry.site, retrievedEntry!!.site)
        assertEquals(testEntry.username, retrievedEntry.username)
        assertEquals(testEntry.encryptedPassword, retrievedEntry.encryptedPassword)
        assertEquals(testEntry.notes, retrievedEntry.notes)
    }
    
    @Test
    fun getAllEntriesFlow() = runTest {
        // Given
        val entries = listOf(
            PasswordEntry(
                site = "site1.com",
                username = "user1",
                encryptedPassword = encryptionService.encrypt("password1")
            ),
            PasswordEntry(
                site = "site2.com",
                username = "user2",
                encryptedPassword = encryptionService.encrypt("password2")
            )
        )
        
        // When
        entries.forEach { passwordDao.insertEntry(it) }
        val allEntries = passwordDao.getAllEntries().first()
        
        // Then
        assertEquals(2, allEntries.size)
        assertEquals("site1.com", allEntries[0].site)
        assertEquals("site2.com", allEntries[1].site)
    }
    
    @Test
    fun searchEntries() = runTest {
        // Given
        val entries = listOf(
            PasswordEntry(
                site = "google.com",
                username = "user@gmail.com",
                encryptedPassword = encryptionService.encrypt("password1")
            ),
            PasswordEntry(
                site = "facebook.com",
                username = "user@facebook.com",
                encryptedPassword = encryptionService.encrypt("password2")
            ),
            PasswordEntry(
                site = "twitter.com",
                username = "twitteruser",
                encryptedPassword = encryptionService.encrypt("password3")
            )
        )
        
        // When
        entries.forEach { passwordDao.insertEntry(it) }
        
        // Search by site
        val googleResults = passwordDao.searchEntries("google").first()
        val facebookResults = passwordDao.searchEntries("facebook").first()
        
        // Search by username
        val gmailResults = passwordDao.searchEntries("gmail").first()
        
        // Then
        assertEquals(1, googleResults.size)
        assertEquals("google.com", googleResults[0].site)
        
        assertEquals(1, facebookResults.size)
        assertEquals("facebook.com", facebookResults[0].site)
        
        assertEquals(1, gmailResults.size)
        assertEquals("user@gmail.com", gmailResults[0].username)
    }
    
    @Test
    fun updateEntry() = runTest {
        // Given
        val originalEntry = PasswordEntry(
            site = "example.com",
            username = "testuser",
            encryptedPassword = encryptionService.encrypt("oldpassword"),
            notes = encryptionService.encrypt("old notes")
        )
        
        // When
        val insertedId = passwordDao.insertEntry(originalEntry)
        val retrievedEntry = passwordDao.getEntryById(insertedId)!!
        
        val updatedEntry = retrievedEntry.copy(
            site = "updated.com",
            encryptedPassword = encryptionService.encrypt("newpassword"),
            updatedAt = System.currentTimeMillis()
        )
        
        passwordDao.updateEntry(updatedEntry)
        val finalEntry = passwordDao.getEntryById(insertedId)
        
        // Then
        assertNotNull(finalEntry)
        assertEquals("updated.com", finalEntry!!.site)
        assertEquals("testuser", finalEntry.username) // Unchanged
        
        // Verify password was updated by decrypting
        val decryptedPassword = encryptionService.decrypt(finalEntry.encryptedPassword)
        assertEquals("newpassword", decryptedPassword)
    }
    
    @Test
    fun deleteEntry() = runTest {
        // Given
        val testEntry = PasswordEntry(
            site = "example.com",
            username = "testuser",
            encryptedPassword = encryptionService.encrypt("testpassword")
        )
        
        // When
        val insertedId = passwordDao.insertEntry(testEntry)
        val retrievedEntry = passwordDao.getEntryById(insertedId)!!
        
        passwordDao.deleteEntry(retrievedEntry)
        val deletedEntry = passwordDao.getEntryById(insertedId)
        
        // Then
        assertNull(deletedEntry)
    }
    
    @Test
    fun deleteEntryById() = runTest {
        // Given
        val testEntry = PasswordEntry(
            site = "example.com",
            username = "testuser",
            encryptedPassword = encryptionService.encrypt("testpassword")
        )
        
        // When
        val insertedId = passwordDao.insertEntry(testEntry)
        val deletedCount = passwordDao.deleteEntryById(insertedId)
        val deletedEntry = passwordDao.getEntryById(insertedId)
        
        // Then
        assertEquals(1, deletedCount)
        assertNull(deletedEntry)
    }
    
    @Test
    fun getEntryCount() = runTest {
        // Given
        val entries = listOf(
            PasswordEntry(
                site = "site1.com",
                username = "user1",
                encryptedPassword = encryptionService.encrypt("password1")
            ),
            PasswordEntry(
                site = "site2.com",
                username = "user2",
                encryptedPassword = encryptionService.encrypt("password2")
            )
        )
        
        // When
        entries.forEach { passwordDao.insertEntry(it) }
        val count = passwordDao.getEntryCount()
        
        // Then
        assertEquals(2, count)
    }
    
    @Test
    fun getEntriesInTimeRange() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)
        val twoHoursAgo = now - (2 * 60 * 60 * 1000)
        
        val entries = listOf(
            PasswordEntry(
                site = "old.com",
                username = "user1",
                encryptedPassword = encryptionService.encrypt("password1"),
                createdAt = twoHoursAgo
            ),
            PasswordEntry(
                site = "recent.com",
                username = "user2",
                encryptedPassword = encryptionService.encrypt("password2"),
                createdAt = now
            )
        )
        
        // When
        entries.forEach { passwordDao.insertEntry(it) }
        val recentEntries = passwordDao.getEntriesInTimeRange(oneHourAgo, now + 1000)
        
        // Then
        assertEquals(1, recentEntries.size)
        assertEquals("recent.com", recentEntries[0].site)
    }
    
    @Test
    fun encryptionDecryptionRoundtrip() = runTest {
        // Given
        val originalPassword = "MySecurePassword123!"
        val originalNotes = "Important notes about this account"
        
        // When
        val encryptedPassword = encryptionService.encrypt(originalPassword)
        val encryptedNotes = encryptionService.encrypt(originalNotes)
        
        val entry = PasswordEntry(
            site = "test.com",
            username = "testuser",
            encryptedPassword = encryptedPassword,
            notes = encryptedNotes
        )
        
        val insertedId = passwordDao.insertEntry(entry)
        val retrievedEntry = passwordDao.getEntryById(insertedId)!!
        
        val decryptedPassword = encryptionService.decrypt(retrievedEntry.encryptedPassword)
        val decryptedNotes = encryptionService.decrypt(retrievedEntry.notes!!)
        
        // Then
        assertEquals(originalPassword, decryptedPassword)
        assertEquals(originalNotes, decryptedNotes)
    }
    
    @Test
    fun databaseEncryptionIntegrity() = runTest {
        // Given
        val testEntry = PasswordEntry(
            site = "secure.com",
            username = "secureuser",
            encryptedPassword = encryptionService.encrypt("securepassword")
        )
        
        // When
        passwordDao.insertEntry(testEntry)
        val allEntries = passwordDao.getAllEntries().first()
        
        // Then
        assertEquals(1, allEntries.size)
        val retrievedEntry = allEntries[0]
        
        // Verify that the stored password is encrypted (not plaintext)
        assertTrue(retrievedEntry.encryptedPassword != "securepassword")
        assertTrue(retrievedEntry.encryptedPassword.isNotEmpty())
        
        // Verify we can decrypt it back to original
        val decryptedPassword = encryptionService.decrypt(retrievedEntry.encryptedPassword)
        assertEquals("securepassword", decryptedPassword)
    }
    
    /**
     * Creates a test database key for SQLCipher.
     * Uses a deterministic approach for consistent testing.
     */
    private fun createTestDatabaseKey(): ByteArray {
        val testSalt = "TestDatabaseSalt2024"
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(testSalt.toByteArray())
        return digest.digest()
    }
}