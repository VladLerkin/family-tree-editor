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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.family.tree.core.model.*

@Composable
fun PropertiesInspector(
    individual: Individual,
    project: ProjectData,
    onUpdateIndividual: (Individual) -> Unit
) {
    val scrollState = rememberScrollState()
    
    // Expandable section states
    var eventsExpanded by remember { mutableStateOf(true) }
    var tagsExpanded by remember { mutableStateOf(false) }
    var notesExpanded by remember { mutableStateOf(false) }
    var mediaExpanded by remember { mutableStateOf(false) }
    var familiesExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        // Header
        Text("Inspector", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        
        // Individual basic info
        Text("Name: ${individual.displayName}", style = MaterialTheme.typography.bodyMedium)
        val years = listOfNotNull(
            individual.birthYear?.toString(),
            individual.deathYear?.toString()
        ).joinToString("–")
        if (years.isNotBlank()) {
            Text("Years: $years", style = MaterialTheme.typography.bodySmall)
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Events section (GEDCOM)
        ExpandableSection(
            title = "Events (GEDCOM)",
            expanded = eventsExpanded,
            onToggle = { eventsExpanded = !eventsExpanded },
            count = individual.events.size
        ) {
            EventsSection(
                events = individual.events,
                sources = project.sources,
                onUpdate = { updatedEvents ->
                    onUpdateIndividual(individual.copy(events = updatedEvents))
                }
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Tags section
        ExpandableSection(
            title = "Tags",
            expanded = tagsExpanded,
            onToggle = { tagsExpanded = !tagsExpanded },
            count = individual.tags.size
        ) {
            TagsSection(
                tags = individual.tags,
                onUpdate = { updatedTags ->
                    onUpdateIndividual(individual.copy(tags = updatedTags))
                }
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Notes section
        ExpandableSection(
            title = "Notes",
            expanded = notesExpanded,
            onToggle = { notesExpanded = !notesExpanded },
            count = individual.notes.size
        ) {
            NotesSection(
                notes = individual.notes,
                sources = project.sources,
                onUpdate = { updatedNotes ->
                    onUpdateIndividual(individual.copy(notes = updatedNotes))
                }
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Media section
        ExpandableSection(
            title = "Media",
            expanded = mediaExpanded,
            onToggle = { mediaExpanded = !mediaExpanded },
            count = individual.media.size
        ) {
            MediaSection(
                media = individual.media,
                onUpdate = { updatedMedia ->
                    onUpdateIndividual(individual.copy(media = updatedMedia))
                }
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Families section
        ExpandableSection(
            title = "Families",
            expanded = familiesExpanded,
            onToggle = { familiesExpanded = !familiesExpanded },
            count = project.families.count { fam -> 
                fam.husbandId == individual.id || fam.wifeId == individual.id 
            }
        ) {
            FamiliesSection(individual = individual, project = project)
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    count: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "$title ($count)",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
        }
        
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun EventsSection(
    events: List<GedcomEvent>,
    sources: List<Source>,
    onUpdate: (List<GedcomEvent>) -> Unit
) {
    var selectedEventIndex by remember { mutableStateOf<Int?>(null) }
    
    if (events.isEmpty()) {
        Text(
            "No events",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    } else {
        events.forEachIndexed { index, event ->
            val isSelected = selectedEventIndex == index
            EventItem(
                event = event,
                isSelected = isSelected,
                onClick = { selectedEventIndex = if (isSelected) null else index },
                onUpdate = { updated ->
                    onUpdate(events.toMutableList().also { it[index] = updated })
                },
                onDelete = {
                    onUpdate(events.toMutableList().also { it.removeAt(index) })
                    if (selectedEventIndex == index) selectedEventIndex = null
                }
            )
            Spacer(Modifier.height(4.dp))
        }
    }
    
    // Add event button
    Button(
        onClick = {
            val newEvent = GedcomEvent(
                id = GedcomEventId.generate(),
                type = "EVEN",
                date = "",
                place = ""
            )
            onUpdate(events + newEvent)
            selectedEventIndex = events.size
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add Event")
        Spacer(Modifier.width(4.dp))
        Text("Add Event")
    }
}

@Composable
private fun EventItem(
    event: GedcomEvent,
    isSelected: Boolean,
    onClick: () -> Unit,
    onUpdate: (GedcomEvent) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        // Event summary
        val parts = listOfNotNull(
            event.type?.takeIf { it.isNotBlank() },
            event.date?.takeIf { it.isNotBlank() },
            event.place?.takeIf { it.isNotBlank() }
        )
        Text(
            text = if (parts.isNotEmpty()) parts.joinToString(" — ") else "(empty event)",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Expanded details
        if (isSelected) {
            Spacer(Modifier.height(8.dp))
            
            // Type field
            var typeText by remember(event.id) { mutableStateOf(event.type) }
            OutlinedTextField(
                value = typeText,
                onValueChange = { 
                    typeText = it
                    onUpdate(event.copy(type = it.trim()))
                },
                label = { Text("Type (BIRT, DEAT, MARR, etc.)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(Modifier.height(4.dp))
            
            // Date field
            var dateText by remember(event.id) { mutableStateOf(event.date) }
            OutlinedTextField(
                value = dateText,
                onValueChange = { 
                    dateText = it
                    onUpdate(event.copy(date = it.trim()))
                },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(Modifier.height(4.dp))
            
            // Place field
            var placeText by remember(event.id) { mutableStateOf(event.place) }
            OutlinedTextField(
                value = placeText,
                onValueChange = { 
                    placeText = it
                    onUpdate(event.copy(place = it.trim()))
                },
                label = { Text("Place") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Delete button
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
                Spacer(Modifier.width(4.dp))
                Text("Delete Event")
            }
        }
    }
}

@Composable
private fun TagsSection(
    tags: List<Tag>,
    onUpdate: (List<Tag>) -> Unit
) {
    var newTagName by remember { mutableStateOf("") }
    
    if (tags.isNotEmpty()) {
        tags.forEachIndexed { index, tag ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "• ${tag.name}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        onUpdate(tags.toMutableList().also { it.removeAt(index) })
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    
    // Add tag input
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = newTagName,
            onValueChange = { newTagName = it },
            label = { Text("New tag") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        IconButton(
            onClick = {
                if (newTagName.isNotBlank()) {
                    val newTag = Tag(
                        id = TagId("tag_${System.currentTimeMillis()}"),
                        name = newTagName.trim()
                    )
                    onUpdate(tags + newTag)
                    newTagName = ""
                }
            }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
}

@Composable
private fun NotesSection(
    notes: List<Note>,
    sources: List<Source>,
    onUpdate: (List<Note>) -> Unit
) {
    var selectedNoteIndex by remember { mutableStateOf<Int?>(null) }
    
    if (notes.isEmpty()) {
        Text(
            "No notes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    } else {
        notes.forEachIndexed { index, note ->
            val isSelected = selectedNoteIndex == index
            NoteItem(
                note = note,
                isSelected = isSelected,
                onClick = { selectedNoteIndex = if (isSelected) null else index },
                onUpdate = { updated ->
                    onUpdate(notes.toMutableList().also { it[index] = updated })
                },
                onDelete = {
                    onUpdate(notes.toMutableList().also { it.removeAt(index) })
                    if (selectedNoteIndex == index) selectedNoteIndex = null
                }
            )
            Spacer(Modifier.height(4.dp))
        }
    }
    
    Spacer(Modifier.height(8.dp))
    
    // Add note button
    Button(
        onClick = {
            val newNote = Note(
                id = NoteId("note_${System.currentTimeMillis()}"),
                text = "New note"
            )
            onUpdate(notes + newNote)
            selectedNoteIndex = notes.size
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add Note")
        Spacer(Modifier.width(4.dp))
        Text("Add Note")
    }
}

@Composable
private fun NoteItem(
    note: Note,
    isSelected: Boolean,
    onClick: () -> Unit,
    onUpdate: (Note) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        // Note preview
        val preview = note.text.take(60) + if (note.text.length > 60) "…" else ""
        Text(
            text = preview,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Expanded editor
        if (isSelected) {
            Spacer(Modifier.height(8.dp))
            
            var noteText by remember(note.id) { mutableStateOf(note.text) }
            OutlinedTextField(
                value = noteText,
                onValueChange = { 
                    noteText = it
                    onUpdate(note.copy(text = it))
                },
                label = { Text("Note text") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 6
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Delete button
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
                Spacer(Modifier.width(4.dp))
                Text("Delete Note")
            }
        }
    }
}

@Composable
private fun MediaSection(
    media: List<MediaAttachment>,
    onUpdate: (List<MediaAttachment>) -> Unit
) {
    if (media.isEmpty()) {
        Text(
            "No media attachments",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    } else {
        media.forEachIndexed { index, attachment ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "• ${attachment.fileName ?: attachment.relativePath ?: "(media)"}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = {
                        onUpdate(media.toMutableList().also { it.removeAt(index) })
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
    
    Spacer(Modifier.height(8.dp))
    
    // Add media button (placeholder - file selection needs platform-specific impl)
    Button(
        onClick = { /* TODO: platform-specific file picker */ },
        modifier = Modifier.fillMaxWidth(),
        enabled = false
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add Media")
        Spacer(Modifier.width(4.dp))
        Text("Add Media (TODO)")
    }
}

@Composable
private fun FamiliesSection(
    individual: Individual,
    project: ProjectData
) {
    val individualsById = remember(project) { project.individuals.associateBy { it.id } }
    
    // Helper to format name as "LastName F."
    fun formatSpouseName(ind: Individual?): String {
        if (ind == null) return ""
        val tokens = ind.displayName.trim().split(Regex("\\s+"))
        val first = tokens.firstOrNull() ?: ""
        val last = if (tokens.size > 1) tokens.drop(1).joinToString(" ") else ""
        return if (last.isNotEmpty() && first.isNotEmpty()) {
            "$last ${first.first().uppercaseChar()}."
        } else if (last.isNotEmpty()) {
            last
        } else {
            first
        }
    }
    
    // Find all families where this person is a spouse
    val spouseFamilies = remember(individual.id, project.families) {
        project.families.filter { fam -> 
            fam.husbandId == individual.id || fam.wifeId == individual.id 
        }
    }
    
    if (spouseFamilies.isEmpty()) {
        Text(
            "No family records",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    } else {
        spouseFamilies.forEach { fam ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                val husband = individualsById[fam.husbandId]
                val wife = individualsById[fam.wifeId]
                
                Text(
                    text = "Family: ${fam.id.value}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (husband != null) {
                    Text(
                        "  Husband: ${formatSpouseName(husband)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (wife != null) {
                    Text(
                        "  Wife: ${formatSpouseName(wife)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                val childrenCount = fam.childrenIds.size
                if (childrenCount > 0) {
                    Text(
                        "  Children: $childrenCount",
                        style = MaterialTheme.typography.bodySmall
                    )
                    fam.childrenIds.forEach { childId ->
                        val child = individualsById[childId]
                        if (child != null) {
                            Text(
                                "    • ${child.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Text(
                        "  Children: 0",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
