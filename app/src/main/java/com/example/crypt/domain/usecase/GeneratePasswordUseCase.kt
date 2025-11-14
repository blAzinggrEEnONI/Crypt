package com.example.crypt.domain.usecase

import com.example.crypt.domain.model.PasswordGenerationConfig
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for generating cryptographically secure passwords with customizable options.
 * Supports configurable length, character sets, and ambiguous character exclusion.
 */
@Singleton
class GeneratePasswordUseCase @Inject constructor() {
    
    companion object {
        // Character sets for password generation
        private const val UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz"
        private const val NUMBERS = "0123456789"
        private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        
        // Ambiguous characters that look similar
        private const val AMBIGUOUS_UPPERCASE = "O"
        private const val AMBIGUOUS_LOWERCASE = "l"
        private const val AMBIGUOUS_NUMBERS = "01"
        private const val AMBIGUOUS_SYMBOLS = ""
        
        // Clear character sets (without ambiguous characters)
        private const val CLEAR_UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        private const val CLEAR_LOWERCASE = "abcdefghijkmnopqrstuvwxyz"
        private const val CLEAR_NUMBERS = "23456789"
        private const val CLEAR_SYMBOLS = SYMBOLS // No ambiguous symbols to exclude
    }
    
    private val secureRandom = SecureRandom()
    
    /**
     * Generates a cryptographically secure password based on the provided configuration.
     * 
     * @param config Password generation configuration
     * @return Generated password string
     * @throws IllegalArgumentException if configuration is invalid
     */
    suspend fun execute(config: PasswordGenerationConfig): String {
        val characterPool = buildCharacterPool(config)
        
        if (characterPool.isEmpty()) {
            throw IllegalArgumentException("No characters available for password generation")
        }
        
        return generatePassword(characterPool, config)
    }
    
    /**
     * Builds the character pool based on the configuration settings.
     */
    private fun buildCharacterPool(config: PasswordGenerationConfig): String {
        val pool = StringBuilder()
        
        if (config.includeUppercase) {
            pool.append(if (config.excludeAmbiguous) CLEAR_UPPERCASE else UPPERCASE_LETTERS)
        }
        
        if (config.includeLowercase) {
            pool.append(if (config.excludeAmbiguous) CLEAR_LOWERCASE else LOWERCASE_LETTERS)
        }
        
        if (config.includeNumbers) {
            pool.append(if (config.excludeAmbiguous) CLEAR_NUMBERS else NUMBERS)
        }
        
        if (config.includeSymbols) {
            pool.append(if (config.excludeAmbiguous) CLEAR_SYMBOLS else SYMBOLS)
        }
        
        return pool.toString()
    }
    
    /**
     * Generates a password ensuring at least one character from each enabled character set.
     */
    private fun generatePassword(characterPool: String, config: PasswordGenerationConfig): String {
        val password = CharArray(config.length)
        val requiredCharacters = mutableListOf<Char>()
        
        // Ensure at least one character from each enabled set
        if (config.includeUppercase) {
            val uppercaseChars = if (config.excludeAmbiguous) CLEAR_UPPERCASE else UPPERCASE_LETTERS
            requiredCharacters.add(uppercaseChars[secureRandom.nextInt(uppercaseChars.length)])
        }
        
        if (config.includeLowercase) {
            val lowercaseChars = if (config.excludeAmbiguous) CLEAR_LOWERCASE else LOWERCASE_LETTERS
            requiredCharacters.add(lowercaseChars[secureRandom.nextInt(lowercaseChars.length)])
        }
        
        if (config.includeNumbers) {
            val numberChars = if (config.excludeAmbiguous) CLEAR_NUMBERS else NUMBERS
            requiredCharacters.add(numberChars[secureRandom.nextInt(numberChars.length)])
        }
        
        if (config.includeSymbols) {
            val symbolChars = if (config.excludeAmbiguous) CLEAR_SYMBOLS else SYMBOLS
            requiredCharacters.add(symbolChars[secureRandom.nextInt(symbolChars.length)])
        }
        
        // Fill required characters first
        for (i in requiredCharacters.indices) {
            password[i] = requiredCharacters[i]
        }
        
        // Fill remaining positions with random characters from the pool
        for (i in requiredCharacters.size until config.length) {
            password[i] = characterPool[secureRandom.nextInt(characterPool.length)]
        }
        
        // Shuffle the password to avoid predictable patterns
        shuffleArray(password)
        
        return String(password)
    }
    
    /**
     * Shuffles the character array using Fisher-Yates algorithm with SecureRandom.
     */
    private fun shuffleArray(array: CharArray) {
        for (i in array.size - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temp = array[i]
            array[i] = array[j]
            array[j] = temp
        }
    }
    
    /**
     * Validates that the generated password meets the configuration requirements.
     * This is used internally for testing and validation.
     */
    internal fun validatePassword(password: String, config: PasswordGenerationConfig): Boolean {
        if (password.length != config.length) return false
        
        val hasUppercase = password.any { it in UPPERCASE_LETTERS }
        val hasLowercase = password.any { it in LOWERCASE_LETTERS }
        val hasNumbers = password.any { it in NUMBERS }
        val hasSymbols = password.any { it in SYMBOLS }
        
        if (config.includeUppercase && !hasUppercase) return false
        if (config.includeLowercase && !hasLowercase) return false
        if (config.includeNumbers && !hasNumbers) return false
        if (config.includeSymbols && !hasSymbols) return false
        
        if (config.excludeAmbiguous) {
            val ambiguousChars = AMBIGUOUS_UPPERCASE + AMBIGUOUS_LOWERCASE + AMBIGUOUS_NUMBERS
            if (password.any { it in ambiguousChars }) return false
        }
        
        return true
    }
    
    /**
     * Gets the available character sets based on configuration.
     * Useful for UI display and testing.
     */
    fun getCharacterSets(config: PasswordGenerationConfig): Map<String, String> {
        val sets = mutableMapOf<String, String>()
        
        if (config.includeUppercase) {
            sets["uppercase"] = if (config.excludeAmbiguous) CLEAR_UPPERCASE else UPPERCASE_LETTERS
        }
        
        if (config.includeLowercase) {
            sets["lowercase"] = if (config.excludeAmbiguous) CLEAR_LOWERCASE else LOWERCASE_LETTERS
        }
        
        if (config.includeNumbers) {
            sets["numbers"] = if (config.excludeAmbiguous) CLEAR_NUMBERS else NUMBERS
        }
        
        if (config.includeSymbols) {
            sets["symbols"] = if (config.excludeAmbiguous) CLEAR_SYMBOLS else SYMBOLS
        }
        
        return sets
    }
}