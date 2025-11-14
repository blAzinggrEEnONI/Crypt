package com.example.crypt.domain.model

/**
 * Configuration for password generation with customizable options.
 * 
 * @param length Password length between 8 and 64 characters
 * @param includeUppercase Include uppercase letters (A-Z)
 * @param includeLowercase Include lowercase letters (a-z)
 * @param includeNumbers Include numbers (0-9)
 * @param includeSymbols Include special symbols
 * @param excludeAmbiguous Exclude visually similar characters (0, O, l, 1, I)
 */
data class PasswordGenerationConfig(
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val excludeAmbiguous: Boolean = true
) {
    init {
        require(length in 8..64) { "Password length must be between 8 and 64 characters" }
        require(includeUppercase || includeLowercase || includeNumbers || includeSymbols) {
            "At least one character set must be enabled"
        }
    }
}