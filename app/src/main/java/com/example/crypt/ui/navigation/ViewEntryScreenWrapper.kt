package com.example.crypt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.crypt.domain.usecase.AuthUseCase
import com.example.crypt.ui.screen.ViewEntryScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Wrapper for ViewEntryScreen that provides the required AuthUseCase dependency.
 * This is needed because ViewEntryScreen requires AuthUseCase as a parameter.
 */
@Composable
fun ViewEntryScreenWrapper(
    entryId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onEntryDeleted: () -> Unit
) {
    val viewModel: ViewEntryWrapperViewModel = hiltViewModel()
    
    ViewEntryScreen(
        entryId = entryId,
        authUseCase = viewModel.authUseCase,
        onNavigateBack = onNavigateBack,
        onNavigateToEdit = onNavigateToEdit,
        onEntryDeleted = onEntryDeleted
    )
}

/**
 * ViewModel that provides AuthUseCase for ViewEntryScreen.
 */
@HiltViewModel
class ViewEntryWrapperViewModel @Inject constructor(
    val authUseCase: AuthUseCase
) : androidx.lifecycle.ViewModel()