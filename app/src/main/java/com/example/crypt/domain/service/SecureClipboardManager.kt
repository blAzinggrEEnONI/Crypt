package com.example.crypt.domain.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing secure clipboard operations.
 * Handles copying sensitive data with sensitive flag and auto-clearing after timeout.
 */
@Singleton
class SecureClipboardManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val AUTO_CLEAR_DELAY_MS = 30_000L // 30 seconds
        private const val SENSITIVE_EXTRA = "android.content.extra.IS_SENSITIVE"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var clearJob: Job? = null
    
    /**
     * Copy text to clipboard with optional excessive security measures.
     * @param label User-visible label for the content
     * @param text The text to copy
     * @param isSensitive If true, marks content as sensitive and schedules auto-clear
     */
    fun copyToClipboard(label: String, text: String, isSensitive: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        
        if (isSensitive) {
            // Mark as sensitive for Android 13+ (API 33)
            if (Build.VERSION.SDK_INT >= 33) {
                clip.description.extras = PersistableBundle().apply {
                    putBoolean(SENSITIVE_EXTRA, true)
                }
            }
            scheduleAutoClear()
        }
        
        clipboard.setPrimaryClip(clip)
    }
    
    /**
     * Schedule the clipboard to be cleared after the delay.
     * Cancels any existing scheduled clear.
     */
    private fun scheduleAutoClear() {
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(AUTO_CLEAR_DELAY_MS)
            clearClipboard()
        }
    }
    
    /**
     * Immediately clear the clipboard.
     */
    fun clearClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // Clearing by setting empty clip
            // Note: On Android 12+, this might show a toast "Clipboard cleared" or similar app behavior
            if (Build.VERSION.SDK_INT >= 28) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        } catch (e: Exception) {
            // Ignore errors during clipboard clearing (might happen in background)
            e.printStackTrace()
        }
    }
}
