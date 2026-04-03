package com.family.tree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class AboutScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("About Family Tree Editor") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Family Tree Editor",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Typically you use a build config for the version
                Text(
                    text = "Version 1.0.0",
                    fontSize = 16.sp
                )
                
                Text(
                    text = "This program is free software.",
                    fontSize = 16.sp
                )
                
                Text(
                    text = "Built with Kotlin Multiplatform and Compose Multiplatform for Android, Desktop, iOS, and Web.",
                    fontSize = 16.sp
                )
                
                Text(
                    text = "Source code is available on GitHub:\nhttps://github.com/VladLerkin/family-tree-editor",
                    fontSize = 16.sp
                )
                
                Text(
                    text = "Author: domfindus@gmail.com",
                    fontSize = 16.sp
                )
            }
        }
    }
}
