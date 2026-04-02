package com.family.tree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.family.tree.core.model.Gender
import com.family.tree.core.model.IndividualId
import org.koin.compose.koinInject

data class PersonEditorScreen(val personId: IndividualId) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinInject<MainViewModel>()
        val state by viewModel.state.collectAsState()
        val project = state.project
        val person = project.individuals.find { it.id == personId } ?: return
        
        var first by remember(person) { mutableStateOf(person.firstName) }
        var last by remember(person) { mutableStateOf(person.lastName) }
        var gender by remember(person) { mutableStateOf(person.gender ?: Gender.UNKNOWN) }
        var genderExpanded by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Person") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val updated = person.copy(firstName = first, lastName = last, gender = gender)
                            val idx = project.individuals.indexOfFirst { it.id == updated.id }
                            if (idx >= 0) {
                                val newIndividuals = project.individuals.toMutableList().apply { this[idx] = updated }
                                viewModel.updateProjectInfo(project.copy(individuals = newIndividuals))
                            }
                            navigator.pop()
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp) // Adjusted layout width for desktop gracefully
                ) {
                    OutlinedTextField(
                        value = first,
                        onValueChange = { first = it },
                        label = { Text("First name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = last,
                        onValueChange = { last = it },
                        label = { Text("Last name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = when (gender) {
                                Gender.MALE -> "Male"
                                Gender.FEMALE -> "Female"
                                Gender.UNKNOWN -> "Unknown"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("Male") }, onClick = { gender = Gender.MALE; genderExpanded = false })
                            DropdownMenuItem(text = { Text("Female") }, onClick = { gender = Gender.FEMALE; genderExpanded = false })
                            DropdownMenuItem(text = { Text("Unknown") }, onClick = { gender = Gender.UNKNOWN; genderExpanded = false })
                        }
                    }
                }
            }
        }
    }
}
