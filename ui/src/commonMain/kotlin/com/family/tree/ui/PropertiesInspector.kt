package com.family.tree.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.family.tree.core.ProjectData
import com.family.tree.core.model.*

// Predefined GEDCOM event types for individuals
private val INDIVIDUAL_EVENT_TYPES = listOf(
    "BIRT", // birth
    "DEAT", // death
    "BURI", // burial
    "CHR",  // christening
    "BAPM", // baptism
    "CREM", // cremation
    "ADOP", // adoption
    "RESI", // residence
    "GRAD", // graduation
    "RETI", // retirement
    "PROB", // probate
    "WILL", // will
    "EVEN"  // generic event
)

// Predefined GEDCOM event types for families
private val FAMILY_EVENT_TYPES = listOf(
    "MARR", // marriage
    "ENGA", // engagement
    "DIV",  // divorce
    "ANUL", // annulment
    "MARS", // marriage settlement
    "MARB", // marriage banns
    "MARL", // marriage license
    "EVEN"  // generic event
)

@Composable
fun PropertiesInspector(
    family: Family,
    project: ProjectData,
    onUpdateFamily: (Family) -> Unit
) {
    val scrollState = rememberScrollState()
    
    // Expandable section states
    var eventsExpanded by remember { mutableStateOf(true) }
    var tagsExpanded by remember { mutableStateOf(false) }
    var notesExpanded by remember { mutableStateOf(false) }
    var mediaExpanded by remember { mutableStateOf(false) }
    var membersExpanded by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        // Header
        Text("Properties", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        
        val individualsById = remember(project) { project.individuals.associateBy { it.id } }
        val husband = individualsById[family.husbandId]
        val wife = individualsById[family.wifeId]
        
        // Selection info - display family as "Husband I. — Wife I."
        val familyLabel = buildFamilyLabel(husband, wife)
        Text("Family: ${familyLabel}", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(Modifier.height(16.dp))
        
        // Members section
        ExpandableSection(
            title = "Members",
            expanded = membersExpanded,
            onToggle = { membersExpanded = !membersExpanded },
            count = listOfNotNull(family.husbandId, family.wifeId).size + family.childrenIds.size
        ) {
            if (husband != null) {
                Text("Husband: ${husband.displayName}", style = MaterialTheme.typography.bodySmall)
            }
            if (wife != null) {
                Text("Wife: ${wife.displayName}", style = MaterialTheme.typography.bodySmall)
            }
            if (family.childrenIds.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Children:", style = MaterialTheme.typography.labelMedium)
                family.childrenIds.forEach { childId ->
                    val child = individualsById[childId]
                    if (child != null) {
                        Text("  • ${child.displayName}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Events section (GEDCOM)
        ExpandableSection(
            title = "Events (GEDCOM)",
            expanded = eventsExpanded,
            onToggle = { eventsExpanded = !eventsExpanded },
            count = family.events.size
        ) {
            EventsSection(
                events = family.events,
                sources = project.sources,
                eventTypes = FAMILY_EVENT_TYPES,
                onUpdate = { updatedEvents ->
                    onUpdateFamily(family.copy(events = updatedEvents))
                }
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Tags section
        ExpandableSection(
            title = "Tags",
            expanded = tagsExpanded,
            onToggle = { tagsExpanded = !tagsExpanded },
            count = family.tags.size
        ) {
            TagsSection(
                tags = family.tags,
                onUpdate = { updatedTags ->
                    onUpdateFamily(family.copy(tags = updatedTags))
                }
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Notes section
        ExpandableSection(
            title = "Notes",
            expanded = notesExpanded,
            onToggle = { notesExpanded = !notesExpanded },
            count = family.notes.size
        ) {
            NotesSection(
                notes = family.notes,
                sources = project.sources,
                onUpdate = { updatedNotes ->
                    onUpdateFamily(family.copy(notes = updatedNotes))
                }
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Media section
        ExpandableSection(
            title = "Media",
            expanded = mediaExpanded,
            onToggle = { mediaExpanded = !mediaExpanded },
            count = family.media.size
        ) {
            MediaSection(
                media = family.media,
                onUpdate = { updatedMedia ->
                    onUpdateFamily(family.copy(media = updatedMedia))
                }
            )
        }
    }
}

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
        Text("Properties", style = MaterialTheme.typography.titleSmall)
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
                eventTypes = INDIVIDUAL_EVENT_TYPES,
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
    eventTypes: List<String>,
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
                sources = sources,
                eventTypes = eventTypes,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventItem(
    event: GedcomEvent,
    sources: List<Source>,
    eventTypes: List<String>,
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
            
            // Type field with predefined GEDCOM types (dropdown only)
            var typeText by remember(event.id) { mutableStateOf(event.type ?: "") }
            var typeMenuExpanded by remember(event.id) { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = typeText,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Type") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
                    }
                )

                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    eventTypes.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                typeText = option
                                onUpdate(event.copy(type = option))
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(4.dp))
            
            // Date field with picker dialog
            var dateText by remember(event.id) { mutableStateOf(event.date) }
            var showDateDialog by remember(event.id) { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        onUpdate(event.copy(date = it.trim()))
                    },
                    label = { Text("Date") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { showDateDialog = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick date")
                }
            }
            if (showDateDialog) {
                DatePhraseDialog(
                    initial = dateText.orEmpty(),
                    onConfirm = { value ->
                        dateText = value
                        onUpdate(event.copy(date = value))
                        showDateDialog = false
                    },
                    onDismiss = { showDateDialog = false }
                )
            }
            
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
            
            Spacer(Modifier.height(12.dp))
            
            // Sources section
            Text(
                text = "Sources:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(Modifier.height(4.dp))
            
            var selectedSourceIndex by remember(event.id) { mutableStateOf<Int?>(null) }
            
            // Display event sources
            if (event.sources.isEmpty()) {
                Text(
                    "No sources",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                event.sources.forEachIndexed { srcIndex, citation ->
                    val isSourceSelected = selectedSourceIndex == srcIndex
                    val sourceTitle = if (citation.text.isNotBlank()) {
                        citation.text
                    } else {
                        citation.sourceId?.let { srcId ->
                            sources.firstOrNull { it.id == srcId }?.title?.takeIf { it.isNotBlank() }
                        } ?: "Unknown source"
                    }
                    val displayText = if (citation.page.isNotBlank()) {
                        "$sourceTitle — PAGE: ${citation.page}"
                    } else {
                        sourceTitle
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSourceSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .clickable { selectedSourceIndex = if (isSourceSelected) null else srcIndex }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• $displayText",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = {
                                val updatedSources = event.sources.toMutableList().also { it.removeAt(srcIndex) }
                                onUpdate(event.copy(sources = updatedSources))
                                if (selectedSourceIndex == srcIndex) selectedSourceIndex = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    // Page editor for selected source
                    if (isSourceSelected) {
                        Spacer(Modifier.height(4.dp))
                        var pageText by remember(citation.id) { mutableStateOf(citation.page) }
                        OutlinedTextField(
                            value = pageText,
                            onValueChange = { newPage ->
                                pageText = newPage
                                val updatedSources = event.sources.toMutableList().also {
                                    it[srcIndex] = citation.copy(page = newPage.trim())
                                }
                                onUpdate(event.copy(sources = updatedSources))
                            },
                            label = { Text("PAGE") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    
                    Spacer(Modifier.height(4.dp))
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Add source button
            var showSourceDialog by remember(event.id) { mutableStateOf(false) }
            Button(
                onClick = { showSourceDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Source")
                Spacer(Modifier.width(4.dp))
                Text("Add Source")
            }
            
            if (showSourceDialog) {
                AddSourceDialog(
                    sources = sources,
                    onConfirm = { sourceId, page ->
                        val newCitation = SourceCitation(
                            id = SourceCitationId.generate(),
                            sourceId = sourceId,
                            page = page.trim()
                        )
                        val updatedSources = event.sources + newCitation
                        onUpdate(event.copy(sources = updatedSources))
                        showSourceDialog = false
                    },
                    onDismiss = { showSourceDialog = false }
                )
            }
            
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
    OutlinedTextField(
        value = newTagName,
        onValueChange = { newTagName = it },
        label = { Text("New tag") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    
    Spacer(Modifier.height(8.dp))
    
    // Add tag button
    Button(
        onClick = {
            if (newTagName.isNotBlank()) {
                val newTag = Tag(
                    id = TagId.generate(),
                    name = newTagName.trim()
                )
                onUpdate(tags + newTag)
                newTagName = ""
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add Tag")
        Spacer(Modifier.width(4.dp))
        Text("Add Tag")
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
                sources = sources,
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
                id = NoteId.generate(),
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
    sources: List<Source>,
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
            
            Spacer(Modifier.height(12.dp))
            
            // Sources section
            Text(
                text = "Sources:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(Modifier.height(4.dp))
            
            var selectedSourceIndex by remember(note.id) { mutableStateOf<Int?>(null) }
            
            // Display note sources
            if (note.sources.isEmpty()) {
                Text(
                    "No sources",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                note.sources.forEachIndexed { srcIndex, citation ->
                    val isSourceSelected = selectedSourceIndex == srcIndex
                    val sourceTitle = if (citation.text.isNotBlank()) {
                        citation.text
                    } else {
                        citation.sourceId?.let { srcId ->
                            sources.firstOrNull { it.id == srcId }?.title?.takeIf { it.isNotBlank() }
                        } ?: "Unknown source"
                    }
                    val displayText = if (citation.page.isNotBlank()) {
                        "$sourceTitle — PAGE: ${citation.page}"
                    } else {
                        sourceTitle
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSourceSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .clickable { selectedSourceIndex = if (isSourceSelected) null else srcIndex }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• $displayText",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = {
                                val updatedSources = note.sources.toMutableList().also { it.removeAt(srcIndex) }
                                onUpdate(note.copy(sources = updatedSources))
                                if (selectedSourceIndex == srcIndex) selectedSourceIndex = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    // Page editor for selected source
                    if (isSourceSelected) {
                        Spacer(Modifier.height(4.dp))
                        var pageText by remember(citation.id) { mutableStateOf(citation.page) }
                        OutlinedTextField(
                            value = pageText,
                            onValueChange = { newPage ->
                                pageText = newPage
                                val updatedSources = note.sources.toMutableList().also {
                                    it[srcIndex] = citation.copy(page = newPage.trim())
                                }
                                onUpdate(note.copy(sources = updatedSources))
                            },
                            label = { Text("PAGE") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    
                    Spacer(Modifier.height(4.dp))
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Add source button
            var showSourceDialog by remember(note.id) { mutableStateOf(false) }
            Button(
                onClick = { showSourceDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Source")
                Spacer(Modifier.width(4.dp))
                Text("Add Source")
            }
            
            if (showSourceDialog) {
                AddSourceDialog(
                    sources = sources,
                    onConfirm = { sourceId, page ->
                        val newCitation = SourceCitation(
                            id = SourceCitationId.generate(),
                            sourceId = sourceId,
                            page = page.trim()
                        )
                        val updatedSources = note.sources + newCitation
                        onUpdate(note.copy(sources = updatedSources))
                        showSourceDialog = false
                    },
                    onDismiss = { showSourceDialog = false }
                )
            }
            
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
            val initial = first.firstOrNull()?.uppercaseChar()
            if (initial != null) "$last $initial." else last
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

@Composable
private fun AddSourceDialog(
    sources: List<Source>,
    onConfirm: (SourceId?, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSourceIndex by remember { mutableStateOf<Int?>(null) }
    var pageText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Source Citation") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                if (sources.isEmpty()) {
                    Text(
                        "No sources available in the project. Please add sources first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        "Select a source:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    sources.forEachIndexed { index, source ->
                        val isSelected = selectedSourceIndex == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable { selectedSourceIndex = index }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedSourceIndex = index }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = source.title.takeIf { it.isNotBlank() } ?: source.id.value,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { pageText = it },
                        label = { Text("PAGE (optional)") },
                        placeholder = { Text("e.g., p. 123, Chapter 5, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sourceId = selectedSourceIndex?.let { sources[it].id }
                    onConfirm(sourceId, pageText)
                },
                enabled = sources.isNotEmpty() && selectedSourceIndex != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper function to build family label in format "Husband I. — Wife I."
private fun buildFamilyLabel(husband: Individual?, wife: Individual?): String {
    val husbandInitials = buildInitials(husband)
    val wifeInitials = buildInitials(wife)
    
    return when {
        husbandInitials.isNotBlank() && wifeInitials.isNotBlank() -> "$husbandInitials — $wifeInitials"
        husbandInitials.isNotBlank() -> husbandInitials
        wifeInitials.isNotBlank() -> wifeInitials
        else -> "Family"
    }
}

// Helper function to build initials for an individual
private fun buildInitials(person: Individual?): String {
    if (person == null) return ""
    
    val lastName = person.lastName.trim()
    val firstName = person.firstName.trim()
    
    return when {
        lastName.isNotBlank() && firstName.isNotBlank() -> {
            val initial = firstName.firstOrNull()?.uppercaseChar() ?: return lastName
            "$lastName $initial."
        }
        lastName.isNotBlank() -> lastName
        firstName.isNotBlank() -> firstName
        else -> ""
    }
}
