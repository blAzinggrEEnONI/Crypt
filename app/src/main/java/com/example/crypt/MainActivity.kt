package com.example.crypt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.crypt.data.security.SecureMemoryManager
import com.example.crypt.ui.navigation.CryptNavigation
import com.example.crypt.ui.theme.CryptTheme
import com.example.crypt.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for the Crypt password manager application.
 * Configured with Hilt integration and Material3 design system as per requirements 1.1 and 1.3.
 * Implements navigation container and state management for authentication flow.
 * Includes memory dump protection for sensitive screens.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var secureMemoryManager: SecureMemoryManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable memory dump protection to prevent screenshots and screen recording
        secureMemoryManager.enableMemoryDumpProtection(this)
        
        enableEdgeToEdge()
        setContent {
            CryptApp()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Request garbage collection to help clear sensitive data
        secureMemoryManager.requestGarbageCollection()
    }
}

/**
 * Main app composable that sets up the theme and navigation.
 * Implements Material3 design system and navigation container as per requirements.
 */
@Composable
fun CryptApp() {
    val authViewModel: AuthViewModel = hiltViewModel()
    
    // Handle user interaction tracking for auto-lock functionality
    LaunchedEffect(Unit) {
        // This will be used to track user interactions for auto-lock
        // The actual interaction tracking will be handled by individual screens
    }
    
    CryptTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CryptNavigation(authViewModel = authViewModel)
        }
    }
}