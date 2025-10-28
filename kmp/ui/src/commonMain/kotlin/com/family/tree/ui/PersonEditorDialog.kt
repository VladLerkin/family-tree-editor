package com.family.tree.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.family.tree.core.model.Individual

@Composable
fun PersonEditorDialog(
    person: Individual,
    onSave: (updated: Individual) -> Unit,
    onDismiss: () -> Unit
) {
    var first by remember(person) { mutableStateOf(person.firstName ?: "") }
    var last by remember(person) { mutableStateOf(person.lastName ?: "") }
    var birth by remember(person) { mutableStateOf(person.birthYear?.toString() ?: "") }
    var death by remember(person) { mutableStateOf(person.deathYear?.toString() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit person") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = first, onValueChange = { first = it }, label = { Text("First name") }, singleLine = true)
                OutlinedTextField(value = last, onValueChange = { last = it }, label = { Text("Last name") }, singleLine = true)
                OutlinedTextField(value = birth, onValueChange = { birth = it.filter { ch -> ch.isDigit() } }, label = { Text("Birth year") }, singleLine = true)
                OutlinedTextField(value = death, onValueChange = { death = it.filter { ch -> ch.isDigit() } }, label = { Text("Death year") }, singleLine = true)
                error?.let { Text(it, color = Color(0xFFD32F2F)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val b = birth.toIntOrNull()
                val d = death.toIntOrNull()
                if (birth.isNotBlank() && b == null) { error = "Birth year must be number"; return@TextButton }
                if (death.isNotBlank() && d == null) { error = "Death year must be number"; return@TextButton }
                if (b != null && (b < 0 || b > 3000)) { error = "Birth year out of range"; return@TextButton }
                if (d != null && (d < 0 || d > 3000)) { error = "Death year out of range"; return@TextButton }
                if (b != null && d != null && d < b) { error = "Death year < birth year"; return@TextButton }
                error = null
                onSave(
                    person.copy(
                        firstName = first.ifBlank { "" },
                        lastName = last.ifBlank { "" },
                        birthYear = b,
                        deathYear = d
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
