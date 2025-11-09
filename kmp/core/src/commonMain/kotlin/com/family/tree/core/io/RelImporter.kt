package com.family.tree.core.io

import com.family.tree.core.ProjectData
import com.family.tree.core.layout.ProjectLayout
import com.family.tree.core.model.*

/**
 * Heuristic importer for binary .rel files produced by "Relatives/Родословная".
 *
 * The .rel format is a binary TLV, but it embeds all tag names (NAME, SEX, BIRT, DEAT, DATE, PLAC,
 * FAMS/FAMC, HUSB/WIFE/CHIL, as well as record markers like P## and F##) and string values as UTF‑8
 * byte sequences. To keep the implementation small and robust without a full TLV parser, we:
 *  - read all bytes;
 *  - drop all 0x00 bytes (common as padding/short fields);
 *  - interpret the remaining byte stream as UTF‑8 text;
 *  - parse individuals (P###) and families (F###) using regex over this text.
 *
 * This tolerant approach is sufficient for typical .rel files and won't crash on unknown fields.
 */
class RelImporter {

    data class Bundle(
        val persons: Map<String, PersonRec>,
        val fams: Map<String, FamRec>,
        val commonSources: Map<Int, String>
    )

    data class PersonRec(
        var name: String? = null,
        var given: String? = null,
        var surname: String? = null,
        var sex: Gender? = null,
        var birth: String? = null,
        var birthPlace: String? = null,
        var birthSource: String? = null,
        var birthPage: String? = null,
        var death: String? = null,
        var deathPlace: String? = null,
        var deathSource: String? = null,
        var deathPage: String? = null,
        var posX: Double? = null,
        var posY: Double? = null,
        val notes: MutableList<String> = mutableListOf(),
        val media: MutableList<MediaRec> = mutableListOf()
    )

    data class FamRec(
        var husb: String? = null,
        var wife: String? = null,
        val children: MutableList<String?> = mutableListOf(),
        var marriageDate: String? = null,
        var marriagePlace: String? = null
    )

    data class MediaRec(
        var form: String? = null,
        var title: String? = null,
        var file: String? = null
    )

    fun importFromBytes(bytes: ByteArray, layout: ProjectLayout? = null): LoadedProject {
        // Remove all NUL bytes (0x00) – they are abundant in .rel TLV and break textual parsing
        val cleaned = stripZeroBytes(bytes)

        // First attempt: UTF-8
        var textUtf8 = cleaned.decodeToString()
        textUtf8 = stripSubmitterBlocks(textUtf8)
        var bundle = parseBundle(textUtf8)
        println("[DEBUG_LOG] RelImporter: After UTF-8 parse - persons=${bundle.persons.size}, families=${bundle.fams.size}")

        val poorNames = bundle.persons.values.all { r ->
            (r.given.isNullOrBlank()) && (r.surname.isNullOrBlank()) && (r.name.isNullOrBlank())
        }
        
        // If all names look missing and there are high-bytes, try Windows-1251 decoding fallback
        if (poorNames && hasNonAscii(cleaned)) {
            try {
                var textCp1251 = decodeWindows1251(cleaned)
                textCp1251 = stripSubmitterBlocks(textCp1251)
                val alt = parseBundle(textCp1251)
                val altBetter = alt.persons.values.any { r ->
                    !r.given.isNullOrBlank() || !r.surname.isNullOrBlank() || !r.name.isNullOrBlank()
                }
                if (altBetter) bundle = alt
            } catch (_: Exception) {
                // keep UTF-8 result if fallback fails
            }
        }

        // Build model
        val individuals = mutableListOf<Individual>()
        val families = mutableListOf<Family>()
        val sources = mutableListOf<Source>()

        // Add global/common SOUR1..SOUR6 as Source records first so we can link citations
        val sourNumToId: MutableMap<Int, String> = mutableMapOf()
        if (bundle.commonSources.isNotEmpty()) {
            for ((key, full) in bundle.commonSources) {
                val abbr = extractSourceTag(full, "ABBR")
                val titl = extractSourceTag(full, "TITL")
                val title = if (!titl.isNullOrBlank()) {
                    titl.trim()
                } else {
                    val nl = full.indexOf('\n')
                    val provisional = if (nl >= 0) full.substring(0, nl).trim() else full.trim()
                    val short = if (provisional.length > 80) provisional.substring(0, 80) + "…" else provisional
                    if (short.isBlank()) "SOUR $key" else short
                }
                val src = Source(
                    id = SourceId.generate(),
                    title = title,
                    abbreviation = abbr ?: "",
                    text = full
                )
                sources.add(src)
                sourNumToId[key] = src.id.value
            }
        }

        val idMap: MutableMap<String, String> = mutableMapOf() // P### -> Individual.id
        
        // Collect referenced person section ids from families to avoid dropping real, unnamed members
        val referenced: MutableSet<String> = mutableSetOf()
        for (rf0 in bundle.fams.values) {
            rf0.husb?.let { referenced.add(it) }
            rf0.wife?.let { referenced.add(it) }
            for (cx0 in rf0.children) cx0?.let { referenced.add(it) }
        }

        println("[DEBUG_LOG] RelImporter: Processing ${bundle.persons.size} person records, ${referenced.size} referenced by families")
        
        // Keep track of person records for coordinate extraction
        val personRecByKey = mutableMapOf<String, PersonRec>()
        
        for ((key, r) in bundle.persons) {
            println("[DEBUG_LOG] RelImporter: Person $key - name='${r.name}', given='${r.given}', surname='${r.surname}', sex=${r.sex}")
            var first: String
            var last: String
            
            // Prefer NAME field if it looks well-structured
            val nameLooksStructured = !r.name.isNullOrBlank() && (
                r.name!!.indexOf('/') >= 0 || r.name!!.any { (it.code and 0x80) != 0 }
            )
            
            if (nameLooksStructured) {
                val nm = splitName(r.name)
                first = nm[0]
                last = nm[1]
            } else if (!r.given.isNullOrBlank() || !r.surname.isNullOrBlank()) {
                first = if (!r.given.isNullOrBlank()) cleanToken(r.given) ?: "?" else "?"
                last = if (r.surname != null) cleanToken(r.surname) ?: "" else ""
            } else {
                val nm = splitName(r.name)
                first = nm[0]
                last = nm[1]
            }
            
            // Clean names
            first = cleanNameValue(first) ?: first
            last = cleanNameValue(last) ?: last
            first = stripParensQuotes(first) ?: first
            last = stripParensQuotes(last) ?: last
            
            if (first.isBlank()) first = "?"
            if (last.isEmpty()) last = ""
            
            println("[DEBUG_LOG] RelImporter: Person $key after processing - first='$first', last='$last'")
            
            // Skip empty/garbage persons if not referenced
            val nameBlank = (first.isBlank() || first == "?") && last.isBlank()
            val sourceNameMissing = r.name.isNullOrBlank()
            val looksEmpty = nameBlank && sourceNameMissing && r.birth == null && r.death == null && 
                             (r.sex == null || r.sex == Gender.UNKNOWN)
            if (looksEmpty && !referenced.contains(key)) {
                println("[DEBUG_LOG] RelImporter: SKIPPING person $key - looksEmpty=$looksEmpty, referenced=${referenced.contains(key)}")
                continue
            }
            println("[DEBUG_LOG] RelImporter: KEEPING person $key")

            val events = mutableListOf<GedcomEvent>()
            
            // BIRT event
            val birthSrc = r.birthSource
            if (!r.birth.isNullOrBlank() || !r.birthPlace.isNullOrBlank() || !birthSrc.isNullOrBlank()) {
                val eventSources = mutableListOf<SourceCitation>()
                if (!birthSrc.isNullOrBlank()) {
                    val refId = resolveSourRef(birthSrc, sourNumToId)
                    val sc = if (refId != null) {
                        SourceCitation(
                            id = SourceCitationId.generate(),
                            sourceId = SourceId(refId),
                            page = r.birthPage ?: ""
                        )
                    } else {
                        SourceCitation(
                            id = SourceCitationId.generate(),
                            text = birthSrc
                        )
                    }
                    eventSources.add(sc)
                }
                events.add(GedcomEvent(
                    id = GedcomEventId.generate(),
                    type = "BIRT",
                    date = r.birth ?: "",
                    place = r.birthPlace ?: "",
                    sources = eventSources
                ))
            }
            
            // DEAT event
            val deathSrc = r.deathSource
            if (!r.death.isNullOrBlank() || !r.deathPlace.isNullOrBlank() || !deathSrc.isNullOrBlank()) {
                val eventSources = mutableListOf<SourceCitation>()
                if (!deathSrc.isNullOrBlank()) {
                    val refId = resolveSourRef(deathSrc, sourNumToId)
                    val sc = if (refId != null) {
                        SourceCitation(
                            id = SourceCitationId.generate(),
                            sourceId = SourceId(refId),
                            page = r.deathPage ?: ""
                        )
                    } else {
                        SourceCitation(
                            id = SourceCitationId.generate(),
                            text = deathSrc
                        )
                    }
                    eventSources.add(sc)
                }
                events.add(GedcomEvent(
                    id = GedcomEventId.generate(),
                    type = "DEAT",
                    date = r.death ?: "",
                    place = r.deathPlace ?: "",
                    sources = eventSources
                ))
            }
            
            // Notes
            val notes = r.notes.map { noteText ->
                Note(id = NoteId.generate(), text = noteText)
            }
            
            // Media
            val media = r.media.map { m ->
                MediaAttachment(
                    id = MediaAttachmentId.generate(),
                    fileName = m.title ?: m.file ?: "",
                    relativePath = m.file ?: ""
                )
            }

            val ind = Individual(
                id = IndividualId(uuid4()),
                firstName = first,
                lastName = last,
                gender = r.sex ?: Gender.UNKNOWN,
                events = events,
                notes = notes,
                media = media
            )
            individuals.add(ind)
            idMap[key] = ind.id.value
            personRecByKey[key] = r
        }

        // Build families
        for ((fkey, rf) in bundle.fams) {
            val husbandId = rf.husb?.let { idMap[it] }?.let { IndividualId(it) }
            val wifeId = rf.wife?.let { idMap[it] }?.let { IndividualId(it) }
            val childrenIds = rf.children.mapNotNull { child ->
                child?.let { idMap[it] }?.let { IndividualId(it) }
            }
            
            val famEvents = mutableListOf<GedcomEvent>()
            if (!rf.marriageDate.isNullOrBlank() || !rf.marriagePlace.isNullOrBlank()) {
                famEvents.add(GedcomEvent(
                    id = GedcomEventId.generate(),
                    type = "MARR",
                    date = rf.marriageDate ?: "",
                    place = rf.marriagePlace ?: ""
                ))
            }

            val fam = Family(
                id = FamilyId(uuid4()),
                husbandId = husbandId,
                wifeId = wifeId,
                childrenIds = childrenIds,
                events = famEvents
            )
            families.add(fam)
        }

        // Build ProjectLayout with coordinates from .rel file
        val nodePositions = mutableMapOf<String, com.family.tree.core.layout.NodePos>()
        for ((key, personRec) in personRecByKey) {
            val individualId = idMap[key]
            if (individualId != null && personRec.posX != null && personRec.posY != null) {
                nodePositions[individualId] = com.family.tree.core.layout.NodePos(
                    x = personRec.posX!!,
                    y = personRec.posY!!
                )
            }
        }
        
        val importedLayout = if (nodePositions.isNotEmpty()) {
            ProjectLayout(
                zoom = 1.0,
                viewOriginX = 0.0,
                viewOriginY = 0.0,
                nodePositions = nodePositions,
                positionsAreCenters = false
            )
        } else {
            layout // Use passed-in layout if no coordinates were imported
        }

        val data = ProjectData(
            individuals = individuals,
            families = families,
            sources = sources
        )

        return LoadedProject(data = data, layout = importedLayout, meta = null)
    }

    private fun stripZeroBytes(bytes: ByteArray): ByteArray {
        return bytes.filter { it != 0.toByte() }.toByteArray()
    }

    private fun hasNonAscii(bytes: ByteArray): Boolean {
        return bytes.any { (it.toInt() and 0x80) != 0 }
    }

    private fun stripSubmitterBlocks(text: String): String {
        // Some .rel exports include large submitter/author blocks wrapped by SUBM … SUBM1.
        // These blocks can inject noise into NAME and other fields. Strip them early.
        // Strategy: keep only the content BETWEEN the first SUBM and the last SUBM1 (removes header before SUBM).
        if (text.isEmpty()) return text
        val up = text.uppercase()
        val first = up.indexOf("SUBM")
        val last = up.lastIndexOf("SUBM1")
        if (first >= 0 && last > first) {
            var start = first + 4 // after 'SUBM'
            // Trim leading whitespace/newlines after SUBM and before SUBM1
            while (start < text.length && text[start].isWhitespace()) start++
            var end = last
            while (end > start && text[end - 1].isWhitespace()) end--
            return text.substring(start, end)
        }
        return text
    }

    private fun parseBundle(text: String): Bundle {
        val persons = mutableMapOf<String, PersonRec>()
        val fams = mutableMapOf<String, FamRec>()
        val commonSources = mutableMapOf<Int, String>()

        // Extract common sources (SOUR1, SOUR2, etc.)
        val sourPattern = Regex("SOUR(\\d+)\\s+([\\s\\S]+?)(?=(?:SOUR\\d+|P\\d+|F\\d+|$))")
        for (match in sourPattern.findAll(text)) {
            val num = match.groupValues[1].toIntOrNull() ?: continue
            val content = match.groupValues[2].trim()
            commonSources[num] = content
        }

        // Find all P### and F### sections (simple pattern matching all candidates)
        val sectionPattern = Regex("(P\\d{1,5}|F\\d{1,5})")
        val candidateMatches = sectionPattern.findAll(text).toList()
        
        // Filter to likely section starts by checking context
        val validMatches = mutableListOf<Pair<Int, String>>()
        for (match in candidateMatches) {
            val sectionId = match.value
            val idx = match.range.first
            
            // Avoid matching cross-references like "HUSB P1" or "CHIL P3" by checking previous character
            if (idx > 0) {
                val prev = text[idx - 1]
                if (prev.isLetterOrDigit()) continue
            }
            
            // Look ahead for expected tags to confirm this is a real section
            val windowEnd = minOf(text.length, idx + 200)
            val lookahead = text.substring(idx, windowEnd).uppercase()
            val isValid = if (sectionId.startsWith("P")) {
                lookahead.contains("NAME") || lookahead.contains("SEX") || 
                lookahead.contains("GIVN") || lookahead.contains("SURN") || 
                lookahead.contains("_X") || lookahead.contains("_Y")
            } else { // F###
                lookahead.contains("HUSB") || lookahead.contains("WIFE") || 
                lookahead.contains("CHIL") || lookahead.contains("MARR")
            }
            
            if (isValid) {
                validMatches.add(Pair(idx, sectionId))
            }
        }
        
        println("[DEBUG_LOG] RelImporter.parseBundle: Found ${validMatches.size} valid section markers (from ${candidateMatches.size} candidates)")
        
        for (i in validMatches.indices) {
            val (startIdx, sectionId) = validMatches[i]
            val start = startIdx + sectionId.length // body begins after id
            val end = if (i < validMatches.size - 1) validMatches[i + 1].first else text.length
            val block = text.substring(start, end)

            if (sectionId.startsWith("P")) {
                persons[sectionId] = parsePersonBlock(block)
            } else if (sectionId.startsWith("F")) {
                println("[DEBUG_LOG] RelImporter.parseBundle: Processing family section $sectionId")
                fams[sectionId] = parseFamilyBlock(block)
            }
        }
        
        println("[DEBUG_LOG] RelImporter.parseBundle: Parsed ${persons.size} persons, ${fams.size} families")

        return Bundle(persons, fams, commonSources)
    }

    private fun parsePersonBlock(block: String): PersonRec {
        val rec = PersonRec()

        // For name extraction, pre-strip OBJE and NOTE(S) blocks to avoid contamination (matching JavaFX logic)
        val objePatternForName = Regex("OBJE\\s*[\\s\\S]+?(?=(?:OBJE|NOTE|NOTES|SOUR|SEX|BIRT|DEAT|FAMC|FAMS|SUBM|P\\d+|F\\d+|_X|_Y)|$)", RegexOption.IGNORE_CASE)
        val notePatternForName = Regex("(?:NOTE|NOTES)\\s*[\\s\\S]+?(?=(?:SOUR|SEX|BIRT|DEAT|FAMC|FAMS|NOTE|NOTES|TITL|SUBM|P\\d+|F\\d+|_X|_Y)|$)", RegexOption.IGNORE_CASE)
        var blockForName = objePatternForName.replace(block, " ")
        blockForName = notePatternForName.replace(blockForName, " ")

        // NAME - try multiple patterns for binary .rel format
        // Pattern 1: Standard NAME with content on same or next line (with optional prefix character)
        var nameMatch = Regex("NAME\\s*([\\s\\S]+?)(?=\\s*(?:SEX|BIRT|DEAT|FAMC|FAMS|NOTE|GIVN|SURN|P\\d+|F\\d+|_X|_Y)|$)").find(blockForName)
        if (nameMatch != null) {
            rec.name = nameMatch.groupValues[1].trim()
        }
        // Pattern 2: Corrupted NAME (sometimes first char is corrupted in binary format)
        if (rec.name.isNullOrBlank()) {
            nameMatch = Regex("(?:NAME|.AME)\\s*([\\s\\S]+?)(?=\\s*(?:SEX|BIRT|DEAT|FAMC|FAMS|NOTE|P\\d+|F\\d+|_X|_Y)|$)").find(blockForName)
            if (nameMatch != null) {
                rec.name = nameMatch.groupValues[1].trim()
            }
        }
        // Pattern 3: Inline GEDCOM-style name anywhere before SEX
        if (rec.name.isNullOrBlank()) {
            val gedcomInline = Regex("[\\p{L} .]+?\\s+/[^/]+/").find(blockForName)
            if (gedcomInline != null) {
                rec.name = gedcomInline.value.trim()
            }
        }
        
        // GIVN and SURN (also use cleaned block)
        rec.given = extractTag(blockForName, "GIVN")
        rec.surname = extractTag(blockForName, "SURN")
        
        // Russian alternatives
        if (rec.given.isNullOrBlank()) {
            rec.given = extractTag(block, "ИМЯ", ignoreCase = true)
        }
        if (rec.surname.isNullOrBlank()) {
            val rusPattern = Regex("ФАМИЛИ[ЯИ]\\P{L}*([^\\r\\n]+)", RegexOption.IGNORE_CASE)
            rec.surname = rusPattern.find(block)?.groupValues?.get(1)?.trim()
        }

        // SEX
        val sexMatch = Regex("SEX\\P{L}*([MFmf12МЖмж])").find(block)
        if (sexMatch != null) {
            val sexChar = sexMatch.groupValues[1].uppercase()
            rec.sex = when {
                sexChar.contains("M") || sexChar.contains("1") || sexChar.contains("М") -> Gender.MALE
                sexChar.contains("F") || sexChar.contains("2") || sexChar.contains("Ж") -> Gender.FEMALE
                else -> Gender.UNKNOWN
            }
        }

        // BIRT - extract date and place first
        val birtMatch = Regex("BIRT.*?DATE\\s*([^\\x00]+?)(?=\\s*(?:PLAC|SOUR|PAGE|DEAT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)").find(block)
        rec.birth = birtMatch?.groupValues?.get(1)?.trim()
        
        val birtPlacMatch = Regex("BIRT.*?PLAC\\s*([^\\r\\n]+?)(?=(?:DEAT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)").find(block)
        rec.birthPlace = birtPlacMatch?.groupValues?.get(1)?.trim()
        
        // Extract SOUR and PAGE inside BIRT block boundaries (matching JavaFX logic)
        val birtIdx = block.indexOf("BIRT", ignoreCase = true)
        if (birtIdx >= 0) {
            var birtEnd = indexOfNextTagExcludingSourPage(block, birtIdx + 4)
            if (birtEnd < 0) birtEnd = block.length
            val birtBlock = block.substring(birtIdx, birtEnd)
            val bSour = extractAfterToken("SOUR", birtBlock)
            if (!bSour.isNullOrBlank()) rec.birthSource = bSour
            val bPage = extractAfterToken("PAGE", birtBlock)
            if (!bPage.isNullOrBlank()) rec.birthPage = bPage
        }

        // DEAT - extract date and place first
        val deatMatch = Regex("DEAT.*?DATE\\s*([^\\x00]+?)(?=\\s*(?:PLAC|SOUR|PAGE|BIRT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)").find(block)
        rec.death = deatMatch?.groupValues?.get(1)?.trim()
        
        val deatPlacMatch = Regex("DEAT.*?PLAC\\s*([^\\r\\n]+?)(?=(?:BIRT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)").find(block)
        rec.deathPlace = deatPlacMatch?.groupValues?.get(1)?.trim()
        
        // Extract SOUR and PAGE inside DEAT block boundaries (matching JavaFX logic)
        val deatIdx = block.indexOf("DEAT", ignoreCase = true)
        if (deatIdx >= 0) {
            var deatEnd = indexOfNextTagExcludingSourPage(block, deatIdx + 4)
            if (deatEnd < 0) deatEnd = block.length
            val deatBlock = block.substring(deatIdx, deatEnd)
            val dSour = extractAfterToken("SOUR", deatBlock)
            if (!dSour.isNullOrBlank()) rec.deathSource = dSour
            val dPage = extractAfterToken("PAGE", deatBlock)
            if (!dPage.isNullOrBlank()) rec.deathPage = dPage
        }

        // Position
        val xMatch = Regex("_X\\P{Alnum}*([+-]?\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE).find(block)
        rec.posX = xMatch?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        
        val yMatch = Regex("_Y\\P{Alnum}*([+-]?\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE).find(block)
        rec.posY = yMatch?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()

        // Notes
        val notePattern = Regex("(?:NOTE|NOTES)\\s*([\\s\\S]+?)\\s*(?=(SOUR|SEX|BIRT|DEAT|FAMC|FAMS|NOTE|NOTES|TITL|SUBM|P\\d+|F\\d+|_X|_Y)|$)", RegexOption.IGNORE_CASE)
        for (match in notePattern.findAll(block)) {
            val noteText = match.groupValues[1].trim()
            if (noteText.isNotBlank()) {
                rec.notes.add(noteText)
            }
        }

        // Media
        val objePattern = Regex("OBJE\\s*([\\s\\S]+?)\\s*(?=(OBJE|NOTE|NOTES|SOUR|SEX|BIRT|DEAT|FAMC|FAMS|SUBM|P\\d+|F\\d+|_X|_Y)|$)", RegexOption.IGNORE_CASE)
        for (match in objePattern.findAll(block)) {
            val objeBlock = match.groupValues[1]
            val media = MediaRec()
            media.form = Regex("FORM\\s*([^\\r\\n\\s]+)", RegexOption.IGNORE_CASE).find(objeBlock)?.groupValues?.get(1)?.trim()
            media.title = Regex("TITL\\s*([\\s\\S]+?)\\s*(?=(FORM|FILE|TITL|OBJE)|$)", RegexOption.IGNORE_CASE).find(objeBlock)?.groupValues?.get(1)?.trim()
            media.file = Regex("FILE\\s*([\\s\\S]+?)\\s*(?=(FORM|FILE|TITL|OBJE)|$)", RegexOption.IGNORE_CASE).find(objeBlock)?.groupValues?.get(1)?.trim()
            if (media.file != null || media.title != null) {
                rec.media.add(media)
            }
        }

        return rec
    }

    private fun parseFamilyBlock(block: String): FamRec {
        val rec = FamRec()
        
        println("[DEBUG_LOG] RelImporter.parseFamilyBlock: block length=${block.length}, first 200 chars: ${block.take(200)}")

        // HUSB
        val husbMatch = Regex("HUSB\\P{Alnum}*(P\\d+)", RegexOption.IGNORE_CASE).find(block)
        rec.husb = husbMatch?.groupValues?.get(1)
        println("[DEBUG_LOG] RelImporter.parseFamilyBlock: husbMatch=${husbMatch?.value}, husb=${rec.husb}")

        // WIFE
        val wifeMatch = Regex("WIFE\\P{Alnum}*(P\\d+)", RegexOption.IGNORE_CASE).find(block)
        rec.wife = wifeMatch?.groupValues?.get(1)
        println("[DEBUG_LOG] RelImporter.parseFamilyBlock: wifeMatch=${wifeMatch?.value}, wife=${rec.wife}")

        // CHIL
        val chilPattern = Regex("CHIL\\P{Alnum}*(P\\d+)", RegexOption.IGNORE_CASE)
        for (match in chilPattern.findAll(block)) {
            rec.children.add(match.groupValues[1])
        }
        println("[DEBUG_LOG] RelImporter.parseFamilyBlock: children count=${rec.children.size}, children=${rec.children}")

        // MARR
        val marrDateMatch = Regex("MARR.*?DATE\\((.+?)\\)").find(block)
        rec.marriageDate = marrDateMatch?.groupValues?.get(1)?.trim()
        
        val marrPlacMatch = Regex("MARR.*?PLAC\\s*([^PF\\r\\n]+)").find(block)
        rec.marriagePlace = marrPlacMatch?.groupValues?.get(1)?.trim()

        return rec
    }

    private fun extractTag(text: String, tag: String, ignoreCase: Boolean = false): String? {
        val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        val pattern = Regex("$tag\\s*([^\\r\\n]+)", options)
        return pattern.find(text)?.groupValues?.get(1)?.trim()
    }
    
    // Find index of the next top-level tag, but deliberately keep SOUR and PAGE inside the current block
    // so that BIRT/DEAT sub-block parsing can see them (matching JavaFX logic)
    private fun indexOfNextTagExcludingSourPage(text: String, from: Int): Int {
        val tags = listOf("SEX", "BIRT", "DEAT", "FAMC", "FAMS", "NOTE", "NOTES", "TITL", "OBJE", "P", "F", "_X", "_Y")
        var min = -1
        for (tag in tags) {
            val idx = text.indexOf(tag, from, ignoreCase = true)
            if (idx >= 0 && (min < 0 || idx < min)) {
                min = idx
            }
        }
        return min
    }
    
    // Extract value after a token until the next known tag (matching JavaFX extractAfterToken logic)
    private fun extractAfterToken(token: String, body: String): String? {
        // Find token (case-insensitive, with non-letter boundaries)
        val idx = body.indexOf(token, ignoreCase = true)
        if (idx < 0) return null
        
        var start = idx + token.length
        // Skip whitespace and punctuation
        while (start < body.length) {
            val ch = body[start]
            if (ch.isLetter() || ch == '/' || ch == '^' || ch.code in 0x0400..0x052F) {
                break
            }
            start++
        }
        
        // Find end at next tag
        val nextTagIdx = indexOfNextTag(body, start)
        val end = if (nextTagIdx < 0) body.length else nextTagIdx
        
        var raw = body.substring(start, end)
        // Strip BOM and Unicode replacement characters
        raw = raw.replace("\uFEFF", "").replace("\uFFFD", "")
        raw = raw.replace(Regex("[\\p{Cntrl}]"), "").trim()
        return if (raw.isBlank()) null else raw
    }
    
    // Find index of any next tag (for extractAfterToken)
    private fun indexOfNextTag(text: String, from: Int): Int {
        val tags = listOf("SEX", "BIRT", "DEAT", "FAMC", "FAMS", "NOTE", "NOTES", "SOUR", "TITL", "OBJE", "P", "F")
        var min = -1
        for (tag in tags) {
            val idx = text.indexOf(tag, from, ignoreCase = true)
            if (idx >= 0 && (min < 0 || idx < min)) {
                min = idx
            }
        }
        return min
    }

    private fun extractSourceTag(text: String, tag: String): String? {
        val pattern = Regex("$tag\\s*([\\s\\S]+?)(?=(ABBR|TITL|AUTH|PUBL|REPO|$))", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.groupValues?.get(1)?.trim()
    }

    private fun splitName(fullName: String?): Array<String> {
        if (fullName.isNullOrBlank()) return arrayOf("?", "")
        
        // GEDCOM style: "Given /Surname/"
        val gedcomPattern = Regex("([\\p{L} .]+?)\\s+/([^/]+)/")
        val gedcomMatch = gedcomPattern.find(fullName)
        if (gedcomMatch != null) {
            var given = gedcomMatch.groupValues[1].trim()
            var surname = gedcomMatch.groupValues[2].trim()
            // Clean burger symbols and other garbage from extracted values
            given = cleanToken(given) ?: given
            surname = cleanToken(surname) ?: surname
            return arrayOf(given.ifBlank { "?" }, surname)
        }
        
        // Fallback: split by whitespace
        val tokens = fullName.trim().split(Regex("\\s+"))
        val result = when {
            tokens.isEmpty() -> arrayOf("?", "")
            tokens.size == 1 -> arrayOf(tokens[0], "")
            else -> arrayOf(tokens[0], tokens.drop(1).joinToString(" "))
        }
        // Clean both parts
        result[0] = cleanToken(result[0]) ?: result[0]
        result[1] = cleanToken(result[1]) ?: result[1]
        return result
    }

    private fun cleanToken(s: String?): String? {
        if (s.isNullOrBlank()) return null
        var clean = s.trim()
        // Remove common garbage patterns
        clean = clean.replace(Regex("[\\x00-\\x1F]"), "")
        // Remove burger Unicode characters (≣, ≡, ≢) and block symbols
        clean = clean.replace("\uFEFF", "").replace("\uFFFD", "")
        clean = clean.replace("\u2261", "").replace("\u2262", "").replace("\u2263", "")
        clean = clean.replace("\u25A0", "").replace("\u25A1", "").replace("\u25AA", "").replace("\u25AB", "")
        return if (clean.isBlank()) null else clean
    }

    private fun cleanNameValue(s: String?): String? {
        if (s.isNullOrBlank()) return null
        var clean = s
        // Remove stray BOM and replacement characters that appear as black bars
        clean = clean.replace("\uFEFF", "").replace("\uFFFD", "")
        // Remove horizontal bar symbols (≣ U+2263 and similar block characters) that appear as "five-layer burgers"
        clean = clean.replace("\u2261", "").replace("\u2262", "").replace("\u2263", "") // ≡ ≢ ≣
        clean = clean.replace("\u25A0", "").replace("\u25A1", "").replace("\u25AA", "").replace("\u25AB", "") // ■ □ ▪ ▫
        // Remove leading digits and special characters (like "2", "4", "6", "*", etc.) that prefix names in .rel format
        clean = clean.replace(Regex("^[^\\p{L}]+"), "")
        // Remove trailing digits and special characters (burger symbols, etc.) that follow names in .rel format
        clean = clean.replace(Regex("[^\\p{L}]+$"), "")
        // Remove embedded GEDCOM tags
        clean = clean.replace(Regex("\\b(OBJE|TITL|FILE|NOTE|FORM|SEX|BIRT|DEAT)\\b.*", RegexOption.IGNORE_CASE), "")
        clean = clean.trim()
        return if (clean.isBlank()) null else clean
    }

    private fun stripParensQuotes(s: String?): String? {
        if (s.isNullOrBlank()) return null
        var clean = s.trim()
        // Remove parenthetical comments
        clean = clean.replace(Regex("\\([^)]*\\)"), "").trim()
        // Remove quotes
        clean = clean.replace(Regex("^[\"']|[\"']$"), "").trim()
        // Remove GEDCOM surname delimiters (/) that may be leftover - strip leading/trailing slashes and empty "//"
        clean = clean.replace("//", "").trim()
        clean = clean.replace(Regex("^/+|/+$"), "").trim()
        return if (clean.isBlank()) null else clean
    }

    private fun resolveSourRef(sourRef: String, sourNumToId: Map<Int, String>): String? {
        // Try to extract SOUR number reference
        val numMatch = Regex("SOUR(\\d+)").find(sourRef)
        if (numMatch != null) {
            val num = numMatch.groupValues[1].toIntOrNull()
            if (num != null) {
                return sourNumToId[num]
            }
        }
        return null
    }

    private fun decodeWindows1251(bytes: ByteArray): String {
        // Simple Windows-1251 decoder (common Cyrillic encoding)
        return buildString {
            for (b in bytes) {
                val code = b.toInt() and 0xFF
                append(when (code) {
                    in 0..127 -> code.toChar()
                    in 192..255 -> (code - 192 + 0x0410).toChar() // Cyrillic А-Я, а-я
                    else -> code.toChar()
                })
            }
        }
    }

    private fun uuid4(): String {
        val chars = "0123456789abcdef"
        return buildString(36) {
            for (i in 0 until 36) {
                if (i == 8 || i == 13 || i == 18 || i == 23) append('-')
                else append(chars.random())
            }
        }
    }
}
