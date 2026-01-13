package com.example.crypt.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.crypt.domain.model.AuthResult
import com.example.crypt.ui.viewmodel.EntryViewModel

import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen for viewing password entry details with secure password display.
 * Requires authentication before showing decrypted password.
 * Provides copy-to-clipboard functionality and edit/delete actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewEntryScreen(
    entryId: Long,
    viewModel: EntryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {},
    onEntryDeleted: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Load entry when screen is first displayed
    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }
    
    // Handle delete success
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onEntryDeleted()
        }
    }
    
    // Handle authentication requirement
    if (uiState.authenticationRequired) {
        LaunchedEffect(Unit) {
            try {
                val activity = context as FragmentActivity
                val result = if (viewModel.isBiometricAvailable()) {
                    viewModel.authenticateWithBiometrics(activity)
                } else {
                    // For this implementation, we'll assume PIN authentication is handled elsewhere
                    // In a real app, you'd show a PIN input dialog here
                    AuthResult.BiometricUnavailable
                }
                viewModel.handleAuthenticationResult(result)
            } catch (e: Exception) {
                viewModel.handleAuthenticationResult(
                    AuthResult.Error("Authentication failed", com.example.crypt.domain.model.AuthErrorCode.UNKNOWN_ERROR)
                )
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = uiState.entry?.site ?: "Password Entry",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                // Edit button
                IconButton(
                    onClick = { 
                        uiState.entry?.let { onNavigateToEdit(it.id) }
                    },
                    enabled = uiState.entry != null && !uiState.isLoading
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit entry"
                    )
                }
                
                // Delete button
                var showDeleteDialog by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = uiState.entry != null && !uiState.isLoading && !uiState.isDeleting
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                
                // Delete confirmation dialog
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Password Entry") },
                        text = { 
                            Text("Are you sure you want to delete this password entry? This action cannot be undone.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    viewModel.deleteEntry()
                                }
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        )
        
        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    // Loading State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading entry...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                uiState.error != null -> {
                    // Error State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Error",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = uiState.error ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(onClick = onNavigateBack) {
                                        Text("Go Back")
                                    }
                                    Button(onClick = { viewModel.clearError() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                }
                
                uiState.entry != null -> {
                    // Entry Details
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Site Information Card
                        val entry = uiState.entry
                        if (entry != null) {
                            EntryDetailCard(
                                title = "Website/Service",
                                content = entry.site,
                                icon = Icons.Default.Home
                            )
                            
                            // Username Card with Copy Button
                            EntryDetailCard(
                                title = "Username",
                                content = entry.username,
                                icon = Icons.Default.Person,
                                onCopy = {
                                    viewModel.copyUsernameToClipboard()
                                    android.widget.Toast.makeText(context, "Username copied", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        
                        // Password Card with Authentication Gate
                        PasswordCard(
                            isPasswordVisible = uiState.isPasswordVisible,
                            decryptedPassword = uiState.decryptedPassword,
                            isAuthenticating = uiState.isAuthenticating,
                            onToggleVisibility = { viewModel.togglePasswordVisibility() },
                            onCopy = {
                                viewModel.copyPasswordToClipboard()
                                android.widget.Toast.makeText(context, "Password copied", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        
                            // Notes Card (if notes exist)
                            if (!entry.notes.isNullOrEmpty() || uiState.decryptedNotes != null) {
                                NotesCard(
                                    encryptedNotes = entry.notes,
                                    decryptedNotes = uiState.decryptedNotes,
                                    isPasswordVisible = uiState.isPasswordVisible
                                )
                            }
                            
                            // Metadata Card
                            MetadataCard(
                                createdAt = entry.createdAt,
                                updatedAt = entry.updatedAt
                            )
                        }
                    }
                }
            }
            
            // Delete loading overlay
            if (uiState.isDeleting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Deleting entry...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable card component for displaying entry details.
 */
@Composable
private fun EntryDetailCard(
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onCopy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title row with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Content row with copy button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionContainer {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (onCopy != null) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Copy $title",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Specialized card for password display with authentication gate.
 */
@Composable
private fun PasswordCard(
    isPasswordVisible: Boolean,
    decryptedPassword: String?,
    isAuthenticating: Boolean,
    onToggleVisibility: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Password content
            when {
                isAuthenticating -> {
                    // Authenticating state
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(
                            text = "Authenticating...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                decryptedPassword != null -> {
                    // Password decrypted and available
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SelectionContainer {
                            Text(
                                text = if (isPasswordVisible) decryptedPassword else "••••••••••••",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row {
                            // Toggle visibility button
                            IconButton(onClick = onToggleVisibility) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Copy button
                            IconButton(onClick = onCopy) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Copy password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                else -> {
                    // Authentication required
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "••••••••••••",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Button(
                            onClick = onToggleVisibility,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authenticate")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card for displaying encrypted notes.
 */
@Composable
private fun NotesCard(
    encryptedNotes: String?,
    decryptedNotes: String?,
    isPasswordVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Notes content
            SelectionContainer {
                Text(
                    text = if (isPasswordVisible && decryptedNotes != null) {
                        decryptedNotes
                    } else if (!encryptedNotes.isNullOrEmpty()) {
                        "••••••••••••"
                    } else {
                        "No notes"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPasswordVisible && decryptedNotes != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Card for displaying entry metadata (creation and update timestamps).
 */
@Composable
private fun MetadataCard(
    createdAt: Long,
    updatedAt: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Entry Information",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Created",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(createdAt),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (updatedAt != createdAt) {
                    Column {
                        Text(
                            text = "Updated",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTimestamp(updatedAt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}



/**
 * Format timestamp to readable date and time string.
 */
private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}