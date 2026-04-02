package com.family.tree.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import org.koin.compose.KoinApplication
import com.family.tree.core.di.coreModule
import com.family.tree.ui.di.uiModule
import org.koin.compose.KoinApplication
import com.family.tree.core.di.coreModule
import com.family.tree.ui.di.uiModule

@Composable
fun App() {
    KoinApplication(application = {
        modules(coreModule, uiModule)
    }) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                cafe.adriel.voyager.navigator.Navigator(MainWorkspaceScreen()) { navigator ->
                    cafe.adriel.voyager.transitions.SlideTransition(navigator)
                }
            }
        }
    }
}
