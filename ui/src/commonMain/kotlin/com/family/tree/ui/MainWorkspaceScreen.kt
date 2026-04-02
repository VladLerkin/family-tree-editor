package com.family.tree.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class MainWorkspaceScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinInject<MainViewModel>()
        val state by viewModel.state.collectAsState()
        
        // Handle global navigation events cleanly
        LaunchedEffect(state.activeDialog) {
            // We've reverted to using standard popups for most dialogs as per user preference.
            // If any future feature requires a true full-screen navigation (e.g., a secondary dashboard),
            // it can be handled here.
        }
        
        // Host the existing main screen
        MainScreen()
    }
}
