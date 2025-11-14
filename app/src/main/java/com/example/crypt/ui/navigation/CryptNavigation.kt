package com.example.crypt.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

/**
 * Main navigation component that handles routing between authentication and main app screens.
 * Implements conditional navigation based on authentication state as per requirements 1.1 and 1.3.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                is com.example.crypt.ui.viewmodel.NavigationEvent.NavigateToMain -> {
                    navController.navigate(MainNavigation.VAULT) {
                        popUpTo(AuthNavigation.AUTH) { inclusive = true }
                    }
                }
                is com.example.crypt.ui.viewmodel.NavigationEvent.ShowMessage -> {
                    // Handle message display if needed
                }
            }
        }
    }
    
    // Determine start destination based on authentication state
    val startDestination = if (authState is AuthState.Authenticated) {
        MainNavigation.VAULT
    } else {
        AuthNavigation.AUTH
    }
    
    when (authState) {
        is AuthState.Authenticated -> {
            // Show main app with bottom navigation
            MainAppNavigation(navController = navController, authViewModel = authViewModel)
        }
        else -> {
            // Show authentication screen
            NavHost(
                navController = navController,
                startDestination = AuthNavigation.AUTH
            ) {
                composable(AuthNavigation.AUTH) {
                    AuthScreenContainer(authViewModel = authViewModel)
                }
            }
        }
    }
}

/**
 * Main app navigation with bottom navigation bar for Generate and Vault screens.
 * Implements bottom navigation as per requirements 1.1 and 1.3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Only show bottom navigation for main screens (Generate and Vault)
            val showBottomBar = currentDestination?.route in listOf(
                MainNavigation.GENERATE,
                MainNavigation.VAULT
            )
            
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainNavigation.VAULT,
            modifier = Modifier.padding(innerPadding)
        ) {
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
                ViewEntryScreenWrapper(
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
                    entryId = null, // New entry
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSaveSuccess = {
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
                    onSaveSuccess = {
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

