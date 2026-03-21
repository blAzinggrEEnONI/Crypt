package com.example.crypt.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.crypt.domain.model.AuthState
import com.example.crypt.ui.screen.*
import com.example.crypt.ui.viewmodel.AuthViewModel
import com.example.crypt.ui.viewmodel.NavigationEvent

/**
 * Main navigation component that handles routing between authentication and main app screens.
 * Implements conditional navigation based on authentication state as per requirements 1.1 and 1.3.
 */
@Composable
fun CryptNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    // Handle navigation events from AuthViewModel
    LaunchedEffect(authViewModel) {
        authViewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToMain -> {
                    navController.navigate(MainNavigation.VAULT) {
                        popUpTo(AuthNavigation.AUTH) { inclusive = true }
                    }
                }
                is NavigationEvent.ShowMessage -> {
                    // Handle message display if needed
                }
            }
        }
    }

    // Navigate back to auth screen when the app is auto-locked or the user logs out
    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated || authState is AuthState.AuthError) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            // Only navigate to auth if we are currently on a main-app screen
            if (currentRoute != null && currentRoute != AuthNavigation.AUTH) {
                navController.navigate(AuthNavigation.AUTH) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Only show bottom navigation for main screens (Generate and Vault)
            // and only when authenticated
            val showBottomBar = authState is AuthState.Authenticated &&
                currentDestination?.route in listOf(MainNavigation.GENERATE, MainNavigation.VAULT)

            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Use a fixed start destination (AUTH) and rely on LaunchedEffect-driven navigation
        // to move to the main flow once authenticated. Changing startDestination dynamically
        // on an already-composed NavHost has no effect and causes subtle bugs.
        NavHost(
            navController = navController,
            startDestination = AuthNavigation.AUTH,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth Flow
            composable(AuthNavigation.AUTH) {
                AuthScreenContainer(authViewModel = authViewModel)
            }

            // Main App Flow
            composable(MainNavigation.GENERATE) {
                GenerateScreen(
                    onNavigateToVault = {
                        navController.navigate(MainNavigation.VAULT)
                    }
                )
            }

            composable(MainNavigation.VAULT) {
                VaultScreen(
                    onNavigateToEntry = { entryId ->
                        navController.navigate("${MainNavigation.VIEW_ENTRY}/$entryId")
                    },
                    onNavigateToAddEntry = {
                        navController.navigate(MainNavigation.EDIT_ENTRY)
                    }
                )
            }

            composable("${MainNavigation.VIEW_ENTRY}/{entryId}") { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId")?.toLongOrNull() ?: 0L
                ViewEntryScreen(
                    entryId = entryId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEdit = { id ->
                        navController.navigate("${MainNavigation.EDIT_ENTRY}/$id")
                    },
                    onEntryDeleted = {
                        navController.popBackStack()
                    }
                )
            }

            composable(MainNavigation.EDIT_ENTRY) {
                EditEntryScreen(
                    entryId = null,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToGenerate = {
                        navController.navigate(MainNavigation.GENERATE)
                    },
                    onSaveSuccess = { _ ->
                        navController.popBackStack()
                    }
                )
            }

            composable("${MainNavigation.EDIT_ENTRY}/{entryId}") { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId")?.toLongOrNull()
                EditEntryScreen(
                    entryId = entryId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToGenerate = {
                        navController.navigate(MainNavigation.GENERATE)
                    },
                    onSaveSuccess = { _ ->
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * Authentication navigation routes.
 */
object AuthNavigation {
    const val AUTH = "auth"
}

/**
 * Main app navigation routes.
 */
object MainNavigation {
    const val GENERATE = "generate"
    const val VAULT = "vault"
    const val VIEW_ENTRY = "view_entry"
    const val EDIT_ENTRY = "edit_entry"
}

/**
 * Bottom navigation item data class.
 */
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

/**
 * Bottom navigation items for Generate and Vault screens.
 */
private val bottomNavItems = listOf(
    BottomNavItem(
        route = MainNavigation.GENERATE,
        icon = Icons.Default.Settings,
        label = "Generate"
    ),
    BottomNavItem(
        route = MainNavigation.VAULT,
        icon = Icons.Default.Lock,
        label = "Vault"
    )
)

/**
 * Container for AuthScreen that connects it with AuthViewModel.
 */
@Composable
private fun AuthScreenContainer(
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val biometricAvailable by authViewModel.biometricAvailable.collectAsState()
    val isPinSetup by authViewModel.isPinSetup.collectAsState()

    AuthScreen(
        authState = authState,
        isBiometricAvailable = biometricAvailable,
        isPinSetup = isPinSetup,
        onBiometricAuthClick = {
            if (context is FragmentActivity) {
                authViewModel.authenticateWithBiometrics(context)
            }
        },
        onPinAuthClick = { pin ->
            authViewModel.authenticateWithPin(pin)
        },
        onSetupPinClick = { pin ->
            authViewModel.setupPin(pin)
        }
    )
}
