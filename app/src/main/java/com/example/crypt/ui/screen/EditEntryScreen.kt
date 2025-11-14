package com.example.crypt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.crypt.ui.viewmodel.EntryViewModel
import com.example.crypt.ui.viewmodel.EditField

/**
 * Screen for creating and editing password entries.
 * Provides form validation, save/cancel operations, and password generation integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(
    entryId: Long? = null, // null for new entry, non-null for editing existing
    viewModel: EntryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToGenerate: () -> Unit = {},
    onSaveSuccess: (Long) -> Unit = {} // Called with entry ID after successful save
) {
    val editState by viewModel.editState.collectAsState()
    val focusManager = LocalFocusManager.current
    
    // Initialize edit mode when screen loads
    LaunchedEffect(entryId) {
        if (entryId != null) {
            // Load existing entry for editing
            viewModel.loadEntry(entryId)
            viewModel.startEditing()
        } else {
            // Start creating new entry
            viewModel.startCreating()
        }
    }
    
    // Handle save success
    LaunchedEffect(editState.saveSuccess) {
        if (editState.saveSuccess) {
            val savedId = editState.savedEntryId ?: entryId ?: 0L
            onSaveSuccess(savedId)
        }
    }
    
    // Unsaved changes dialog
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = if (editState.isNewEntry) "Add Password" else "Edit Password",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (editState.hasUnsavedChanges) {
                            showUnsavedChangesDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                // Save button
                TextButton(
                    onClick = { viewModel.saveEntry() },
                    enabled = !editState.isSaving
                ) {
                    if (editState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Save")
                }
            }
        )
        
        // Content Area
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                editState.error != null -> {
                    // Error State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
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
                                    text = editState.error ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Button(onClick = { viewModel.clearError() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
                
                editState.isEditing -> {
                    // Edit Form
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Site/Service Name Field
                        OutlinedTextField(
                            value = editState.site,
                            onValueChange = { viewModel.updateEditField(EditField.SITE, it) },
                            label = { Text("Website/Service") },
                            placeholder = { Text("e.g., google.com, GitHub") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isError = editState.validationErrors.containsKey(EditField.SITE),
                            supportingText = {
                                editState.validationErrors[EditField.SITE]?.let { error ->
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true
                        )
                        
                        // Username Field
                        OutlinedTextField(
                            value = editState.username,
                            onValueChange = { viewModel.updateEditField(EditField.USERNAME, it) },
                            label = { Text("Username/Email") },
                            placeholder = { Text("e.g., john@example.com") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isError = editState.validationErrors.containsKey(EditField.USERNAME),
                            supportingText = {
                                editState.validationErrors[EditField.USERNAME]?.let { error ->
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true
                        )
                        
                        // Password Field with Generator Integration
                        PasswordFieldWithGenerator(
                            password = editState.password,
                            onPasswordChange = { viewModel.updateEditField(EditField.PASSWORD, it) },
                            onGeneratePassword = onNavigateToGenerate,
                            isError = editState.validationErrors.containsKey(EditField.PASSWORD),
                            errorMessage = editState.validationErrors[EditField.PASSWORD],
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                        
                        // Notes Field (Optional)
                        OutlinedTextField(
                            value = editState.notes,
                            onValueChange = { viewModel.updateEditField(EditField.NOTES, it) },
                            label = { Text("Notes (Optional)") },
                            placeholder = { Text("Additional information...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            )
                        )
                        
                        // Action Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel Button
                            OutlinedButton(
                                onClick = {
                                    if (editState.hasUnsavedChanges) {
                                        showUnsavedChangesDialog = true
                                    } else {
                                        onNavigateBack()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !editState.isSaving
                            ) {
                                Text("Cancel")
                            }
                            
                            // Save Button
                            Button(
                                onClick = { viewModel.saveEntry() },
                                modifier = Modifier.weight(1f),
                                enabled = !editState.isSaving
                            ) {
                                if (editState.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (editState.isNewEntry) "Create" else "Update")
                            }
                        }
                        
                        // Help Text
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Security Tips",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "• Use the password generator for strong, unique passwords\n" +
                                          "• Avoid reusing passwords across different sites\n" +
                                          "• Your password will be encrypted before storage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Unsaved Changes Dialog
    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Unsaved Changes") },
            text = { 
                Text("You have unsaved changes. Are you sure you want to leave without saving?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedChangesDialog = false
                        viewModel.cancelEditing()
                        onNavigateBack()
                    }
                ) {
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }
}

/**
 * Password field with integrated password generator button.
 */
@Composable
private fun PasswordFieldWithGenerator(
    password: String,
    onPasswordChange: (String) -> Unit,
    onGeneratePassword: () -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    onNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Password Input Field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            placeholder = { Text("Enter or generate password") },
            leadingIcon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null
                )
            },
            trailingIcon = {
                Row {
                    // Toggle visibility button
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                    
                    // Generate password button
                    IconButton(onClick = onGeneratePassword) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Generate password",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            isError = isError,
            supportingText = {
                when {
                    errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    password.isNotEmpty() -> {
                        Text(
                            text = "Password strength: ${getPasswordStrength(password)}",
                            color = getPasswordStrengthColor(password)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { onNext() }
            ),
            singleLine = true
        )
        
        // Generate Password Button (Alternative)
        OutlinedButton(
            onClick = onGeneratePassword,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Strong Password")
        }
    }
}

/**
 * Simple password strength assessment.
 */
@Composable
private fun getPasswordStrength(password: String): String {
    val score = calculatePasswordScore(password)
    return when {
        score < 2 -> "Weak"
        score < 4 -> "Fair"
        score < 6 -> "Good"
        else -> "Strong"
    }
}

/**
 * Get color for password strength indicator.
 */
@Composable
private fun getPasswordStrengthColor(password: String): androidx.compose.ui.graphics.Color {
    val score = calculatePasswordScore(password)
    return when {
        score < 2 -> MaterialTheme.colorScheme.error
        score < 4 -> MaterialTheme.colorScheme.tertiary
        score < 6 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Calculate password strength score (0-8).
 */
private fun calculatePasswordScore(password: String): Int {
    var score = 0
    
    // Length scoring
    when {
        password.length >= 12 -> score += 2
        password.length >= 8 -> score += 1
    }
    
    // Character variety scoring
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    
    // Complexity bonus
    if (password.length >= 16 && score >= 5) score++
    if (password.length >= 20 && score >= 6) score++
    
    return score
}