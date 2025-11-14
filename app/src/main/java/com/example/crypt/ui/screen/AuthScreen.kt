package com.example.crypt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.crypt.domain.model.AuthState
import com.example.crypt.domain.usecase.AuthUseCase
import com.example.crypt.ui.theme.CryptTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * Authentication screen that handles both biometric and PIN authentication.
 * Displays biometric prompt integration and secure PIN input UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authUseCase: AuthUseCase, // Pass AuthUseCase to handle biometric prompt
    authState: AuthState,
    isBiometricAvailable: Boolean,
    isPinSetup: Boolean,
    onBiometricAuthClick: () -> Unit,
    onPinAuthClick: (String) -> Unit,
    onSetupPinClick: (String) -> Unit,
    onBiometricAuthResult: (com.example.crypt.domain.model.AuthResult) -> Unit, // Callback for result
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var pinText by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSettingUpPin by remember { mutableStateOf(false) }
    var confirmPinText by remember { mutableStateOf("") }

    val pinFocusRequester = remember { FocusRequester() }
    val confirmPinFocusRequester = remember { FocusRequester() }

    // Reset PIN setup mode when auth state changes
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            isSettingUpPin = false
            pinText = ""
            confirmPinText = ""
        }
    }

    // Handle Biometric Authentication
    LaunchedEffect(Unit) {
        authUseCase.biometricAuthEvent.collectLatest { 
            val activity = context as? FragmentActivity
            if (activity != null) {
                val result = authUseCase.authenticateWithBiometrics(activity)
                onBiometricAuthResult(result)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo and Title
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Crypt Logo",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Crypt",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Secure Password Manager",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Authentication Content
        when {
            !isPinSetup -> {
                // PIN Setup Flow
                PinSetupContent(
                    pinText = pinText,
                    confirmPinText = confirmPinText,
                    isPasswordVisible = isPasswordVisible,
                    authState = authState,
                    pinFocusRequester = pinFocusRequester,
                    confirmPinFocusRequester = confirmPinFocusRequester,
                    onPinTextChange = { pinText = it },
                    onConfirmPinTextChange = { confirmPinText = it },
                    onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                    onSetupPin = { pin ->
                        onSetupPinClick(pin)
                        keyboardController?.hide()
                    }
                )
            }
            else -> {
                // Authentication Flow
                AuthenticationContent(
                    pinText = pinText,
                    isPasswordVisible = isPasswordVisible,
                    authState = authState,
                    isBiometricAvailable = isBiometricAvailable,
                    pinFocusRequester = pinFocusRequester,
                    onPinTextChange = { pinText = it },
                    onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                    onBiometricAuth = onBiometricAuthClick,
                    onPinAuth = { pin ->
                        onPinAuthClick(pin)
                        keyboardController?.hide()
                    }
                )
            }
        }

        // Error Message
        if (authState is AuthState.AuthError) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = authState.message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PinSetupContent(
    pinText: String,
    confirmPinText: String,
    isPasswordVisible: Boolean,
    authState: AuthState,
    pinFocusRequester: FocusRequester,
    confirmPinFocusRequester: FocusRequester,
    onPinTextChange: (String) -> Unit,
    onConfirmPinTextChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSetupPin: (String) -> Unit
) {
    Text(
        text = "Set up your PIN",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )

    Text(
        text = "Create a 4-digit PIN to secure your password vault",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    // PIN Input
    OutlinedTextField(
        value = pinText,
        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) onPinTextChange(it) },
        label = { Text("Enter PIN") },
        placeholder = { Text("4-digit PIN") },
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { confirmPinFocusRequester.requestFocus() }
        ),
        trailingIcon = {
            IconButton(onClick = onPasswordVisibilityToggle) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = if (isPasswordVisible) "Hide PIN" else "Show PIN"
                )
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(pinFocusRequester)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Confirm PIN Input
    OutlinedTextField(
        value = confirmPinText,
        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) onConfirmPinTextChange(it) },
        label = { Text("Confirm PIN") },
        placeholder = { Text("Re-enter PIN") },
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (pinText.length == 4 && pinText == confirmPinText) {
                    onSetupPin(pinText)
                }
            }
        ),
        isError = confirmPinText.isNotEmpty() && pinText != confirmPinText,
        supportingText = {
            if (confirmPinText.isNotEmpty() && pinText != confirmPinText) {
                Text(
                    text = "PINs do not match",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(confirmPinFocusRequester)
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Setup PIN Button
    Button(
        onClick = { onSetupPin(pinText) },
        enabled = pinText.length == 4 && pinText == confirmPinText && authState !is AuthState.Authenticating,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (authState is AuthState.Authenticating) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text("Set up PIN")
    }
}

@Composable
private fun AuthenticationContent(
    pinText: String,
    isPasswordVisible: Boolean,
    authState: AuthState,
    isBiometricAvailable: Boolean,
    pinFocusRequester: FocusRequester,
    onPinTextChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onBiometricAuth: () -> Unit,
    onPinAuth: (String) -> Unit
) {
    Text(
        text = "Welcome back",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )

    Text(
        text = "Authenticate to access your password vault",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Biometric Authentication Button
    if (isBiometricAvailable) {
        Button(
            onClick = onBiometricAuth,
            enabled = authState !is AuthState.Authenticating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Biometric Authentication"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use Biometric")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "or",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // PIN Authentication
    OutlinedTextField(
        value = pinText,
        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) onPinTextChange(it) },
        label = { Text("Enter PIN") },
        placeholder = { Text("4-digit PIN") },
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (pinText.length == 4) {
                    onPinAuth(pinText)
                }
            }
        ),
        trailingIcon = {
            IconButton(onClick = onPasswordVisibilityToggle) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = if (isPasswordVisible) "Hide PIN" else "Show PIN"
                )
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(pinFocusRequester)
    )

    Spacer(modifier = Modifier.height(24.dp))

    // PIN Authentication Button
    Button(
        onClick = { onPinAuth(pinText) },
        enabled = pinText.length == 4 && authState !is AuthState.Authenticating,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (authState is AuthState.Authenticating) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text("Unlock with PIN")
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    CryptTheme {
        // This preview will not work as intended due to the AuthUseCase dependency
//        AuthScreen(
//            authUseCase = TODO(),
//            authState = AuthState.Unauthenticated,
//            isBiometricAvailable = true,
//            isPinSetup = true,
//            onBiometricAuthClick = {},
//            onPinAuthClick = {},
//            onSetupPinClick = {},
//            onBiometricAuthResult = {}
//        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPinSetupPreview() {
    CryptTheme {
        // This preview will not work as intended due to the AuthUseCase dependency
//        AuthScreen(
//            authUseCase = TODO(),
//            authState = AuthState.Unauthenticated,
//            isBiometricAvailable = true,
//            isPinSetup = false,
//            onBiometricAuthClick = {},
//            onPinAuthClick = {},
//            onSetupPinClick = {},
//            onBiometricAuthResult = {}
//        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenErrorPreview() {
    CryptTheme {
        // This preview will not work as intended due to the AuthUseCase dependency
//        AuthScreen(
//            authUseCase = TODO(),
//            authState = AuthState.AuthError("Incorrect PIN. Please try again."),
//            isBiometricAvailable = false,
//            isPinSetup = true,
//            onBiometricAuthClick = {},
//            onPinAuthClick = {},
//            onSetupPinClick = {},
//            onBiometricAuthResult = {}
//        )
    }
}
