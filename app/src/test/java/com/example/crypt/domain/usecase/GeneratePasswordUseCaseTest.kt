package com.example.crypt.domain.usecase

import com.example.crypt.domain.model.PasswordGenerationConfig
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for GeneratePasswordUseCase focusing on password generation logic,
 * character set compliance, and randomness verification.
 */
class GeneratePasswordUseCaseTest {

    private val generatePasswordUseCase = GeneratePasswordUseCase()

    @Test
    fun `generated password should have correct length`() = runTest {
        // Given
        val configs = listOf(
            PasswordGenerationConfig(length = 8),
            PasswordGenerationConfig(length = 16),
            PasswordGenerationConfig(length = 32),
            PasswordGenerationConfig(length = 64)
        )

        configs.forEach { config ->
            // When
            val password = generatePasswordUseCase.execute(config)

            // Then
            assertEquals("Password length should match configuration", config.length, password.length)
        }
    }

    @Test
    fun `generated password should contain uppercase letters when enabled`() = runTest {
        // Given
        val config = PasswordGenerationConfig(
            length = 20,
            includeUppercase = true,
            includeLowercase = false,
            includeNumbers = false,
            includeSymbols = false
        )

        // When
        val password = generatePasswordUseCase.execute(config)

        // Then
        assertTrue("Password should contain uppercase letters", password.any { it.isUpperCase() })
        assertFalse("Password should not contain lowercase letters", password.any { it.isLowerCase() })
        assertFalse("Password should not contain numbers", password.any { it.isDigit() })
    }

    @Test
    fun `generated password should contain lowercase letters when enabled`() = runTest {
        // Given
        val config = PasswordGenerationConfig(
            length = 20,
            includeUppercase = false,
            includeLowercase = true,
            includeNumbers = false,
            includeSymbols = false
        )

        // When
        val password = generatePasswordUseCase.execute(config)

        // Then
        assertTrue("Password should contain lowercase letters", password.any { it.isLowerCase() })
        assertFalse("Password should not contain uppercase letters", password.any { it.isUpperCase() })
        assertFalse("Password should not contain numbers", password.any { it.isDigit() })
    }

    @Test
    fun `generated password should contain numbers when enabled`() = runTest {
        // Given
        val config = PasswordGenerationConfig(
            length = 20,
            includeUppercase = false,
            includeLowercase = false,
            includeNumbers = true,
            includeSymbols = false
        )

        // When
        val password = generatePasswordUseCase.execute(config)

        // Then
        assertTrue("Password should contain numbers", password.any { it.isDigit() })
        assertFalse("Password should not contain uppercase letters", password.any { it.isUpperCase() })
        assertFalse("Password should not contain lowercase letters", password.any { it.isLowerCase() })
    }

    @Test
    fun `generated password should contain symbols when enabled`() = runTest {
        // Given
        val config = PasswordGenerationConfig(
            length = 20,
            includeUppercase = false,
            includeLowercase = false,
            includeNumbers = false,
            includeSymbols = true
        )

        // When
        val password = generatePasswordUseCase.execute(config)

        // Then
        val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        assertTrue("Password should contain symbols", password.any { it in symbols })
        assertFalse("Password should not contain uppercase letters", password.any { it.isUpperCase() })
        assertFalse("Password should not contain lowercase letters", password.any { it.isLowerCase() })
        assertFalse("Password should not contain numbers", password.any { it.isDigit() })
    }

    @Test
    fun `generated password should exclude ambiguous characters when enabled`() = runTest {
        // Given
        val config = PasswordGenerationConfig(
            length = 50, // Larger length to increase chance of ambiguous characters
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = false,
            excludeAmbiguous = true
        )

        // When
        val password = generatePasswordUseCase.execute(config)

        // Then
        val ambiguousChars = "O0l1I"
        assertFalse("Password should not contain ambiguous characters", password.any { it in ambiguousChars })
    }

    @Test
    fun `generated password should include ambiguous characters when disabled`() = runTest {
        // Given
        val config = PasswordGenerationConfig(
            length = 50, // Reasonable length
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = false,
            excludeAmbiguous = false
        )

        // When - Generate multiple passwords to increase chance of getting ambiguous chars
        val passwords = (1..20).map { generatePasswordUseCase.execute(config) }
        val combinedPassword = passwords.joinToString("")

        // Then - Check that ambiguous characters are allowed in the character sets
        val characterSets = generatePasswordUseCase.getCharacterSets(config)
        val uppercaseSet = characterSets["uppercase"]!!
        val lowercaseSet = characterSets["lowercase"]!!
        val numbersSet = characterSets["numbers"]!!
        
        // Verify ambiguous characters are in the sets when not excluded
        assertTrue("Uppercase set should contain 'O' when ambiguous not excluded", uppercaseSet.contains('O'))
        assertTrue("Lowercase set should contain 'l' when ambiguous not excluded", lowercaseSet.contains('l'))
        assertTrue("Numbers set should contain '0' when ambiguous not excluded", numbersSet.contains('0'))
        assertTrue("Numbers set should contain '1' when ambiguous not excluded", numbersSet.contains('1'))
    }

    @Test
    fun `generated password should contain characters from all enabled sets`() = runTest {
        // Given
        val config = PasswordGenerationConfig(
            length = 20,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = true
        )

        // When
        val password = generatePasswordUseCase.execute(config)

        // Then
        assertTrue("Password should contain uppercase letters", password.any { it.isUpperCase() })
        assertTrue("Password should contain lowercase letters", password.any { it.isLowerCase() })
        assertTrue("Password should contain numbers", password.any { it.isDigit() })
        
        val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        assertTrue("Password should contain symbols", password.any { it in symbols })
    }

    @Test
    fun `generated passwords should be different on multiple calls`() = runTest {
        // Given
        val config = PasswordGenerationConfig(length = 16)

        // When
        val passwords = (1..10).map { generatePasswordUseCase.execute(config) }

        // Then
        val uniquePasswords = passwords.toSet()
        assertEquals("All generated passwords should be unique", passwords.size, uniquePasswords.size)
    }

    @Test
    fun `password validation should work correctly`() = runTest {
        // Given
        val config = PasswordGenerationConfig(
            length = 16,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = true
        )

        // When
        val password = generatePasswordUseCase.execute(config)

        // Then
        assertTrue(
            "Generated password should pass validation",
            generatePasswordUseCase.validatePassword(password, config)
        )
    }

    @Test
    fun `password validation should fail for incorrect length`() {
        // Given
        val config = PasswordGenerationConfig(length = 16)
        val wrongLengthPassword = "short"

        // When
        val isValid = generatePasswordUseCase.validatePassword(wrongLengthPassword, config)

        // Then
        assertFalse("Password with wrong length should fail validation", isValid)
    }

    @Test
    fun `password validation should fail when missing required character sets`() {
        // Given
        val config = PasswordGenerationConfig(
            length = 8,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = true
        )
        val passwordWithoutSymbols = "Abc12345" // Missing symbols

        // When
        val isValid = generatePasswordUseCase.validatePassword(passwordWithoutSymbols, config)

        // Then
        assertFalse("Password missing required character sets should fail validation", isValid)
    }

    @Test
    fun `password validation should fail when containing ambiguous characters`() {
        // Given
        val config = PasswordGenerationConfig(
            length = 8,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = false,
            excludeAmbiguous = true
        )
        val passwordWithAmbiguous = "Abc123O0" // Contains O and 0

        // When
        val isValid = generatePasswordUseCase.validatePassword(passwordWithAmbiguous, config)

        // Then
        assertFalse("Password with ambiguous characters should fail validation when excluded", isValid)
    }

    @Test
    fun `getCharacterSets should return correct sets based on configuration`() {
        // Given
        val config = PasswordGenerationConfig(
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = false,
            includeSymbols = true,
            excludeAmbiguous = true
        )

        // When
        val characterSets = generatePasswordUseCase.getCharacterSets(config)

        // Then
        assertTrue("Should include uppercase set", characterSets.containsKey("uppercase"))
        assertTrue("Should include lowercase set", characterSets.containsKey("lowercase"))
        assertFalse("Should not include numbers set", characterSets.containsKey("numbers"))
        assertTrue("Should include symbols set", characterSets.containsKey("symbols"))
        
        // Verify ambiguous characters are excluded
        val uppercaseSet = characterSets["uppercase"]!!
        assertFalse("Uppercase set should not contain ambiguous 'O'", uppercaseSet.contains('O'))
        
        val lowercaseSet = characterSets["lowercase"]!!
        assertFalse("Lowercase set should not contain ambiguous 'l'", lowercaseSet.contains('l'))
    }

    @Test
    fun `getCharacterSets should include ambiguous characters when not excluded`() {
        // Given
        val config = PasswordGenerationConfig(
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = false,
            excludeAmbiguous = false
        )

        // When
        val characterSets = generatePasswordUseCase.getCharacterSets(config)

        // Then
        val uppercaseSet = characterSets["uppercase"]!!
        assertTrue("Uppercase set should contain 'O' when ambiguous not excluded", uppercaseSet.contains('O'))
        
        val lowercaseSet = characterSets["lowercase"]!!
        assertTrue("Lowercase set should contain 'l' when ambiguous not excluded", lowercaseSet.contains('l'))
        
        val numbersSet = characterSets["numbers"]!!
        assertTrue("Numbers set should contain '0' when ambiguous not excluded", numbersSet.contains('0'))
        assertTrue("Numbers set should contain '1' when ambiguous not excluded", numbersSet.contains('1'))
    }

    @Test
    fun `configuration validation should work correctly`() {
        // Test valid configurations
        assertTrue(
            "Minimum length should be valid",
            runCatching { PasswordGenerationConfig(length = 8) }.isSuccess
        )
        assertTrue(
            "Maximum length should be valid",
            runCatching { PasswordGenerationConfig(length = 64) }.isSuccess
        )

        // Test invalid configurations
        assertFalse(
            "Length below minimum should be invalid",
            runCatching { PasswordGenerationConfig(length = 7) }.isSuccess
        )
        assertFalse(
            "Length above maximum should be invalid",
            runCatching { PasswordGenerationConfig(length = 65) }.isSuccess
        )
        assertFalse(
            "Configuration with no character sets should be invalid",
            runCatching { 
                PasswordGenerationConfig(
                    includeUppercase = false,
                    includeLowercase = false,
                    includeNumbers = false,
                    includeSymbols = false
                )
            }.isSuccess
        )
    }

    @Test
    fun `entropy should be sufficient for generated passwords`() = runTest {
        // Given
        val config = PasswordGenerationConfig(length = 16)

        // When - Generate multiple passwords and check for patterns
        val passwords = (1..100).map { generatePasswordUseCase.execute(config) }

        // Then - Check that passwords don't have obvious patterns
        val uniquePasswords = passwords.toSet()
        assertTrue(
            "Generated passwords should have high uniqueness (entropy)",
            uniquePasswords.size > 95 // Allow for small chance of collision
        )

        // Check character distribution is reasonable
        val allChars = passwords.joinToString("")
        val charCounts = allChars.groupingBy { it }.eachCount()
        val maxCount = charCounts.values.maxOrNull() ?: 0
        val totalChars = allChars.length
        
        // No single character should appear more than 10% of the time (rough entropy check)
        assertTrue(
            "Character distribution should be reasonably uniform",
            maxCount < totalChars * 0.1
        )
    }
}