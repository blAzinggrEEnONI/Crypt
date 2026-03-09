package com.example.crypt.domain.service

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Singleton

/**
 * Centralized logging service with structured logging support.
 * Provides consistent log formatting and severity levels across the application.
 */
@Singleton
object CryptLogger {
    
    // Log levels
    enum class Level {
        VERBOSE, DEBUG, INFO, WARNING, ERROR
    }
    
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var minLevel = Level.DEBUG
    
    /**
     * Sets the minimum log level for production filtering.
     * By default, DEBUG level is used.
     */
    fun setMinimumLevel(level: Level) {
        minLevel = level
    }
    
    /**
     * Logs a verbose message.
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (Level.VERBOSE >= minLevel) {
            Log.v(tag, formatMessage(message), throwable)
        }
    }
    
    /**
     * Logs a debug message.
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (Level.DEBUG >= minLevel) {
            Log.d(tag, formatMessage(message), throwable)
        }
    }
    
    /**
     * Logs an info message.
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (Level.INFO >= minLevel) {
            Log.i(tag, formatMessage(message), throwable)
        }
    }
    
    /**
     * Logs a warning message.
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (Level.WARNING >= minLevel) {
            Log.w(tag, formatMessage(message), throwable)
        }
    }
    
    /**
     * Logs an error message.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (Level.ERROR >= minLevel) {
            Log.e(tag, formatMessage(message), throwable)
        }
    }
    
    /**
     * Logs performance metrics.
     */
    fun logPerformance(tag: String, operation: String, durationMs: Long) {
        d(tag, "PERF: $operation completed in ${durationMs}ms")
    }
    
    /**
     * Logs security-related events.
     */
    fun logSecurityEvent(tag: String, event: String, level: Level = Level.INFO) {
        when (level) {
            Level.VERBOSE -> v(tag, "SECURITY: $event")
            Level.DEBUG -> d(tag, "SECURITY: $event")
            Level.INFO -> i(tag, "SECURITY: $event")
            Level.WARNING -> w(tag, "SECURITY: $event")
            Level.ERROR -> e(tag, "SECURITY: $event")
        }
    }
    
    /**
     * Logs authentication events.
     */
    fun logAuthEvent(tag: String, event: String, success: Boolean) {
        val status = if (success) "SUCCESS" else "FAILED"
        i(tag, "AUTH: $event - $status")
    }
    
    /**
     * Logs database operations.
     */
    fun logDbOperation(tag: String, operation: String, affectedRows: Int? = null) {
        val suffix = affectedRows?.let { " (rows affected: $it)" } ?: ""
        d(tag, "DB: $operation$suffix")
    }
    
    /**
     * Formats a log message with timestamp and thread info.
     */
    private fun formatMessage(message: String): String {
        val timestamp = dateFormat.format(Date())
        val thread = Thread.currentThread().name
        return "[$timestamp] [$thread] $message"
    }
}

/**
 * Extension function for convenient logging from any class.
 * Usage: logDebug("Message") instead of CryptLogger.d("ClassName", "Message")
 */
inline fun <reified T> T.logDebug(message: String) {
    CryptLogger.d(T::class.java.simpleName, message)
}

inline fun <reified T> T.logInfo(message: String) {
    CryptLogger.i(T::class.java.simpleName, message)
}

inline fun <reified T> T.logWarning(message: String) {
    CryptLogger.w(T::class.java.simpleName, message)
}

inline fun <reified T> T.logError(message: String, throwable: Throwable? = null) {
    CryptLogger.e(T::class.java.simpleName, message, throwable)
}
