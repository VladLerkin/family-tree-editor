package com.family.tree.gedcom

import com.family.tree.model.*
import com.family.tree.storage.ProjectRepository
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.Locale
import java.util.Objects
import java.util.UUID
import java.util.regex.Pattern

/**
 * Kotlin port of the original Java GedcomImporter with 1:1 behavior.
 * No delegation to Java classes; preserves parsing logic and mapping.
 */
class GedcomImporter {
    companion object {
        private val LINE: Pattern = Pattern.compile("^\\s*(\\d+)\\s+(?:(@[^@]+@)\\s+)?([A-Z0-9_]+)(?:\\s+(.*))?$")
    }

    @Throws(IOException::class)
    fun importFromFile(path: Path): ProjectRepository.ProjectData {
        Objects.requireNonNull(path, "path")
        val lines = Files.readAllLines(path, StandardCharsets.UTF_8)

        val inds = LinkedHashMap<String, IndiRec>()
        val fams = LinkedHashMap<String, FamRec>()
        val notes = LinkedHashMap<String, NoteRec>()
        val media = LinkedHashMap<String, MediaRec>()
        val sources = LinkedHashMap<String, SourceRec>()
        val repos = LinkedHashMap<String, RepositoryRec>()
        val submitters = LinkedHashMap<String, SubmitterRec>()

        var currentRecord: String? = null
        var currentXref: String? = null
        var curInd: IndiRec? = null
        var curFam: FamRec? = null
        var curNote: NoteRec? = null
        var curMedia: MediaRec? = null
        var curSource: SourceRec? = null
        var curRepo: RepositoryRec? = null
        var curSubmitter: SubmitterRec? = null
        var curEvent: EventRec? = null
        var curAddress: AddressRec? = null
        val context: ArrayDeque<String> = ArrayDeque()
        var textAccumulator: StringBuilder? = null

        for (raw in lines) {
            if (raw == null || raw.isBlank()) continue
            val m = LINE.matcher(raw)
            if (!m.matches()) continue
            val level = m.group(1).toInt()
            val xref = m.group(2)
            val tag = m.group(3)
            val value = m.group(4)?.trim()

            while (context.size > level) context.pop()

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
                curMedia = null
                curSource = null
                curRepo = null
                curSubmitter = null
                curEvent = null
                curAddress = null
                context.clear()

                when (tag) {
                    "INDI" -> {
                        curInd = IndiRec().also { it.xref = xref }
                        inds[xref ?: UUID.randomUUID().toString()] = curInd!!
                    }
                    "FAM" -> {
                        curFam = FamRec().also { it.xref = xref }
                        fams[xref ?: UUID.randomUUID().toString()] = curFam!!
                    }
                    "NOTE" -> {
                        curNote = NoteRec().also { it.xref = xref }
                        notes[xref ?: UUID.randomUUID().toString()] = curNote!!
                        textAccumulator = StringBuilder()
                        if (!value.isNullOrEmpty()) textAccumulator!!.append(value)
                    }
                    "OBJE" -> {
                        curMedia = MediaRec().also { it.xref = xref }
                        media[xref ?: UUID.randomUUID().toString()] = curMedia!!
                    }
                    "SOUR" -> {
                        curSource = SourceRec().also { it.xref = xref }
                        sources[xref ?: UUID.randomUUID().toString()] = curSource!!
                    }
                    "REPO" -> {
                        curRepo = RepositoryRec().also { it.xref = xref }
                        repos[xref ?: UUID.randomUUID().toString()] = curRepo!!
                    }
                    "SUBM" -> {
                        curSubmitter = SubmitterRec().also { it.xref = xref }
                        submitters[xref ?: UUID.randomUUID().toString()] = curSubmitter!!
                    }
                }
            } else if ("INDI" == currentRecord && curInd != null) {
                if (level == 1) {
                    context.clear()
                    context.push(tag)
                    when (tag) {
                        "NAME" -> {
                            val nm = GedcomMapper.parseName(value)
                            curInd.given = nm[0]
                            curInd.surname = nm[1]
                        }
                        "SEX" -> curInd.sex = GedcomMapper.parseSex(value)
                        "BIRT", "DEAT", "BURI", "ADOP", "RESI" -> {
                            curEvent = EventRec().also { it.type = tag }
                            curInd.events.add(curEvent!!)
                        }
                        "FAMC" -> if (value != null) curInd.famcXrefs.add(value)
                        "FAMS" -> if (value != null) curInd.famsXrefs.add(value)
                        "NOTE" -> if (value != null) curInd.noteXrefs.add(value)
                        "OBJE" -> if (value != null) curInd.mediaXrefs.add(value)
                        "_TAG" -> if (value != null) curInd.tags.add(value)
                    }
                } else if (level == 2) {
                    val ctx = context.peek() ?: continue
                    if (ctx == "BIRT" || ctx == "DEAT" || ctx == "BURI" || ctx == "ADOP" || ctx == "RESI") {
                        if (curEvent != null) {
                            when (tag) {
                                "DATE" -> curEvent!!.date = value
                                "PLAC" -> curEvent!!.place = value
                                "SOUR" -> if (value != null) {
                                    val sc = SourceCitationRec()
                                    sc.sourceXref = value
                                    curEvent!!.sources.add(sc)
                                    context.push("SOUR")
                                }
                            }
                        }
                        if (ctx == "BIRT") {
                            if (tag == "DATE") curInd.birthDate = value else if (tag == "PLAC") curInd.birthPlace = value
                        } else if (ctx == "DEAT") {
                            if (tag == "DATE") curInd.deathDate = value else if (tag == "PLAC") curInd.deathPlace = value
                        }
                    } else if (ctx == "FAMC") {
                        if (tag == "PEDI" && value != null) {
                            curInd.pedigree = value
                        }
                    }
                } else if (level == 3) {
                    val ctx = context.peek()
                    if (ctx == "SOUR" && curEvent != null && curEvent!!.sources.isNotEmpty()) {
                        val sc = curEvent!!.sources[curEvent!!.sources.size - 1]
                        if (tag == "PAGE") sc.page = value
                    }
                }
            } else if ("FAM" == currentRecord && curFam != null) {
                if (level == 1) {
                    context.clear()
                    context.push(tag)
                    when (tag) {
                        "HUSB" -> curFam.husbXref = value
                        "WIFE" -> curFam.wifeXref = value
                        "CHIL" -> if (value != null) curFam.childXrefs.add(value)
                        "MARR" -> {
                            curEvent = EventRec().also { it.type = tag }
                            curFam.events.add(curEvent!!)
                        }
                        "NOTE" -> if (value != null) curFam.noteXrefs.add(value)
                        "OBJE" -> if (value != null) curFam.mediaXrefs.add(value)
                        "_TAG" -> if (value != null) curFam.tags.add(value)
                    }
                } else if (level == 2) {
                    val ctx = context.peek()
                    if (ctx == "MARR" && curEvent != null) {
                        when (tag) {
                            "DATE" -> {
                                curEvent!!.date = value
                                curFam.marrDate = value
                            }
                            "PLAC" -> {
                                curEvent!!.place = value
                                curFam.marrPlace = value
                            }
                            "SOUR" -> if (value != null) {
                                val sc = SourceCitationRec()
                                sc.sourceXref = value
                                curEvent!!.sources.add(sc)
                            }
                        }
                    }
                }
            } else if ("NOTE" == currentRecord && curNote != null) {
                if (level == 1) {
                    context.clear()
                    context.push(tag)
                    when (tag) {
                        "CONC" -> if (value != null && textAccumulator != null) {
                            textAccumulator!!.append(value)
                        }
                        "CONT" -> if (textAccumulator != null) {
                            textAccumulator!!.append("\n")
                            if (value != null) textAccumulator!!.append(value)
                        }
                        "SOUR" -> if (value != null) {
                            val sc = SourceCitationRec()
                            sc.sourceXref = value
                            curNote.sources.add(sc)
                        }
                    }
                } else if (level == 2) {
                    val ctx = context.peek()
                    if (ctx == "SOUR" && curNote.sources.isNotEmpty()) {
                        val sc = curNote.sources[curNote.sources.size - 1]
                        if (tag == "PAGE") sc.page = value
                    }
                }
            } else if ("OBJE" == currentRecord && curMedia != null) {
                if (level == 1) {
                    when (tag) {
                        "FILE" -> curMedia.fileName = value
                        "_PATH" -> curMedia.relativePath = value
                    }
                }
            } else if ("SOUR" == currentRecord && curSource != null) {
                if (level == 1) {
                    context.clear()
                    context.push(tag)
                    when (tag) {
                        "TITL" -> curSource.title = value
                        "ABBR" -> curSource.abbreviation = value
                        "REPO" -> curSource.repoXref = value
                        "DATA" -> { /* DATA sub-structure */ }
                    }
                } else if (level == 2) {
                    val ctx = context.peek()
                    if (ctx == "DATA") {
                        if (tag == "AGNC") {
                            curSource.agency = value
                        }
                    } else if (ctx == "REPO") {
                        if (tag == "CALN") {
                            curSource.callNumber = value
                        }
                    }
                }
            } else if ("REPO" == currentRecord && curRepo != null) {
                if (level == 1) {
                    context.clear()
                    context.push(tag)
                    when (tag) {
                        "NAME" -> curRepo.name = value
                        "ADDR" -> {
                            curAddress = AddressRec()
                            curRepo.address = curAddress
                        }
                        "PHON" -> curRepo.phone = value
                    }
                } else if (level == 2) {
                    val ctx = context.peek()
                    if (ctx == "ADDR" && curAddress != null) {
                        when (tag) {
                            "ADR1" -> curAddress!!.line1 = value
                            "ADR2" -> curAddress!!.line2 = value
                            "CITY" -> curAddress!!.city = value
                            "STAE" -> curAddress!!.state = value
                            "POST" -> curAddress!!.postalCode = value
                            "CTRY" -> curAddress!!.country = value
                        }
                    }
                }
            } else if ("SUBM" == currentRecord && curSubmitter != null) {
                if (level == 1) {
                    context.clear()
                    context.push(tag)
                    when (tag) {
                        "NAME" -> curSubmitter.name = value
                        "ADDR" -> {
                            curAddress = AddressRec()
                            curSubmitter.address = curAddress
                        }
                        "PHON" -> curSubmitter.phone = value
                    }
                } else if (level == 2) {
                    val ctx = context.peek()
                    if (ctx == "ADDR" && curAddress != null) {
                        when (tag) {
                            "ADR1" -> curAddress!!.line1 = value
                            "ADR2" -> curAddress!!.line2 = value
                            "CITY" -> curAddress!!.city = value
                            "STAE" -> curAddress!!.state = value
                            "POST" -> curAddress!!.postalCode = value
                            "CTRY" -> curAddress!!.country = value
                        }
                    }
                }
            }
        }

        if (textAccumulator != null && curNote != null) {
            curNote.text = textAccumulator.toString()
        }

        // Build model objects
        val data = ProjectRepository.ProjectData()

        // Notes map
        val noteById = HashMap<String, Note>()
        for ((_, r) in notes) {
            val note = Note()
            if (r.text != null) note.text = r.text
            r.xref?.let { noteById[it] = note }
        }

        // Media map
        val mediaById = HashMap<String, MediaAttachment>()
        for ((_, r) in media) {
            val m = MediaAttachment()
            if (r.fileName != null) m.fileName = r.fileName
            if (r.relativePath != null) m.relativePath = r.relativePath
            r.xref?.let { mediaById[it] = m }
        }

        // Repositories
        val repoById = HashMap<String, Repository>()
        for ((_, r) in repos) {
            val repo = Repository()
            if (r.name != null) repo.name = r.name
            if (r.phone != null) repo.phone = r.phone
            if (r.address != null) {
                val addr = Address()
                addr.line1 = r.address!!.line1
                addr.line2 = r.address!!.line2
                addr.city = r.address!!.city
                addr.state = r.address!!.state
                addr.postalCode = r.address!!.postalCode
                addr.country = r.address!!.country
                repo.address = addr
            }
            data.repositories.add(repo)
            r.xref?.let { repoById[it] = repo }
        }

        // Sources
        val sourceById = HashMap<String, Source>()
        for ((_, r) in sources) {
            val src = Source()
            if (r.title != null) src.title = r.title
            if (r.abbreviation != null) src.abbreviation = r.abbreviation
            if (r.agency != null) src.agency = r.agency
            if (r.callNumber != null) src.callNumber = r.callNumber
            if (r.repoXref != null) {
                val repo = repoById[r.repoXref!!]
                if (repo != null) src.repositoryId = repo.id
            }
            data.sources.add(src)
            r.xref?.let { sourceById[it] = src }
        }

        // Attach source citations to notes
        for ((_, r) in notes) {
            val note = r.xref?.let { noteById[it] }
            if (note != null) {
                for (scr in r.sources) {
                    val src = sourceById[scr.sourceXref]
                    if (src != null) {
                        val sc = SourceCitation()
                        sc.sourceId = src.id
                        sc.page = scr.page
                        note.sources.add(sc)
                    }
                }
            }
        }

        // Submitters
        for ((_, r) in submitters) {
            val subm = Submitter()
            if (r.name != null) subm.name = r.name
            if (r.phone != null) subm.phone = r.phone
            if (r.address != null) {
                val addr = Address()
                addr.line1 = r.address!!.line1
                addr.line2 = r.address!!.line2
                addr.city = r.address!!.city
                addr.state = r.address!!.state
                addr.postalCode = r.address!!.postalCode
                addr.country = r.address!!.country
                subm.address = addr
            }
            data.submitters.add(subm)
        }

        // Individuals
        val indiIdByXref = HashMap<String, String>()
        for ((_, r) in inds) {
            val given = r.given ?: ""
            val surname = r.surname ?: ""
            val sex = r.sex ?: Gender.UNKNOWN
            val ind = Individual(if (given.isEmpty()) "?" else given, surname, sex)

            for (ev in r.events) {
                val event = GedcomEvent()
                event.type = ev.type
                event.date = ev.date
                event.place = ev.place
                for (scr in ev.sources) {
                    val src = sourceById[scr.sourceXref]
                    if (src != null) {
                        val sc = SourceCitation()
                        sc.sourceId = src.id
                        sc.page = scr.page
                        event.sources.add(sc)
                    }
                }
                if (r.pedigree != null && ev.type == "ADOP") {
                    val attr = GedcomAttribute()
                    attr.tag = "PEDI"
                    attr.value = r.pedigree
                    event.attributes.add(attr)
                }
                ind.events.add(event)
            }

            for (noteXref in r.noteXrefs) {
                val note = noteById[noteXref]
                if (note != null) ind.notes.add(note)
            }
            for (mediaXref in r.mediaXrefs) {
                val m = mediaById[mediaXref]
                if (m != null) ind.media.add(m)
            }
            for (tagName in r.tags) {
                val tagObj = Tag()
                tagObj.name = tagName
                ind.tags.add(tagObj)
            }

            data.individuals.add(ind)
            r.xref?.let { indiIdByXref[it] = ind.id }
        }

        // Families
        val famIdByXref = HashMap<String, String>()
        for ((_, r) in fams) {
            val fam = Family()
            if (r.husbXref != null) fam.husbandId = indiIdByXref[r.husbXref!!]
            if (r.wifeXref != null) fam.wifeId = indiIdByXref[r.wifeXref!!]
            for (cx in r.childXrefs) {
                val id = indiIdByXref[cx]
                if (id != null) fam.childrenIds.add(id)
            }

            for (ev in r.events) {
                val event = GedcomEvent()
                event.type = ev.type
                event.date = ev.date
                event.place = ev.place
                for (scr in ev.sources) {
                    val src = sourceById[scr.sourceXref]
                    if (src != null) {
                        val sc = SourceCitation()
                        sc.sourceId = src.id
                        sc.page = scr.page
                        event.sources.add(sc)
                    }
                }
                fam.events.add(event)
            }

            for (noteXref in r.noteXrefs) {
                val note = noteById[noteXref]
                if (note != null) fam.notes.add(note)
            }
            for (mediaXref in r.mediaXrefs) {
                val m = mediaById[mediaXref]
                if (m != null) fam.media.add(m)
            }
            for (tagName in r.tags) {
                val tagObj = Tag()
                tagObj.name = tagName
                fam.tags.add(tagObj)
            }

            data.families.add(fam)
            r.xref?.let { famIdByXref[it] = fam.id }

            if (fam.husbandId != null) {
                val rel = Relationship()
                rel.type = Relationship.Type.SPOUSE_TO_FAMILY
                rel.fromId = fam.husbandId
                rel.toId = fam.id
                data.relationships.add(rel)
            }
            if (fam.wifeId != null) {
                val rel = Relationship()
                rel.type = Relationship.Type.SPOUSE_TO_FAMILY
                rel.fromId = fam.wifeId
                rel.toId = fam.id
                data.relationships.add(rel)
            }
            for (cid in fam.childrenIds) {
                val rel = Relationship()
                rel.type = Relationship.Type.FAMILY_TO_CHILD
                rel.fromId = fam.id
                rel.toId = cid
                data.relationships.add(rel)
            }
        }

        return data
    }

    // Internal record structures (mirror Java classes)
    private class IndiRec {
        var xref: String? = null
        var given: String? = null
        var surname: String? = null
        var sex: Gender? = null
        var birthDate: String? = null
        var birthPlace: String? = null
        var deathDate: String? = null
        var deathPlace: String? = null
        var pedigree: String? = null
        val events: MutableList<EventRec> = ArrayList()
        val famcXrefs: MutableList<String> = ArrayList()
        val famsXrefs: MutableList<String> = ArrayList()
        val noteXrefs: MutableList<String> = ArrayList()
        val mediaXrefs: MutableList<String> = ArrayList()
        val tags: MutableList<String> = ArrayList()
    }

    private class FamRec {
        var xref: String? = null
        var husbXref: String? = null
        var wifeXref: String? = null
        val childXrefs: MutableList<String> = ArrayList()
        var marrDate: String? = null
        var marrPlace: String? = null
        val events: MutableList<EventRec> = ArrayList()
        val noteXrefs: MutableList<String> = ArrayList()
        val mediaXrefs: MutableList<String> = ArrayList()
        val tags: MutableList<String> = ArrayList()
    }

    private class NoteRec {
        var xref: String? = null
        var text: String? = null
        val sources: MutableList<SourceCitationRec> = ArrayList()
    }

    private class MediaRec {
        var xref: String? = null
        var fileName: String? = null
        var relativePath: String? = null
    }

    private class EventRec {
        var type: String? = null
        var date: String? = null
        var place: String? = null
        val sources: MutableList<SourceCitationRec> = ArrayList()
    }

    private class SourceCitationRec {
        var sourceXref: String? = null
        var page: String? = null
    }

    private class SourceRec {
        var xref: String? = null
        var title: String? = null
        var abbreviation: String? = null
        var agency: String? = null
        var repoXref: String? = null
        var callNumber: String? = null
    }

    private class RepositoryRec {
        var xref: String? = null
        var name: String? = null
        var phone: String? = null
        var address: AddressRec? = null
    }

    private class SubmitterRec {
        var xref: String? = null
        var name: String? = null
        var phone: String? = null
        var address: AddressRec? = null
    }

    private class AddressRec {
        var line1: String? = null
        var line2: String? = null
        var line3: String? = null
        var city: String? = null
        var state: String? = null
        var postalCode: String? = null
        var country: String? = null
    }
}
