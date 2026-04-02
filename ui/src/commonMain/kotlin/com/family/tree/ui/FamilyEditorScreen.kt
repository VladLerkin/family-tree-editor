package com.family.tree.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.family.tree.core.model.FamilyId
import com.family.tree.core.model.IndividualId
import org.koin.compose.koinInject

data class FamilyEditorScreen(val familyId: FamilyId) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinInject<MainViewModel>()
        val state by viewModel.state.collectAsState()
        val project = state.project
        val family = project.families.find { it.id == familyId } ?: return
        val allIndividuals = project.individuals

        val individualsById = remember(allIndividuals) { allIndividuals.associateBy { it.id } }
        val options = remember(allIndividuals) { allIndividuals.map { it.id to (it.displayName.ifBlank { it.id.value }) } }

        var expandedH by remember { mutableStateOf(false) }
        var expandedW by remember { mutableStateOf(false) }

        var husbandId by remember(family) { mutableStateOf(family.husbandId) }
        var wifeId by remember(family) { mutableStateOf(family.wifeId) }
        val selectedChildren = remember(family, allIndividuals) {
            mutableStateListOf<IndividualId>().also { lst ->
                family.childrenIds.forEach { lst.add(it) }
            }
        }
        var selectedAvailableId by remember { mutableStateOf<IndividualId?>(null) }
        var selectedChildId by remember { mutableStateOf<IndividualId?>(null) }
        var filterQuery by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit family ${family.id.value}") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val childIds = selectedChildren.toList()
                            if (childIds.any { it !in individualsById }) {
                                error = "Unknown child id in list"
                                return@IconButton
                            }
                            error = null
                            val updated = family.copy(
                                husbandId = husbandId,
                                wifeId = wifeId,
                                childrenIds = childIds
                            )
                            val idx = project.families.indexOfFirst { it.id == updated.id }
                            if (idx >= 0) {
                                val newFamilies = project.families.toMutableList().apply { this[idx] = updated }
                                viewModel.updateProjectInfo(project.copy(families = newFamilies))
                            }
                            navigator.pop()
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Husband selector
                androidx.compose.foundation.layout.Box {
                    val currentHText = husbandId?.let { individualsById[it]?.displayName ?: it.value } ?: "—"
                    OutlinedTextField(
                        value = currentHText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Husband") },
                        modifier = Modifier.fillMaxWidth().clickable { expandedH = true }
                    )
                    DropdownMenu(expanded = expandedH, onDismissRequest = { expandedH = false }) {
                        DropdownMenuItem(text = { Text("— none —") }, onClick = { husbandId = null; expandedH = false })
                        options.forEach { (id, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { husbandId = id; expandedH = false })
                        }
                    }
                }
                // Wife selector
                androidx.compose.foundation.layout.Box {
                    val currentWText = wifeId?.let { individualsById[it]?.displayName ?: it.value } ?: "—"
                    OutlinedTextField(
                        value = currentWText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wife") },
                        modifier = Modifier.fillMaxWidth().clickable { expandedW = true }
                    )
                    DropdownMenu(expanded = expandedW, onDismissRequest = { expandedW = false }) {
                        DropdownMenuItem(text = { Text("— none —") }, onClick = { wifeId = null; expandedW = false })
                        options.forEach { (id, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { wifeId = id; expandedW = false })
                        }
                    }
                }
                
                Text("Children", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            label = { Text("Filter") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyColumn {
                            val filtered = options.filter { (_, label) ->
                                filterQuery.isBlank() || label.contains(filterQuery, ignoreCase = true)
                            }.filter { (id, _) -> id != husbandId && id != wifeId && id !in selectedChildren }
                            items(filtered) { (id, label) ->
                                val selected = (selectedAvailableId == id)
                                val bg = if (selected) Color(0x201976D2) else Color.Transparent
                                Row(Modifier.fillMaxWidth().clickable { selectedAvailableId = id }.padding(8.dp)) {
                                    Text(label, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            selectedAvailableId?.let { id ->
                                if (id != husbandId && id != wifeId && id !in selectedChildren) {
                                    selectedChildren.add(id)
                                }
                            }
                        }) { Text("> Add") }
                        TextButton(onClick = {
                            selectedChildId?.let { id -> selectedChildren.remove(id) }
                        }) { Text("< Remove") }
                        TextButton(onClick = {
                            selectedChildId?.let { id ->
                                val idx = selectedChildren.indexOf(id)
                                if (idx > 0) {
                                    selectedChildren.removeAt(idx)
                                    selectedChildren.add(idx - 1, id)
                                }
                            }
                        }) { Text("Up") }
                        TextButton(onClick = {
                            selectedChildId?.let { id ->
                                val idx = selectedChildren.indexOf(id)
                                if (idx >= 0 && idx < selectedChildren.size - 1) {
                                    selectedChildren.removeAt(idx)
                                    selectedChildren.add(idx + 1, id)
                                }
                            }
                        }) { Text("Down") }
                    }
                    Column(Modifier.weight(1f)) {
                        LazyColumn {
                            items(selectedChildren) { id ->
                                val label = individualsById[id]?.displayName ?: id.value
                                val selected = (selectedChildId == id)
                                val bg = if (selected) Color(0x201976D2) else Color.Transparent
                                Row(Modifier.fillMaxWidth().clickable { selectedChildId = id }.padding(8.dp)) {
                                    Text(label, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
                error?.let { Text(it, color = Color(0xFFD32F2F)) }
            }
        }
    }
}
