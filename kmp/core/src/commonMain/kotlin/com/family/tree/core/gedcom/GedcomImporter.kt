package com.family.tree.core.gedcom

import com.family.tree.core.ProjectData
import com.family.tree.core.ProjectMetadata
import com.family.tree.core.model.*

/**
 * GEDCOM importer for KMP.
 * Parses GEDCOM 5.5 format and builds ProjectData.
 */
class GedcomImporter {
    
    fun importFromString(content: String): ProjectData {
        val lines = content.lines()
        
        val inds = mutableMapOf<String, IndiRec>()
        val fams = mutableMapOf<String, FamRec>()
        val notes = mutableMapOf<String, NoteRec>()
        
        var currentRecord: String? = null
        var currentXref: String? = null
        var curInd: IndiRec? = null
        var curFam: FamRec? = null
        var curNote: NoteRec? = null
        var curEvent: EventRec? = null
        val context = mutableListOf<String>()
        var textAccumulator: StringBuilder? = null
        
        for (raw in lines) {
            if (raw.isBlank()) continue
            val parsed = parseLine(raw) ?: continue
            val (level, xref, tag, value) = parsed
            
            while (context.size > level) {
                context.removeAt(context.size - 1)
            }
            
            if (level == 0) {
                if (textAccumulator != null && curNote != null) {
                    curNote.text = textAccumulator.toString()
                    textAccumulator = null
                }
                
                currentRecord = tag
                currentXref = xref
                curInd = null
                curFam = null
                curNote = null
                curEvent = null
                context.clear()
                
                when (tag) {
                    "INDI" -> {
                        curInd = IndiRec(xref = xref ?: generateId())
                        inds[curInd.xref] = curInd
                    }
                    "FAM" -> {
                        curFam = FamRec(xref = xref ?: generateId())
                        fams[curFam.xref] = curFam
                    }
                    "NOTE" -> {
                        curNote = NoteRec(xref = xref ?: generateId())
                        notes[curNote.xref] = curNote
                        textAccumulator = StringBuilder()
                        if (value != null) textAccumulator.append(value)
                    }
                }
            } else if (currentRecord == "INDI" && curInd != null) {
                processIndiLine(level, tag, value, curInd, context)
            } else if (currentRecord == "FAM" && curFam != null) {
                processFamLine(level, tag, value, curFam, context)
            } else if (currentRecord == "NOTE" && curNote != null) {
                processNoteLine(level, tag, value, curNote, textAccumulator)
            }
        }
        
        // Final note text
        if (textAccumulator != null && curNote != null) {
            curNote.text = textAccumulator.toString()
        }
        
        return buildProjectData(inds, fams, notes)
    }
    
    private fun parseLine(line: String): LineParsed? {
        // Format: LEVEL [XREF] TAG [VALUE]
        // Examples:
        // 0 @I1@ INDI
        // 1 NAME John /Doe/
        // 2 DATE 1 JAN 1980
        
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        
        val parts = trimmed.split(Regex("\\s+"), limit = 4)
        if (parts.isEmpty()) return null
        
        val level = parts[0].toIntOrNull() ?: return null
        
        var xref: String? = null
        var tag: String
        var value: String? = null
        
        when {
            parts.size >= 3 && parts[1].startsWith("@") && parts[1].endsWith("@") -> {
                // Has xref
                xref = parts[1]
                tag = parts[2]
                value = if (parts.size >= 4) parts[3] else null
            }
            parts.size >= 2 -> {
                // No xref
                tag = parts[1]
                value = if (parts.size >= 3) {
                    // Rejoin remaining parts
                    parts.subList(2, parts.size).joinToString(" ")
                } else null
            }
            else -> return null
        }
        
        return LineParsed(level, xref, tag, value?.trim())
    }
    
    private fun processIndiLine(
        level: Int,
        tag: String,
        value: String?,
        curInd: IndiRec,
        context: MutableList<String>
    ) {
        if (level == 1) {
            context.clear()
            context.add(tag)
            when (tag) {
                "NAME" -> {
                    val (given, surname) = GedcomMapper.parseName(value)
                    curInd.given = given
                    curInd.surname = surname
                }
                "SEX" -> curInd.sex = GedcomMapper.parseSex(value)
                // Handle all standard GEDCOM individual event tags
                "BIRT", "CHR", "DEAT", "BURI", "CREM", "ADOP", "BAPM", "BARM", "BASM", 
                "BLES", "CHRA", "CONF", "FCOM", "ORDN", "NATU", "EMIG", "IMMI", 
                "CENS", "PROB", "WILL", "GRAD", "RETI", "EVEN",
                // Handle individual attribute tags that can have dates/places
                "CAST", "DSCR", "EDUC", "IDNO", "NATI", "NCHI", "NMR", "OCCU", 
                "PROP", "RELI", "RESI", "SSN", "TITL" -> {
                    val event = EventRec(type = tag)
                    curInd.events.add(event)
                }
                "FAMC" -> if (value != null) curInd.famcXrefs.add(value)
                "FAMS" -> if (value != null) curInd.famsXrefs.add(value)
                "NOTE" -> if (value != null) curInd.noteXrefs.add(value)
                "_TAG" -> if (value != null) curInd.tags.add(value)
            }
        } else if (level == 2) {
            val ctx = context.lastOrNull()
            // Check if context is any event/attribute tag
            val eventTags = setOf(
                "BIRT", "CHR", "DEAT", "BURI", "CREM", "ADOP", "BAPM", "BARM", "BASM", 
                "BLES", "CHRA", "CONF", "FCOM", "ORDN", "NATU", "EMIG", "IMMI", 
                "CENS", "PROB", "WILL", "GRAD", "RETI", "EVEN",
                "CAST", "DSCR", "EDUC", "IDNO", "NATI", "NCHI", "NMR", "OCCU", 
                "PROP", "RELI", "RESI", "SSN", "TITL"
            )
            if (ctx != null && ctx in eventTags) {
                val event = curInd.events.lastOrNull()
                if (event != null) {
                    when (tag) {
                        "DATE" -> {
                            event.date = value
                            if (ctx == "BIRT") curInd.birthDate = value
                            else if (ctx == "DEAT") curInd.deathDate = value
                        }
                        "PLAC" -> {
                            event.place = value
                            if (ctx == "BIRT") curInd.birthPlace = value
                            else if (ctx == "DEAT") curInd.deathPlace = value
                        }
                    }
                }
            }
        }
    }
    
    private fun processFamLine(
        level: Int,
        tag: String,
        value: String?,
        curFam: FamRec,
        context: MutableList<String>
    ) {
        if (level == 1) {
            context.clear()
            context.add(tag)
            when (tag) {
                "HUSB" -> curFam.husbXref = value
                "WIFE" -> curFam.wifeXref = value
                "CHIL" -> if (value != null) curFam.childXrefs.add(value)
                "MARR" -> {
                    val event = EventRec(type = tag)
                    curFam.events.add(event)
                }
                "NOTE" -> if (value != null) curFam.noteXrefs.add(value)
                "_TAG" -> if (value != null) curFam.tags.add(value)
            }
        } else if (level == 2) {
            val ctx = context.lastOrNull()
            if (ctx == "MARR") {
                val event = curFam.events.lastOrNull()
                if (event != null) {
                    when (tag) {
                        "DATE" -> {
                            event.date = value
                            curFam.marrDate = value
                        }
                        "PLAC" -> {
                            event.place = value
                            curFam.marrPlace = value
                        }
                    }
                }
            }
        }
    }
    
    private fun processNoteLine(
        level: Int,
        tag: String,
        value: String?,
        curNote: NoteRec,
        textAccumulator: StringBuilder?
    ) {
        if (level == 1) {
            when (tag) {
                "CONC" -> if (value != null && textAccumulator != null) {
                    textAccumulator.append(value)
                }
                "CONT" -> if (textAccumulator != null) {
                    textAccumulator.append("\n")
                    if (value != null) textAccumulator.append(value)
                }
            }
        }
    }
    
    private fun buildProjectData(
        inds: Map<String, IndiRec>,
        fams: Map<String, FamRec>,
        notes: Map<String, NoteRec>
    ): ProjectData {
        val xrefToIndId = mutableMapOf<String, IndividualId>()
        val individuals = mutableListOf<Individual>()
        
        // Build individuals
        for ((xref, rec) in inds) {
            val id = IndividualId(generateId())
            xrefToIndId[xref] = id
            
            val birthYear = GedcomMapper.extractYearFromDate(rec.birthDate)
            val deathYear = GedcomMapper.extractYearFromDate(rec.deathDate)
            
            val gedcomEvents = rec.events.map { evt ->
                GedcomEvent(
                    type = evt.type,
                    date = evt.date ?: "",
                    place = evt.place ?: ""
                )
            }
            
            val notesList = rec.noteXrefs.mapNotNull { noteXref ->
                notes[noteXref]?.let { noteRec ->
                    Note(id = NoteId(generateId()), text = noteRec.text)
                }
            }
            
            val tagsList = rec.tags.map { tagName ->
                Tag(id = TagId(generateId()), name = tagName)
            }
            
            individuals.add(
                Individual(
                    id = id,
                    firstName = rec.given,
                    lastName = rec.surname,
                    gender = rec.sex,
                    birthYear = birthYear,
                    deathYear = deathYear,
                    events = gedcomEvents,
                    notes = notesList,
                    tags = tagsList
                )
            )
        }
        
        // Build families
        val families = mutableListOf<Family>()
        for ((xref, rec) in fams) {
            val famId = FamilyId(generateId())
            
            val husbandId = rec.husbXref?.let { xrefToIndId[it] }
            val wifeId = rec.wifeXref?.let { xrefToIndId[it] }
            val childrenIds = rec.childXrefs.mapNotNull { xrefToIndId[it] }
            
            families.add(
                Family(
                    id = famId,
                    husbandId = husbandId,
                    wifeId = wifeId,
                    childrenIds = childrenIds
                )
            )
        }
        
        return ProjectData(
            individuals = individuals,
            families = families,
            metadata = ProjectMetadata(name = "Imported GEDCOM")
        )
    }
    
    private var idCounter = 0
    private fun generateId(): String = "id_${++idCounter}"
    
    // Internal record classes for parsing
    private data class LineParsed(
        val level: Int,
        val xref: String?,
        val tag: String,
        val value: String?
    )
    
    private data class IndiRec(
        val xref: String,
        var given: String = "",
        var surname: String = "",
        var sex: Gender = Gender.UNKNOWN,
        var birthDate: String? = null,
        var birthPlace: String? = null,
        var deathDate: String? = null,
        var deathPlace: String? = null,
        val events: MutableList<EventRec> = mutableListOf(),
        val famcXrefs: MutableList<String> = mutableListOf(),
        val famsXrefs: MutableList<String> = mutableListOf(),
        val noteXrefs: MutableList<String> = mutableListOf(),
        val tags: MutableList<String> = mutableListOf()
    )
    
    private data class FamRec(
        val xref: String,
        var husbXref: String? = null,
        var wifeXref: String? = null,
        val childXrefs: MutableList<String> = mutableListOf(),
        var marrDate: String? = null,
        var marrPlace: String? = null,
        val events: MutableList<EventRec> = mutableListOf(),
        val noteXrefs: MutableList<String> = mutableListOf(),
        val tags: MutableList<String> = mutableListOf()
    )
    
    private data class NoteRec(
        val xref: String,
        var text: String = ""
    )
    
    private data class EventRec(
        val type: String,
        var date: String? = null,
        var place: String? = null
    )
}
