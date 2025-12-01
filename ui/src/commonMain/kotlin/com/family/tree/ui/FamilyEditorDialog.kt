package com.family.tree.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import com.family.tree.core.model.Family
import com.family.tree.core.model.FamilyId
import com.family.tree.core.model.Individual
import com.family.tree.core.model.IndividualId

@Composable
fun FamilyEditorDialog(
    family: Family,
    allIndividuals: List<Individual>,
    onSave: (updated: Family) -> Unit,
    onDismiss: () -> Unit
) {
    // Prepare lists
    val individualsById = remember(allIndividuals) { allIndividuals.associateBy { it.id } }
    val options = remember(allIndividuals) { allIndividuals.map { it.id to (it.displayName.ifBlank { it.id.value }) } }

    var expandedH by remember { mutableStateOf(false) }
    var expandedW by remember { mutableStateOf(false) }

    var husbandId by remember(family) { mutableStateOf(family.husbandId) }
    var wifeId by remember(family) { mutableStateOf(family.wifeId) }
    // Children picker state: keep explicit order (like legacy JavaFX)
    val selectedChildren = remember(family, allIndividuals) {
        mutableStateListOf<IndividualId>().also { lst ->
            family.childrenIds.forEach { lst.add(it) }
        }
    }
    var selectedAvailableId by remember { mutableStateOf<IndividualId?>(null) }
    var selectedChildId by remember { mutableStateOf<IndividualId?>(null) }
    var filterQuery by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit family ${family.id.value}") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Husband selector (optional)
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
                        // Option: none
                        DropdownMenuItem(text = { Text("— none —") }, onClick = { husbandId = null; expandedH = false })
                        options.forEach { (id, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                husbandId = id
                                expandedH = false
                            })
                        }
                    }
                }
                // Wife selector (optional)
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
                        // Option: none
                        DropdownMenuItem(text = { Text("— none —") }, onClick = { wifeId = null; expandedW = false })
                        options.forEach { (id, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                wifeId = id
                                expandedW = false
                            })
                        }
                    }
                }
                // Children selector (legacy-like): two lists with Add/Remove and Up/Down
                Text("Children")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Available list with simple filter and excluding husband/wife and already selected
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
                                val bg = if (selected) androidx.compose.ui.graphics.Color(0x201976D2) else androidx.compose.ui.graphics.Color.Unspecified
                                Row(
                                    Modifier.fillMaxWidth().clickable { selectedAvailableId = id }.then(Modifier),
                                ) {
                                    Text(label, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                    // Middle controls
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
                    // Selected list (ordered)
                    Column(Modifier.weight(1f)) {
                        LazyColumn {
                            items(selectedChildren) { id ->
                                val label = individualsById[id]?.displayName ?: id.value
                                val selected = (selectedChildId == id)
                                Row(
                                    Modifier.fillMaxWidth().clickable { selectedChildId = id }
                                ) {
                                    Text(label, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
                error?.let { Text(it, color = Color(0xFFD32F2F)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Формируем список детей из отмеченных чекбоксов
                val childIds = selectedChildren.toList()
                // Простейшая проверка: дети должны существовать в списке персон (из выпадающего списка — всегда так)
                if (childIds.any { it !in individualsById }) {
                    error = "Unknown child id in list"
                    return@TextButton
                }
                error = null
                onSave(
                    family.copy(
                        husbandId = husbandId,
                        wifeId = wifeId,
                        childrenIds = childIds
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
