package com.family.tree.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.family.tree.core.ProjectData
import com.family.tree.core.model.Source
import com.family.tree.core.model.SourceId

@Composable
fun SourcesManagerDialog(
    project: ProjectData,
    onDismiss: () -> Unit,
    onUpdateProject: (ProjectData) -> Unit
) {
    var sources by remember { mutableStateOf(project.sources.sortedBy { it.title.lowercase() }) }
    var selectedSourceIndex by remember { mutableStateOf<Int?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<Source?>(null) }
    var isAddMode by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sources") },
        text = {
            Column(
                modifier = Modifier.width(780.dp).height(500.dp)
            ) {
                // Sources list
                if (sources.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No sources",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(sources.size) { index ->
                            val source = sources[index]
                            val isSelected = selectedSourceIndex == index
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                    .clickable { selectedSourceIndex = if (isSelected) null else index }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = source.title.takeIf { it.isNotBlank() } ?: "(no title)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (source.abbreviation.isNotBlank() || source.agency.isNotBlank() || source.callNumber.isNotBlank()) {
                                        val details = listOfNotNull(
                                            source.abbreviation.takeIf { it.isNotBlank() },
                                            source.agency.takeIf { it.isNotBlank() },
                                            source.callNumber.takeIf { it.isNotBlank() }
                                        ).joinToString(" • ")
                                        Text(
                                            text = details,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isAddMode = true
                            editingSource = Source(id = SourceId.generate(), title = "")
                            showEditDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                    
                    Button(
                        onClick = {
                            val index = selectedSourceIndex
                            if (index != null && index < sources.size) {
                                isAddMode = false
                                editingSource = sources[index]
                                showEditDialog = true
                            }
                        },
                        enabled = selectedSourceIndex != null
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                    
                    Button(
                        onClick = {
                            val index = selectedSourceIndex
                            if (index != null && index < sources.size) {
                                val updatedSources = sources.toMutableList().also { it.removeAt(index) }
                                sources = updatedSources.sortedBy { it.title.lowercase() }
                                val updatedProject = project.copy(sources = updatedSources)
                                onUpdateProject(updatedProject)
                                selectedSourceIndex = null
                            }
                        },
                        enabled = selectedSourceIndex != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                        Spacer(Modifier.width(4.dp))
                        Text("Remove")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
    
    // Edit dialog
    if (showEditDialog && editingSource != null) {
        SourceEditDialog(
            source = editingSource!!,
            isAddMode = isAddMode,
            onConfirm = { updatedSource ->
                val updatedSources = if (isAddMode) {
                    sources + updatedSource
                } else {
                    sources.map { if (it.id == updatedSource.id) updatedSource else it }
                }
                sources = updatedSources.sortedBy { it.title.lowercase() }
                val updatedProject = project.copy(sources = updatedSources)
                onUpdateProject(updatedProject)
                showEditDialog = false
                editingSource = null
            },
            onDismiss = {
                showEditDialog = false
                editingSource = null
            }
        )
    }
}

@Composable
private fun SourceEditDialog(
    source: Source,
    isAddMode: Boolean,
    onConfirm: (Source) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(source.title) }
    var abbreviation by remember { mutableStateOf(source.abbreviation) }
    var agency by remember { mutableStateOf(source.agency) }
    var callNumber by remember { mutableStateOf(source.callNumber) }
    var text by remember { mutableStateOf(source.text) }
    var publicationFacts by remember { mutableStateOf(source.publicationFacts) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAddMode) "Add Source" else "Edit Source") },
        text = {
            Column(
                modifier = Modifier
                    .width(780.dp)
                    .height(400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = abbreviation,
                    onValueChange = { abbreviation = it },
                    label = { Text("Abbreviation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = agency,
                    onValueChange = { agency = it },
                    label = { Text("Agency") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = callNumber,
                    onValueChange = { callNumber = it },
                    label = { Text("Call Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = publicationFacts,
                    onValueChange = { publicationFacts = it },
                    label = { Text("Publication Facts") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isBlank()) {
                        // Don't allow empty title
                        return@TextButton
                    }
                    val updatedSource = source.copy(
                        title = title.trim(),
                        abbreviation = abbreviation.trim(),
                        agency = agency.trim(),
                        callNumber = callNumber.trim(),
                        publicationFacts = publicationFacts.trim(),
                        text = text.trim()
                    )
                    onConfirm(updatedSource)
                },
                enabled = title.isNotBlank()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
