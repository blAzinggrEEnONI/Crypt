package com.example.crypt.ui.screen

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.crypt.ui.viewmodel.GenerateViewModel

/**
 * Screen for generating passwords with customizable options.
 * Provides sliders for length configuration, toggles for character sets,
 * and functionality to save generated passwords to the vault.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    viewModel: GenerateViewModel = hiltViewModel(),
    onNavigateToVault: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var siteInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Password Generator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        // Generated Password Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Generated Password",
                    style = MaterialTheme.typography.titleMedium
                )
                
                SelectionContainer {
                    Text(
                        text = uiState.generatedPassword.ifEmpty { "Click Generate to create a password" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Generate Button
                    Button(
                        onClick = { viewModel.generatePassword() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate")
                    }
                    
                    // Copy Button
                    OutlinedButton(
                        onClick = { 
                            if (uiState.generatedPassword.isNotEmpty()) {
                                viewModel.copyPasswordToClipboard()
                                viewModel.showMessage("Password copied to clipboard")
                            }
                        },
                        enabled = uiState.generatedPassword.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy")
                    }
                    
                    // Save Button
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        enabled = uiState.generatedPassword.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
        
        // Configuration Options
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Password Options",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Length Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Length")
                        Text(
                            text = "${uiState.config.length}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    Slider(
                        value = uiState.config.length.toFloat(),
                        onValueChange = { viewModel.updateLength(it.toInt()) },
                        valueRange = 8f..64f,
                        steps = 55, // 64 - 8 - 1
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "8",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "64",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Character Set Options
                Text(
                    text = "Character Sets",
                    style = MaterialTheme.typography.titleSmall
                )
                
                // Uppercase Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Uppercase Letters")
                        Text(
                            text = "A-Z",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.config.includeUppercase,
                        onCheckedChange = { viewModel.updateIncludeUppercase(it) }
                    )
                }
                
                // Lowercase Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Lowercase Letters")
                        Text(
                            text = "a-z",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.config.includeLowercase,
                        onCheckedChange = { viewModel.updateIncludeLowercase(it) }
                    )
                }
                
                // Numbers Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Numbers")
                        Text(
                            text = "0-9",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.config.includeNumbers,
                        onCheckedChange = { viewModel.updateIncludeNumbers(it) }
                    )
                }
                
                // Symbols Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Symbols")
                        Text(
                            text = "!@#$%^&*()_+-=[]{}|;:,.<>?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.config.includeSymbols,
                        onCheckedChange = { viewModel.updateIncludeSymbols(it) }
                    )
                }
                
                HorizontalDivider()
                
                // Exclude Ambiguous Characters Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Exclude Ambiguous Characters")
                        Text(
                            text = "Excludes: 0, O, l, 1, I",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.config.excludeAmbiguous,
                        onCheckedChange = { viewModel.updateExcludeAmbiguous(it) }
                    )
                }
            }
        }
        
        // Error/Success Messages
        uiState.message?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = if (uiState.isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
        }
    }
    
    // Save to Vault Dialog
    if (showSaveDialog) {
        SavePasswordDialog(
            password = uiState.generatedPassword,
            site = siteInput,
            username = usernameInput,
            notes = notesInput,
            onSiteChange = { siteInput = it },
            onUsernameChange = { usernameInput = it },
            onNotesChange = { notesInput = it },
            onSave = { site, username, notes ->
                viewModel.savePassword(site, username, notes)
                showSaveDialog = false
                siteInput = ""
                usernameInput = ""
                notesInput = ""
            },
            onDismiss = { showSaveDialog = false },
            isSaving = uiState.isSaving
        )
    }
}

/**
 * Dialog for saving generated password to vault with site and username information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavePasswordDialog(
    password: String,
    site: String,
    username: String,
    notes: String,
    onSiteChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Save Password to Vault") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Password Preview
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = password,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
                
                // Site Input
                OutlinedTextField(
                    value = site,
                    onValueChange = onSiteChange,
                    label = { Text("Site/Service *") },
                    placeholder = { Text("example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true
                )
                
                // Username Input
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username/Email *") },
                    placeholder = { Text("user@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true
                )
                
                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Additional information...") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (site.isNotBlank() && username.isNotBlank()) {
                        onSave(site.trim(), username.trim(), notes.trim())
                    }
                },
                enabled = !isSaving && site.isNotBlank() && username.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}
