package com.example.crypt.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.crypt.domain.model.AuthResult
import com.example.crypt.domain.model.AuthErrorCode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Use case for handling biometric and PIN authentication.
 * Manages authentication state and provides secure PIN storage using EncryptedSharedPreferences.
 */
@Singleton
class AuthUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LAST_ATTEMPT_TIME = "last_attempt_time"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds
        private const val SALT_LENGTH = 32
    }

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _biometricAuthEvent = MutableSharedFlow<Unit>()
    val biometricAuthEvent = _biometricAuthEvent.asSharedFlow()

    private val secureRandom = SecureRandom()

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Checks if biometric authentication is available on the device.
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    suspend fun triggerBiometricAuth() {
        _biometricAuthEvent.emit(Unit)
    }

    /**
     * Authenticates user using biometric prompt.
     * @param activity The FragmentActivity to show the biometric prompt
     * @return AuthResult indicating success or failure
     */
    suspend fun authenticateWithBiometrics(activity: FragmentActivity): AuthResult {
        return suspendCoroutine { continuation ->
            val biometricManager = BiometricManager.from(context)

            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    val executor = ContextCompat.getMainExecutor(context)
                    val biometricPrompt = BiometricPrompt(activity, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                val result = when (errorCode) {
                                    BiometricPrompt.ERROR_USER_CANCELED,
                                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> AuthResult.UserCancelled
                                    BiometricPrompt.ERROR_HW_UNAVAILABLE,
                                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> AuthResult.Error(
                                        errString.toString(),
                                        AuthErrorCode.HARDWARE_UNAVAILABLE
                                    )
                                    BiometricPrompt.ERROR_NO_BIOMETRICS -> AuthResult.Error(
                                        errString.toString(),
                                        AuthErrorCode.NO_BIOMETRIC_ENROLLED
                                    )
                                    else -> AuthResult.Error(
                                        errString.toString(),
                                        AuthErrorCode.BIOMETRIC_ERROR
                                    )
                                }
                                continuation.resume(result)
                            }

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                _isAuthenticated.value = true
                                resetFailedAttempts()
                                continuation.resume(AuthResult.Success)
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                // Don'''t resume here, let user try again
                            }
                        })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Authenticate to access Crypt")
                        .setSubtitle("Use your biometric credential to unlock your password vault")
                        .setNegativeButtonText("Use PIN")
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    continuation.resume(
                        AuthResult.Error(
                            "Biometric hardware not available",
                            AuthErrorCode.HARDWARE_UNAVAILABLE
                        )
                    )
                }
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    continuation.resume(
                        AuthResult.Error(
                            "Biometric hardware currently unavailable",
                            AuthErrorCode.HARDWARE_UNAVAILABLE
                        )
                    )
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    continuation.resume(
                        AuthResult.Error(
                            "No biometric credentials enrolled",
                            AuthErrorCode.NO_BIOMETRIC_ENROLLED
                        )
                    )
                }
                else -> {
                    continuation.resume(AuthResult.BiometricUnavailable)
                }
            }
        }
    }

    /**
     * Authenticates user using PIN.
     * @param pin The PIN entered by the user
     * @return AuthResult indicating success or failure
     */
    suspend fun authenticateWithPin(pin: String): AuthResult {
        if (isLockedOut()) {
            return AuthResult.Error(
                "Too many failed attempts. Please try again later.",
                AuthErrorCode.TOO_MANY_ATTEMPTS
            )
        }

        val storedHash = encryptedPrefs.getString(KEY_PIN_HASH, null)
        val storedSalt = encryptedPrefs.getString(KEY_PIN_SALT, null)

        if (storedHash == null || storedSalt == null) {
            return AuthResult.Error(
                "PIN not set up",
                AuthErrorCode.PIN_INCORRECT
            )
        }

        val pinHash = hashPin(pin, storedSalt)

        return if (pinHash == storedHash) {
            _isAuthenticated.value = true
            resetFailedAttempts()
            AuthResult.Success
        } else {
            incrementFailedAttempts()
            AuthResult.Error(
                "Incorrect PIN",
                AuthErrorCode.PIN_INCORRECT
            )
        }
    }

    /**
     * Sets up a new PIN for the user.
     * @param pin The new PIN to set
     * @return true if PIN was set successfully, false otherwise
     */
    suspend fun setupPin(pin: String): Boolean {
        return try {
            if (pin.length < 4) {
                return false
            }

            val salt = generateSalt()
            val pinHash = hashPin(pin, salt)

            encryptedPrefs.edit()
                .putString(KEY_PIN_HASH, pinHash)
                .putString(KEY_PIN_SALT, salt)
                .apply()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if PIN is already set up.
     * @return true if PIN is configured, false otherwise
     */
    fun isPinSetup(): Boolean {
        return encryptedPrefs.contains(KEY_PIN_HASH) && encryptedPrefs.contains(KEY_PIN_SALT)
    }

    /**
     * Checks if authentication is required (user is not authenticated).
     * @return true if authentication is required, false if user is authenticated
     */
    fun isAuthenticationRequired(): Boolean {
        return !_isAuthenticated.value
    }

    /**
     * Logs out the user by clearing authentication state.
     */
    fun logout() {
        _isAuthenticated.value = false
    }

    /**
     * Generates a cryptographically secure salt for PIN hashing.
     */
    private fun generateSalt(): String {
        val saltBytes = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(saltBytes)
        return android.util.Base64.encodeToString(saltBytes, android.util.Base64.NO_WRAP)
    }

    /**
     * Hashes a PIN with the provided salt using SHA-256.
     */
    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltBytes = android.util.Base64.decode(salt, android.util.Base64.NO_WRAP)
        val pinBytes = pin.toByteArray(Charsets.UTF_8)

        digest.update(saltBytes)
        digest.update(pinBytes)

        val hashedBytes = digest.digest()
        return android.util.Base64.encodeToString(hashedBytes, android.util.Base64.NO_WRAP)
    }

    /**
     * Increments the failed authentication attempts counter.
     */
    private fun incrementFailedAttempts() {
        val currentAttempts = encryptedPrefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        encryptedPrefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, currentAttempts + 1)
            .putLong(KEY_LAST_ATTEMPT_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Resets the failed authentication attempts counter.
     */
    private fun resetFailedAttempts() {
        encryptedPrefs.edit()
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LAST_ATTEMPT_TIME)
            .apply()
    }

    /**
     * Checks if the user is currently locked out due to too many failed attempts.
     */
    private fun isLockedOut(): Boolean {
        val failedAttempts = encryptedPrefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        if (failedAttempts < MAX_FAILED_ATTEMPTS) {
            return false
        }

        val lastAttemptTime = encryptedPrefs.getLong(KEY_LAST_ATTEMPT_TIME, 0)
        val currentTime = System.currentTimeMillis()

        return (currentTime - lastAttemptTime) < LOCKOUT_DURATION_MS
    }
}
