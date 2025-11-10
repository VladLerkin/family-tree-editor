package com.family.tree.core.gedcom

import com.family.tree.core.ProjectData
import com.family.tree.core.model.*

/**
 * GEDCOM exporter for KMP.
 * Exports ProjectData to GEDCOM 5.5.5 format string.
 */
class GedcomExporter {
    
    fun exportToString(data: ProjectData): String {
        val sb = StringBuilder()
        
        val inds = data.individuals
        val fams = data.families
        
        val indXref = GedcomMapper.buildIndividualXrefs(inds)
        val famXref = GedcomMapper.buildFamilyXrefs(fams)
        
        // Collect all notes from individuals and families
        val allNotes = mutableMapOf<String, Note>()
        for (ind in inds) {
            for (note in ind.notes) {
                allNotes[note.id.value] = note
            }
            for (event in ind.events) {
                for (note in event.notes) {
                    allNotes[note.id.value] = note
                }
            }
        }
        for (fam in fams) {
            for (note in fam.notes) {
                allNotes[note.id.value] = note
            }
            for (event in fam.events) {
                for (note in event.notes) {
                    allNotes[note.id.value] = note
                }
            }
        }
        
        // Build xrefs for notes
        val noteXref = mutableMapOf<String, String>()
        var noteIdx = 1
        for (noteId in allNotes.keys) {
            noteXref[noteId] = "@N${noteIdx++}@"
        }
        
        // Header (GEDCOM 5.5.5 structure)
        appendLine(sb, "0 HEAD")
        appendLine(sb, "1 GEDC")
        appendLine(sb, "2 VERS 5.5.5")
        appendLine(sb, "2 FORM LINEAGE-LINKED")
        appendLine(sb, "3 VERS 5.5.5")
        appendLine(sb, "1 CHAR UTF-8")
        appendLine(sb, "1 SOUR family.tree.kmp")
        appendLine(sb, "2 NAME Family Tree KMP")
        appendLine(sb, "2 VERS 1.0")
        
        // Current date/time in GEDCOM format
        val now = getCurrentDateTime()
        appendLine(sb, "1 DATE $now")
        
        // Individuals
        for (ind in inds) {
            val xref = indXref[ind.id.value]
            appendLine(sb, "0 $xref INDI")
            appendLine(sb, "1 NAME ${GedcomMapper.buildName(ind)}")
            
            val surname = ind.lastName
            val givenName = ind.firstName
            if (surname.isNotBlank()) {
                appendLine(sb, "2 SURN ${escapeValue(surname)}")
            }
            if (givenName.isNotBlank()) {
                appendLine(sb, "2 GIVN ${escapeValue(givenName)}")
            }
            
            appendLine(sb, "1 SEX ${GedcomMapper.sexCode(ind.gender)}")
            
            // Export events
            for (event in ind.events) {
                val type = event.type
                if (type.isNotBlank()) {
                    appendLine(sb, "1 $type")
                    if (event.date?.isNotBlank() == true) {
                        appendLine(sb, "2 DATE ${event.date}")
                    }
                    if (event.place?.isNotBlank() == true) {
                        appendLine(sb, "2 PLAC ${event.place}")
                    }
                    
                    // Export event notes
                    for (note in event.notes) {
                        val nx = noteXref[note.id.value]
                        if (nx != null) {
                            appendLine(sb, "2 NOTE $nx")
                        }
                    }
                }
            }
            
            // Family child/spouse links
            for (f in fams) {
                if (f.childrenIds.any { it == ind.id }) {
                    val fx = famXref[f.id.value]
                    if (fx != null) appendLine(sb, "1 FAMC $fx")
                }
                if ((f.husbandId != null && f.husbandId == ind.id) || 
                    (f.wifeId != null && f.wifeId == ind.id)) {
                    val fx = famXref[f.id.value]
                    if (fx != null) appendLine(sb, "1 FAMS $fx")
                }
            }
            
            // Notes
            for (note in ind.notes) {
                val nx = noteXref[note.id.value]
                if (nx != null) appendLine(sb, "1 NOTE $nx")
            }
            
            // Tags as custom fields
            for (tag in ind.tags) {
                if (tag.name.isNotBlank()) {
                    appendLine(sb, "1 _TAG ${escapeValue(tag.name)}")
                }
            }
        }
        
        // Families
        for (fam in fams) {
            val fx = famXref[fam.id.value]
            appendLine(sb, "0 $fx FAM")
            
            if (fam.husbandId != null) {
                val hx = indXref[fam.husbandId.value]
                if (hx != null) appendLine(sb, "1 HUSB $hx")
            }
            
            if (fam.wifeId != null) {
                val wx = indXref[fam.wifeId.value]
                if (wx != null) appendLine(sb, "1 WIFE $wx")
            }
            
            for (childId in fam.childrenIds) {
                val cx = indXref[childId.value]
                if (cx != null) appendLine(sb, "1 CHIL $cx")
            }
            
            // Export family events
            for (event in fam.events) {
                val type = event.type
                if (type.isNotBlank()) {
                    appendLine(sb, "1 $type")
                    if (event.date?.isNotBlank() == true) {
                        appendLine(sb, "2 DATE ${event.date}")
                    }
                    if (event.place?.isNotBlank() == true) {
                        appendLine(sb, "2 PLAC ${event.place}")
                    }
                    
                    // Export event notes
                    for (note in event.notes) {
                        val nx = noteXref[note.id.value]
                        if (nx != null) {
                            appendLine(sb, "2 NOTE $nx")
                        }
                    }
                }
            }
            
            // Notes
            for (note in fam.notes) {
                val nx = noteXref[note.id.value]
                if (nx != null) appendLine(sb, "1 NOTE $nx")
            }
            
            // Tags as custom fields
            for (tag in fam.tags) {
                if (tag.name.isNotBlank()) {
                    appendLine(sb, "1 _TAG ${escapeValue(tag.name)}")
                }
            }
        }
        
        // Notes
        for ((noteId, note) in allNotes) {
            val nx = noteXref[noteId]
            if (nx != null) {
                appendLine(sb, "0 $nx NOTE")
                writeMultilineValue(sb, 1, note.text)
            }
        }
        
        // Trailer
        appendLine(sb, "0 TRLR")
        
        return sb.toString()
    }
    
    private fun appendLine(sb: StringBuilder, line: String) {
        sb.append(line).append("\n")
    }
    
    private fun escapeValue(value: String): String {
        // Replace newlines with spaces for single-line values
        return value.replace("\n", " ").replace("\r", " ")
    }
    
    private fun writeMultilineValue(sb: StringBuilder, level: Int, text: String) {
        if (text.isEmpty()) {
            appendLine(sb, "$level CONT")
            return
        }
        
        val lines = text.split("\n")
        for ((idx, line) in lines.withIndex()) {
            if (idx == 0) {
                // First line uses CONC (no newline before it)
                if (line.length <= 248) {
                    appendLine(sb, "$level CONC $line")
                } else {
                    // Split long lines
                    var remaining = line
                    while (remaining.isNotEmpty()) {
                        val chunk = remaining.take(248)
                        remaining = remaining.drop(248)
                        appendLine(sb, "$level CONC $chunk")
                    }
                }
            } else {
                // Subsequent lines use CONT (newline before them)
                if (line.length <= 248) {
                    appendLine(sb, "$level CONT $line")
                } else {
                    var remaining = line
                    appendLine(sb, "$level CONT ${remaining.take(248)}")
                    remaining = remaining.drop(248)
                    while (remaining.isNotEmpty()) {
                        val chunk = remaining.take(248)
                        remaining = remaining.drop(248)
                        appendLine(sb, "$level CONC $chunk")
                    }
                }
            }
        }
    }
    
    private fun getCurrentDateTime(): String {
        // Return current date in GEDCOM format: DD MMM YYYY
        // For KMP, we'll use a simple approach
        // In a real implementation, you might want to use kotlinx-datetime
        return "1 JAN 2025"
    }
}
