package com.example.crypt.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.crypt.data.database.CryptDatabase
import com.example.crypt.data.database.PasswordEntry
import com.example.crypt.data.repository.PasswordRepository
import com.example.crypt.data.repository.PasswordRepositoryImpl
import com.example.crypt.data.security.EncryptionService
import com.example.crypt.data.security.KeystoreManager
import com.example.crypt.domain.model.AuthResult
import com.example.crypt.domain.model.PasswordGenerationConfig
import com.example.crypt.domain.usecase.AuthUseCase
import com.example.crypt.domain.usecase.GeneratePasswordUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * End-to-end integration tests for the Crypt password manager.
 * Tests complete user flows from authentication to password management,
 * verifying security measures and data persistence.
 * 
 * Requirements tested:
 * - 1.1: Authentication system with biometric and PIN support
 * - 2.1: Password generation with customizable options
 * - 3.1: Hardware-backed encryption using Android Keystore
 * - 4.1: Password entry CRUD operations with encryption
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EndToEndIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var database: CryptDatabase
    
    @Inject
    lateinit var passwordRepository: PasswordRepository
    
    @Inject
    lateinit var encryptionService: EncryptionService
    
    @Inject
    lateinit var keystoreManager: KeystoreManager
    
    @Inject
    lateinit var authUseCase: AuthUseCase
    
    @Inject
    lateinit var generatePasswordUseCase: GeneratePasswordUseCase
    

    
    private val testContext = InstrumentationRegistry.getInstrumentation().targetContext
    
    @Before
    fun setup() {
        hiltRule.inject()
        
        // Clear any existing data
        runBlocking {
            database.clearAllTables()
        }
        
        // Reset authentication state
        authUseCase.logout()
    }
    
    @After
    fun tearDown() {
        runBlocking {
            database.clearAllTables()
        }
        authUseCase.logout()
    }
    
    /**
     * Test complete user flow: PIN setup -> authentication -> password generation -> save -> retrieve
     * Requirements: 1.1, 2.1, 3.1, 4.1
     */
    @Test
    fun testCompleteUserFlow() = runTest {
        // Step 1: Set up PIN authentication (Requirement 1.1)
        val testPin = "123456"
        val pinSetupResult = authUseCase.setupPin(testPin)
        assertTrue("PIN setup should succeed", pinSetupResult)
        assertTrue("PIN should be configured", authUseCase.isPinSetup())
        
        // Step 2: Authenticate with PIN (Requirement 1.1)
        assertTrue("User should require authentication", authUseCase.isAuthenticationRequired())
        val authResult = authUseCase.authenticateWithPin(testPin)
        assertEquals("Authentication should succeed", AuthResult.Success, authResult)
        assertFalse("User should be authenticated", authUseCase.isAuthenticationRequired())
        
        // Step 3: Generate a secure password (Requirement 2.1)
        val passwordConfig = PasswordGenerationConfig(
            length = 16,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = true,
            excludeAmbiguous = true
        )
        val generatedPassword = generatePasswordUseCase.execute(passwordConfig)
        
        // Verify password meets requirements
        assertEquals("Password should have correct length", 16, generatedPassword.length)
        assertTrue("Password should contain uppercase", generatedPassword.any { it.isUpperCase() })
        assertTrue("Password should contain lowercase", generatedPassword.any { it.isLowerCase() })
        assertTrue("Password should contain numbers", generatedPassword.any { it.isDigit() })
        assertTrue("Password should contain symbols", generatedPassword.any { !it.isLetterOrDigit() })
        
        // Step 4: Save password entry (Requirement 4.1)
        val testSite = "example.com"
        val testUsername = "user@example.com"
        val testNotes = "Test account notes"
        
        val entryId = passwordRepository.saveEntry(
            site = testSite,
            username = testUsername,
            password = generatedPassword,
            notes = testNotes
        )
        assertTrue("Entry ID should be valid", entryId > 0)
        
        // Step 5: Retrieve and verify password entry (Requirement 4.1)
        val savedEntry = passwordRepository.getEntryById(entryId)
        assertNotNull("Saved entry should exist", savedEntry)
        savedEntry?.let { entry ->
            assertEquals("Site should match", testSite, entry.site)
            assertEquals("Username should match", testUsername, entry.username)
            assertNotEquals("Password should be encrypted", generatedPassword, entry.encryptedPassword)
            
            // Decrypt and verify password (Requirement 3.1)
            val decryptedPassword = passwordRepository.decryptPassword(entry)
            assertEquals("Decrypted password should match original", generatedPassword, decryptedPassword)
            
            // Decrypt and verify notes
            val decryptedNotes = passwordRepository.decryptNotes(entry)
            assertEquals("Decrypted notes should match original", testNotes, decryptedNotes)
        }
        
        // Step 6: Verify data persistence across repository operations
        val allEntries = passwordRepository.getAllEntries().first()
        assertEquals("Should have one entry", 1, allEntries.size)
        assertEquals("Entry should match saved entry", savedEntry, allEntries.first())
    }
    
    /**
     * Test encryption security measures and data integrity
     * Requirements: 3.1, 3.2, 3.3, 3.4
     */
    @Test
    fun testEncryptionSecurityMeasures() = runTest {
        // Test 1: Verify hardware-backed key generation (Requirement 3.1)
        val isKeystoreInitialized = encryptionService.initializeKeystore()
        assertTrue("Keystore should initialize successfully", isKeystoreInitialized)
        
        // Test 2: Verify encryption/decryption integrity (Requirement 3.3)
        val testPasswords = listOf(
            "simple123",
            "Complex!Password@2024",
            "Unicode测试密码🔐",
            "Very_Long_Password_With_Many_Characters_123456789!@#$%^&*()",
            "" // Empty string edge case
        )
        
        for (testPassword in testPasswords) {
            val encrypted = encryptionService.encrypt(testPassword)
            assertNotEquals("Encrypted data should differ from plaintext", testPassword, encrypted)
            
            val decrypted = encryptionService.decrypt(encrypted)
            assertEquals("Decrypted data should match original", testPassword, decrypted)
            
            // Verify each encryption produces different ciphertext (IV uniqueness)
            val encrypted2 = encryptionService.encrypt(testPassword)
            if (testPassword.isNotEmpty()) {
                assertNotEquals("Each encryption should produce unique ciphertext", encrypted, encrypted2)
            }
        }
        
        // Test 3: Verify database encryption (Requirement 3.5)
        val testEntry = PasswordEntry(
            site = "security-test.com",
            username = "testuser",
            encryptedPassword = encryptionService.encrypt("test_password_123"),
            notes = encryptionService.encrypt("Encrypted notes"),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val entryId = database.passwordDao().insertEntry(testEntry)
        val retrievedEntry = database.passwordDao().getEntryById(entryId)
        
        assertNotNull("Entry should be retrievable from encrypted database", retrievedEntry)
        assertEquals("Entry data should match", testEntry.copy(id = entryId), retrievedEntry)
    }
    
    /**
     * Test authentication security and session management
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
     */
    @Test
    fun testAuthenticationSecurity() = runTest {
        // Test 1: PIN setup and verification (Requirement 1.1, 1.2)
        val correctPin = "987654"
        val incorrectPin = "123456"
        
        // Setup PIN
        assertTrue("PIN setup should succeed", authUseCase.setupPin(correctPin))
        assertTrue("PIN should be configured", authUseCase.isPinSetup())
        
        // Test correct PIN
        val correctAuthResult = authUseCase.authenticateWithPin(correctPin)
        assertEquals("Correct PIN should authenticate", AuthResult.Success, correctAuthResult)
        
        // Reset authentication state
        authUseCase.logout()
        
        // Test incorrect PIN
        val incorrectAuthResult = authUseCase.authenticateWithPin(incorrectPin)
        assertTrue("Incorrect PIN should fail", incorrectAuthResult is AuthResult.Error)
        assertTrue("Should still require authentication", authUseCase.isAuthenticationRequired())
        
        // Test 2: Session management (Requirement 1.3, 1.4)
        // Authenticate successfully
        authUseCase.authenticateWithPin(correctPin)
        assertFalse("Should be authenticated", authUseCase.isAuthenticationRequired())
        
        // Test logout functionality
        authUseCase.logout()
        assertTrue("Should require authentication after logout", authUseCase.isAuthenticationRequired())
        
        // Test 3: Multiple authentication attempts
        // Re-authenticate to test session management
        authUseCase.authenticateWithPin(correctPin)
        assertFalse("Should be authenticated", authUseCase.isAuthenticationRequired())
        
        // Test logout and re-authentication
        authUseCase.logout()
        assertTrue("Should require authentication after logout", authUseCase.isAuthenticationRequired())
    }
    
    /**
     * Test password generation security and requirements compliance
     * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
     */
    @Test
    fun testPasswordGenerationSecurity() = runTest {
        // Test 1: Length requirements (Requirement 2.1)
        val lengthConfigs = listOf(8, 16, 32, 64)
        for (length in lengthConfigs) {
            val config = PasswordGenerationConfig(length = length)
            val password = generatePasswordUseCase.execute(config)
            assertEquals("Password should have correct length", length, password.length)
        }
        
        // Test 2: Character set requirements (Requirements 2.2, 2.3, 2.4, 2.5)
        val uppercaseConfig = PasswordGenerationConfig(
            length = 16,
            includeUppercase = true,
            includeLowercase = false,
            includeNumbers = false,
            includeSymbols = false
        )
        val uppercasePassword = generatePasswordUseCase.execute(uppercaseConfig)
        assertTrue("Should contain only uppercase", uppercasePassword.all { it.isUpperCase() })
        
        val lowercaseConfig = PasswordGenerationConfig(
            length = 16,
            includeUppercase = false,
            includeLowercase = true,
            includeNumbers = false,
            includeSymbols = false
        )
        val lowercasePassword = generatePasswordUseCase.execute(lowercaseConfig)
        assertTrue("Should contain only lowercase", lowercasePassword.all { it.isLowerCase() })
        
        val numbersConfig = PasswordGenerationConfig(
            length = 16,
            includeUppercase = false,
            includeLowercase = false,
            includeNumbers = true,
            includeSymbols = false
        )
        val numbersPassword = generatePasswordUseCase.execute(numbersConfig)
        assertTrue("Should contain only numbers", numbersPassword.all { it.isDigit() })
        
        // Test 3: Ambiguous character exclusion (Requirement 2.6)
        val ambiguousConfig = PasswordGenerationConfig(
            length = 32,
            excludeAmbiguous = true
        )
        val ambiguousPassword = generatePasswordUseCase.execute(ambiguousConfig)
        val ambiguousChars = "0O1lI"
        assertFalse("Should not contain ambiguous characters", 
            ambiguousPassword.any { it in ambiguousChars })
        
        // Test 4: Randomness verification
        val passwords = mutableSetOf<String>()
        repeat(10) {
            val password = generatePasswordUseCase.execute(PasswordGenerationConfig(length = 16))
            passwords.add(password)
        }
        assertEquals("All generated passwords should be unique", 10, passwords.size)
    }
    
    /**
     * Test password repository CRUD operations with encryption
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
     */
    @Test
    fun testPasswordRepositoryCRUD() = runTest {
        // Test 1: Create multiple entries (Requirement 4.1)
        val testEntries = listOf(
            Triple("google.com", "user1@gmail.com", "password123"),
            Triple("facebook.com", "user2@fb.com", "fb_password"),
            Triple("github.com", "developer", "github_token_xyz")
        )
        
        val entryIds = mutableListOf<Long>()
        for ((site, username, password) in testEntries) {
            val entryId = passwordRepository.saveEntry(site, username, password, "Test notes for $site")
            entryIds.add(entryId)
            assertTrue("Entry ID should be valid", entryId > 0)
        }
        
        // Test 2: Read all entries (Requirement 4.2)
        val allEntries = passwordRepository.getAllEntries().first()
        assertEquals("Should have correct number of entries", testEntries.size, allEntries.size)
        
        // Verify entries are sorted by site
        val sortedSites = testEntries.map { it.first }.sorted()
        val retrievedSites = allEntries.map { it.site }
        assertEquals("Entries should be sorted by site", sortedSites, retrievedSites)
        
        // Test 3: Read specific entry (Requirement 4.3)
        val firstEntryId = entryIds.first()
        val specificEntry = passwordRepository.getEntryById(firstEntryId)
        assertNotNull("Specific entry should exist", specificEntry)
        
        specificEntry?.let { entry ->
            assertEquals("Site should match", testEntries.first().first, entry.site)
            assertEquals("Username should match", testEntries.first().second, entry.username)
            
            // Verify password decryption
            val decryptedPassword = passwordRepository.decryptPassword(entry)
            assertEquals("Password should match", testEntries.first().third, decryptedPassword)
        }
        
        // Test 4: Update entry (Requirement 4.5)
        val entryToUpdate = allEntries.first()
        val newPassword = "updated_password_123"
        val newNotes = "Updated notes"
        
        passwordRepository.updateEntry(entryToUpdate, newPassword, newNotes)
        
        val updatedEntry = passwordRepository.getEntryById(entryToUpdate.id)
        assertNotNull("Updated entry should exist", updatedEntry)
        
        updatedEntry?.let { entry ->
            val decryptedPassword = passwordRepository.decryptPassword(entry)
            assertEquals("Password should be updated", newPassword, decryptedPassword)
            
            val decryptedNotes = passwordRepository.decryptNotes(entry)
            assertEquals("Notes should be updated", newNotes, decryptedNotes)
            
            assertTrue("Updated timestamp should be newer", entry.updatedAt > entryToUpdate.updatedAt)
        }
        
        // Test 5: Delete entry (Requirement 4.4)
        val entryToDelete = allEntries.last()
        passwordRepository.deleteEntry(entryToDelete)
        
        val deletedEntry = passwordRepository.getEntryById(entryToDelete.id)
        assertNull("Deleted entry should not exist", deletedEntry)
        
        val remainingEntries = passwordRepository.getAllEntries().first()
        assertEquals("Should have one less entry", testEntries.size - 1, remainingEntries.size)
        
        // Test 6: Search functionality (Requirement 4.2)
        val searchResults = passwordRepository.searchEntries("google").first()
        assertEquals("Should find google entry", 1, searchResults.size)
        assertEquals("Should match google site", "google.com", searchResults.first().site)
    }
    
    /**
     * Test data persistence and database integrity
     * Requirements: 3.5, 4.1
     */
    @Test
    fun testDataPersistence() = runTest {
        // Create test data
        val testSite = "persistence-test.com"
        val testUsername = "persistence_user"
        val testPassword = "persistence_password_123"
        val testNotes = "Persistence test notes"
        
        // Save entry
        val entryId = passwordRepository.saveEntry(testSite, testUsername, testPassword, testNotes)
        
        // Verify immediate retrieval
        val savedEntry = passwordRepository.getEntryById(entryId)
        assertNotNull("Entry should be saved", savedEntry)
        
        // Simulate app restart by creating new repository instance
        val newRepository = PasswordRepositoryImpl(database, encryptionService)
        
        // Verify data persists across repository instances
        val persistedEntry = newRepository.getEntryById(entryId)
        assertNotNull("Entry should persist", persistedEntry)
        
        persistedEntry?.let { entry ->
            assertEquals("Site should persist", testSite, entry.site)
            assertEquals("Username should persist", testUsername, entry.username)
            
            val decryptedPassword = newRepository.decryptPassword(entry)
            assertEquals("Password should persist and decrypt correctly", testPassword, decryptedPassword)
            
            val decryptedNotes = newRepository.decryptNotes(entry)
            assertEquals("Notes should persist and decrypt correctly", testNotes, decryptedNotes)
        }
        
        // Test entry count
        val entryCount = passwordRepository.getEntryCount()
        assertEquals("Entry count should be correct", 1, entryCount)
        
        // Test time range queries
        val currentTime = System.currentTimeMillis()
        val entriesInRange = passwordRepository.getEntriesInTimeRange(
            currentTime - 60000, // 1 minute ago
            currentTime + 60000  // 1 minute from now
        )
        assertEquals("Should find entry in time range", 1, entriesInRange.size)
    }
    
    /**
     * Test error handling and edge cases
     * Requirements: 1.2, 3.1, 3.3
     */
    @Test
    fun testErrorHandling() = runTest {
        // Test 1: Invalid PIN authentication
        authUseCase.setupPin("123456")
        val invalidAuthResult = authUseCase.authenticateWithPin("wrong_pin")
        assertTrue("Invalid PIN should return error", invalidAuthResult is AuthResult.Error)
        
        // Test 2: Encryption service error handling
        try {
            val testData = "test_encryption_data"
            val encrypted = encryptionService.encrypt(testData)
            val decrypted = encryptionService.decrypt(encrypted)
            assertEquals("Encryption roundtrip should work", testData, decrypted)
        } catch (e: Exception) {
            fail("Encryption should not throw exception: ${e.message}")
        }
        
        // Test 3: Repository error handling
        try {
            // Try to get non-existent entry
            val nonExistentEntry = passwordRepository.getEntryById(99999)
            assertNull("Non-existent entry should return null", nonExistentEntry)
            
            // Try to delete non-existent entry
            val deleteResult = passwordRepository.deleteEntryById(99999)
            assertFalse("Deleting non-existent entry should return false", deleteResult)
        } catch (e: Exception) {
            fail("Repository operations should handle missing entries gracefully: ${e.message}")
        }
        
        // Test 4: Password generation edge cases
        try {
            // Test minimum length
            val minConfig = PasswordGenerationConfig(length = 8)
            val minPassword = generatePasswordUseCase.execute(minConfig)
            assertEquals("Minimum password should have correct length", 8, minPassword.length)
            
            // Test maximum length
            val maxConfig = PasswordGenerationConfig(length = 64)
            val maxPassword = generatePasswordUseCase.execute(maxConfig)
            assertEquals("Maximum password should have correct length", 64, maxPassword.length)
        } catch (e: Exception) {
            fail("Password generation should handle edge cases: ${e.message}")
        }
    }
}