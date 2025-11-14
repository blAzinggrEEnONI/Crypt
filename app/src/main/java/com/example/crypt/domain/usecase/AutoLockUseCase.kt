package com.example.crypt.domain.usecase

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.crypt.domain.model.AppLockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for managing automatic app locking based on user inactivity and app lifecycle.
 * Implements 60-second inactivity timeout and immediate locking when app goes to background.
 */
@Singleton
class AutoLockUseCase @Inject constructor(
    private val authUseCase: AuthUseCase
) : DefaultLifecycleObserver {
    
    companion object {
        private const val INACTIVITY_TIMEOUT_MS = 60_000L // 60 seconds
        private const val COUNTDOWN_INTERVAL_MS = 1_000L // 1 second
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var inactivityJob: Job? = null
    private var countdownJob: Job? = null
    
    private val _appLockState = MutableStateFlow<AppLockState>(AppLockState.Locked)
    val appLockState: StateFlow<AppLockState> = _appLockState.asStateFlow()
    
    private var isAppInForeground = false
    private var lastActivityTime = System.currentTimeMillis()
    
    init {
        // Register lifecycle observer to monitor app state changes
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Start with locked state
        _appLockState.value = AppLockState.Locked
    }
    
    /**
     * Called when app moves to foreground.
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isAppInForeground = true
        
        // If user was authenticated before going to background, they need to re-authenticate
        if (_appLockState.value is AppLockState.Unlocked) {
            lockApp()
        }
    }
    
    /**
     * Called when app moves to background.
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInForeground = false
        
        // Immediately lock the app when it goes to background
        lockApp()
    }
    
    /**
     * Starts the inactivity timer after successful authentication.
     * Should be called when user successfully authenticates.
     */
    fun startInactivityTimer() {
        if (!authUseCase.isAuthenticated.value) {
            return
        }
        
        lastActivityTime = System.currentTimeMillis()
        _appLockState.value = AppLockState.Unlocked
        
        // Cancel any existing timer
        inactivityJob?.cancel()
        countdownJob?.cancel()
        
        // Start new inactivity timer
        inactivityJob = scope.launch {
            delay(INACTIVITY_TIMEOUT_MS)
            
            // Start countdown before locking
            startCountdown()
        }
    }
    
    /**
     * Resets the inactivity timer when user interacts with the app.
     * Should be called on any user interaction (touch, scroll, etc.).
     */
    fun resetInactivityTimer() {
        if (!authUseCase.isAuthenticated.value || !isAppInForeground) {
            return
        }
        
        lastActivityTime = System.currentTimeMillis()
        
        // If we're in countdown state, go back to unlocked
        if (_appLockState.value is AppLockState.AutoLocking) {
            _appLockState.value = AppLockState.Unlocked
        }
        
        // Restart the timer
        startInactivityTimer()
    }
    
    /**
     * Immediately locks the app and clears authentication state.
     */
    fun lockApp() {
        // Cancel any running timers
        inactivityJob?.cancel()
        countdownJob?.cancel()
        
        // Update state
        _appLockState.value = AppLockState.Locked
        
        // Clear authentication state
        authUseCase.logout()
    }
    
    /**
     * Called when user successfully authenticates.
     * Unlocks the app and starts the inactivity timer.
     */
    fun onAuthenticationSuccess() {
        if (isAppInForeground) {
            startInactivityTimer()
        } else {
            // If app is not in foreground, keep it locked
            lockApp()
        }
    }
    
    /**
     * Starts a countdown before automatically locking the app.
     * Gives user a chance to interact and reset the timer.
     */
    private fun startCountdown() {
        countdownJob = scope.launch {
            var remainingSeconds = 10 // 10 second countdown
            
            while (remainingSeconds > 0) {
                _appLockState.value = AppLockState.AutoLocking(remainingSeconds)
                delay(COUNTDOWN_INTERVAL_MS)
                remainingSeconds--
                
                // Check if user interacted during countdown
                val timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime
                if (timeSinceLastActivity < COUNTDOWN_INTERVAL_MS) {
                    // User interacted, cancel countdown and reset timer
                    _appLockState.value = AppLockState.Unlocked
                    startInactivityTimer()
                    return@launch
                }
            }
            
            // Countdown finished, lock the app
            lockApp()
        }
    }
    
    /**
     * Observes the current app lock state.
     * @return StateFlow of AppLockState
     */
    fun observeAppState(): StateFlow<AppLockState> = appLockState
    
    /**
     * Checks if the app is currently locked.
     * @return true if app is locked, false otherwise
     */
    fun isAppLocked(): Boolean {
        return _appLockState.value is AppLockState.Locked
    }
    
    /**
     * Gets the remaining time before auto-lock in milliseconds.
     * @return remaining time in milliseconds, or 0 if not applicable
     */
    fun getRemainingTimeMs(): Long {
        return when (val state = _appLockState.value) {
            is AppLockState.AutoLocking -> state.remainingSeconds * 1000L
            is AppLockState.Unlocked -> {
                val elapsed = System.currentTimeMillis() - lastActivityTime
                maxOf(0, INACTIVITY_TIMEOUT_MS - elapsed)
            }
            else -> 0L
        }
    }
    
    /**
     * Cleanup method to cancel all running jobs.
     * Should be called when the use case is no longer needed.
     */
    fun cleanup() {
        inactivityJob?.cancel()
        countdownJob?.cancel()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }
}