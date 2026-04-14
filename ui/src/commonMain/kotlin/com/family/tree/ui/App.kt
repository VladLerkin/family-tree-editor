package com.family.tree.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.family.tree.core.di.coreModule
import com.family.tree.ui.di.uiModule
import org.koin.compose.KoinApplication





@Composable
fun App() {
    @Suppress("DEPRECATION")
    KoinApplication(application = {
        modules(coreModule, uiModule)
    }) {




        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Navigator(MainWorkspaceScreen()) { navigator ->
                    SlideTransition(navigator)
                }
            }
        }
    }
}
