package com.family.tree.rel

import com.family.tree.model.Family
import com.family.tree.model.Gender
import com.family.tree.model.ProjectLayout
import com.family.tree.storage.ProjectRepository
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

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
 * This tolerant approach is sufficient for typical .rel files and won’t crash on unknown fields.
 */
class RelImporter {

    companion object {
        private val DMY: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")

        private val SECTION_CANDIDATE: Pattern = Pattern.compile("(P\\d{1,5}|F\\d{1,5})")

        // Accept M/F as well as digits (1=male, 2=female) and Cyrillic (М/Ж); optional parentheses/spaces
        private val SEX_RE: Pattern = Pattern.compile("SEX\\P{L}*([MFmf12МЖмж])")
        private val NAME_RE: Pattern = Pattern.compile("NAME\\s*([\\s\\S]+?)\\s*(?=(SEX|BIRT|DEAT|FAMC|FAMS|NOTE|TITL|OBJE|SUBM|SOUR|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL)
        // Some files show a corrupted first letter in NAME (e.g., '\u044EAME' instead of 'NAME').
        private val NAME_ALT_RE: Pattern = Pattern.compile("(?:NAME|.AME)\\s*([\\s\\S]+?)\\s*(?=(SEX|BIRT|DEAT|FAMC|FAMS|NOTE|TITL|OBJE|SUBM|SOUR|P\\d+|F\\d+)|$)", Pattern.DOTALL)
        private val GIVN_RE: Pattern = Pattern.compile("GIVN\\P{L}*([^\\r\\n]+)")
        private val SURN_RE: Pattern = Pattern.compile("SURN\\P{L}*([^\\r\\n]+)")
        // Russian localized tokens (Имя, Фамилия, Отчество, Пол)
        private const val RU_FLAGS: Int = Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        private val RUS_GIVN_RE: Pattern = Pattern.compile("ИМЯ\\P{L}*([^\\r\\n]+)", RU_FLAGS)
        private val RUS_SURN_RE: Pattern = Pattern.compile("ФАМИЛИ[ЯИ]\\P{L}*([^\\r\\n]+)", RU_FLAGS)
        private val RUS_SEX_LETTER_RE: Pattern = Pattern.compile("ПОЛ\\P{L}*([МЖ])", RU_FLAGS)
        private val RUS_SEX_WORD_RE: Pattern = Pattern.compile("(Муж(ской)?|Жен(ский)?)", RU_FLAGS)
        private val BIRT_RE: Pattern = Pattern.compile("BIRT.*?DATE\\s*([^\\x00]+?)(?=\\s*(?:PLAC|SOUR|PAGE|DEAT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL)
        private val BIRT_PLAC_RE: Pattern = Pattern.compile("BIRT.*?PLAC\\s*([^\\r\\n]+?)(?=(?:DEAT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL)
        private val DEAT_RE: Pattern = Pattern.compile("DEAT.*?DATE\\s*([^\\x00]+?)(?=\\s*(?:PLAC|SOUR|PAGE|BIRT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL)
        private val DEAT_PLAC_RE: Pattern = Pattern.compile("DEAT.*?PLAC\\s*([^\\r\\n]+?)(?=(?:BIRT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL)
        private const val POS_FLAGS: Int = Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        private val POS_X_RE: Pattern = Pattern.compile("_X\\P{Alnum}*([+-]?\\d+(?:[.,]\\d+)?)", POS_FLAGS)
        private val POS_Y_RE: Pattern = Pattern.compile("_Y\\P{Alnum}*([+-]?\\d+(?:[.,]\\d+)?)", POS_FLAGS)
        // Fallback: inline GEDCOM-like "Given /Surname/" anywhere before SEX
        // Inline GEDCOM-like name: require a space before first slash to avoid matching URLs; capture Given and /Surname/
        private val INLINE_GEDCOM_NAME: Pattern = Pattern.compile("[\\p{L} .]+?\\s+/[^/]+/", Pattern.UNICODE_CASE)
        private val NOTE_BLOCK_RE: Pattern = Pattern.compile("(?:NOTE|NOTES)\\s*([\\s\\S]+?)\\s*(?=(SOUR|SEX|BIRT|DEAT|FAMC|FAMS|NOTE|NOTES|TITL|SUBM|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        // Multimedia: OBJE blocks possibly containing FORM/TITL/FILE
        // Do NOT terminate at TITL — TITL/FILE/FORM belong inside OBJE
        private val OBJE_BLOCK_RE: Pattern = Pattern.compile("OBJE\\s*([\\s\\S]+?)\\s*(?=(OBJE|NOTE|NOTES|SOUR|SEX|BIRT|DEAT|FAMC|FAMS|SUBM|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        private val FORM_INNER_RE: Pattern = Pattern.compile("FORM\\s*([^\\r\\n\\s]+)", Pattern.CASE_INSENSITIVE)
        private val TITL_INNER_RE: Pattern = Pattern.compile("TITL\\s*([\\s\\S]+?)\\s*(?=(FORM|FILE|TITL|OBJE|NOTE|NOTES|SOUR|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        private val FILE_INNER_RE: Pattern = Pattern.compile("FILE\\s*([\\s\\S]+?)\\s*(?=(FORM|FILE|TITL|OBJE|NOTE|NOTES|SOUR|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)

        private const val REL_FLAGS: Int = Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        private val HUSB_RE: Pattern = Pattern.compile("HUSB\\P{Alnum}*(P\\d+)", REL_FLAGS)
        private val WIFE_RE: Pattern = Pattern.compile("WIFE\\P{Alnum}*(P\\d+)", REL_FLAGS)
        private val CHIL_RE: Pattern = Pattern.compile("CHIL\\P{Alnum}*(P\\d+)", REL_FLAGS)
        private val MARR_DATE_RE: Pattern = Pattern.compile("MARR.*?DATE\\((.+?)\\)", Pattern.DOTALL)
        private val MARR_PLAC_RE: Pattern = Pattern.compile("MARR.*?PLAC\\s*([^PF\\r\\n]+)", Pattern.DOTALL)
    }

    @Throws(IOException::class)
    fun importFromFile(path: Path): ProjectRepository.ProjectData = importFromFileWithLayout(path, null)

    @Throws(IOException::class)
    fun importFromFileWithLayout(path: Path, layout: ProjectLayout?): ProjectRepository.ProjectData {
        Objects.requireNonNull(path, "path")
        val raw = Files.readAllBytes(path)
        // Remove all NUL bytes (0x00) – they are abundant in .rel TLV and break textual parsing
        val cleaned = stripZeroBytes(raw)

        // First attempt: UTF-8
        var textUtf8 = String(cleaned, StandardCharsets.UTF_8)
        textUtf8 = stripSubmitterBlocks(textUtf8)
        var bundle = parseBundle(textUtf8)

        val poorNames = bundle.persons.values.all { r ->
            (r.given.isNullOrBlank()) && (r.surname.isNullOrBlank()) && (r.name.isNullOrBlank())
        }
        // If all names look missing and there are high-bytes, try Windows-1251 decoding fallback
        if (poorNames && hasNonAscii(cleaned)) {
            try {
                var textCp1251 = String(cleaned, java.nio.charset.Charset.forName("windows-1251"))
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
        val data = ProjectRepository.ProjectData()
        // Add global/common SOUR1..SOUR6 as Source records first so we can link citations
        val sourNumToId: MutableMap<Int, String> = HashMap()
        if (bundle.commonSources.isNotEmpty()) {
            for (e in bundle.commonSources.entries) {
                val src = com.family.tree.model.Source()
                val full = e.value
                // Try to extract ABBR from the global SOURn block
                val abbr = extractSourceTag(full, "ABBR")
                if (!abbr.isNullOrBlank()) src.abbreviation = abbr
                // Derive a short title from TITL if present; otherwise from the first line or first 80 chars
                val titl = extractSourceTag(full, "TITL")
                val title = if (!titl.isNullOrBlank()) {
                    titl.trim()
                } else {
                    var t = full
                    val nl = t.indexOf('\n')
                    val provisional = if (nl >= 0) t.substring(0, nl).trim() else t.trim()
                    var short = if (provisional.length > 80) provisional.substring(0, 80) + "…" else provisional
                    if (short.isBlank()) short = "SOUR ${'$'}{e.key}"
                    short
                }
                src.title = title
                // Preserve full cleaned text for reference
                src.text = full
                data.sources.add(src)
                sourNumToId[e.key] = src.id
            }
        }
        val idMap: MutableMap<String, String> = HashMap() // P### -> Individual.id
        // Collect referenced person section ids from families to avoid dropping real, unnamed members
        val referenced: MutableSet<String> = HashSet()
        for (rf0 in bundle.fams.values) {
            rf0.husb?.let { referenced.add(it) }
            rf0.wife?.let { referenced.add(it) }
            for (cx0 in rf0.children) cx0?.let { referenced.add(it) }
        }
        for ((key, r) in bundle.persons) {
            var first: String
            var last: String
            // Prefer NAME field if it looks well-structured (e.g., contains /Surname/ or non-ASCII letters like Cyrillic)
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
            // Final defensive cleanup: ensure names don’t contain glued tags like OBJE/TITL/FILE/NOTE
            first = cleanNameValue(first) ?: first
            last = cleanNameValue(last) ?: last
            // Remove parenthetical/bracketed commentary and surrounding quotes from names
            first = stripParensQuotes(first) ?: first
            last = stripParensQuotes(last) ?: last
            if (first.isBlank()) first = "?"
            if (last.isEmpty()) last = ""
            // Skip obviously empty/garbage persons if they are not referenced by any family.
            // Be conservative: if the source NAME field exists (even if parsing to first/last failed), keep the person.
            val nameBlank = (first.isBlank() || first == "?") && last.isBlank()
            val sourceNameMissing = r.name.isNullOrBlank()
            val looksEmpty = nameBlank && sourceNameMissing && r.birth == null && r.death == null && (r.sex == null || r.sex == Gender.UNKNOWN)
            if (looksEmpty && !referenced.contains(key)) continue

            val ind = com.family.tree.model.Individual(first, last, r.sex ?: Gender.UNKNOWN)
            // Dates and places are stored only within events (BIRT/DEAT), not in top-level fields.
            // Create GedcomEvent entries for BIRT/DEAT with sources if available
            if (!r.birth.isNullOrBlank() || !r.birthPlace.isNullOrBlank() || !r.birthSource.isNullOrBlank()) {
                val ev = com.family.tree.model.GedcomEvent()
                ev.type = "BIRT"
                if (!r.birth.isNullOrBlank()) ev.date = r.birth
                if (!r.birthPlace.isNullOrBlank()) ev.place = r.birthPlace
                if (!r.birthSource.isNullOrBlank()) {
                    val sc = com.family.tree.model.SourceCitation()
                    val refId = resolveSourRef(r.birthSource, sourNumToId)
                    if (refId != null) sc.sourceId = refId else sc.text = r.birthSource
                    if (!r.birthPage.isNullOrBlank()) sc.page = r.birthPage
                    ev.sources.add(sc)
                }
                ind.events.add(ev)
            }
            if (!r.death.isNullOrBlank() || !r.deathPlace.isNullOrBlank() || !r.deathSource.isNullOrBlank()) {
                val ev = com.family.tree.model.GedcomEvent()
                ev.type = "DEAT"
                if (!r.death.isNullOrBlank()) ev.date = r.death
                if (!r.deathPlace.isNullOrBlank()) ev.place = r.deathPlace
                if (!r.deathSource.isNullOrBlank()) {
                    val sc = com.family.tree.model.SourceCitation()
                    val refId = resolveSourRef(r.deathSource, sourNumToId)
                    if (refId != null) sc.sourceId = refId else sc.text = r.deathSource
                    if (!r.deathPage.isNullOrBlank()) sc.page = r.deathPage
                    ev.sources.add(sc)
                }
                ind.events.add(ev)
            }
            // Attach optional notes parsed from NOTE/NOTES
            if (r.notes.isNotEmpty()) {
                for (nt in r.notes) {
                    if (!nt.isNullOrBlank()) {
                        val n = com.family.tree.model.Note()
                        n.text = nt.trim()
                        ind.notes.add(n)
                    }
                }
            }
            // Attach optional media parsed from OBJE/FORM/TITL/FILE
            if (r.media.isNotEmpty()) {
                for (mt in r.media) {
                    val file = mt.file?.trim()
                    val titl = mt.title?.trim()
                    if ((file.isNullOrBlank()) && (titl.isNullOrBlank())) continue
                    val ma = com.family.tree.model.MediaAttachment()
                    if (!file.isNullOrBlank()) {
                        ma.relativePath = file
                    }
                    val fileName = if (!titl.isNullOrBlank()) titl else baseName(file)
                    if (!fileName.isNullOrBlank()) ma.fileName = fileName
                    ind.media.add(ma)
                }
            }
            data.individuals.add(ind)
            idMap[key] = ind.id
        }

        val famIdMap: MutableMap<String, String> = HashMap() // F### -> Family.id
        for ((key, rf) in bundle.fams) {
            val fam = Family()
            if (rf.husb != null) fam.husbandId = idMap[rf.husb]
            if (rf.wife != null) fam.wifeId = idMap[rf.wife]
            for (cP in rf.children) {
                val cid = idMap[cP]
                if (cid != null) fam.childrenIds.add(cid)
            }
            // Marriage date/place as event would be added separately if needed; skip here.
            // Attach optional media parsed for family OBJE blocks
            if (!rf.media.isNullOrEmpty()) {
                for (mt in rf.media) {
                    val file = mt.file?.trim()
                    val titl = mt.title?.trim()
                    if ((file.isNullOrBlank()) && (titl.isNullOrBlank())) continue
                    val ma = com.family.tree.model.MediaAttachment()
                    if (!file.isNullOrBlank()) ma.relativePath = file
                    val fileName = if (!titl.isNullOrBlank()) titl else baseName(file)
                    if (!fileName.isNullOrBlank()) ma.fileName = fileName
                    fam.media.add(ma)
                }
            }
            data.families.add(fam)
            famIdMap[key] = fam.id

            // Relationships for renderer
            fam.husbandId?.let {
                val r = com.family.tree.model.Relationship()
                r.type = com.family.tree.model.Relationship.Type.SPOUSE_TO_FAMILY
                r.fromId = it
                r.toId = fam.id
                data.relationships.add(r)
            }
            fam.wifeId?.let {
                val r = com.family.tree.model.Relationship()
                r.type = com.family.tree.model.Relationship.Type.SPOUSE_TO_FAMILY
                r.fromId = it
                r.toId = fam.id
                data.relationships.add(r)
            }
            for (cid in fam.childrenIds) {
                val r = com.family.tree.model.Relationship()
                r.type = com.family.tree.model.Relationship.Type.FAMILY_TO_CHILD
                r.fromId = fam.id
                r.toId = cid
                data.relationships.add(r)
            }
        }

        // If layout was provided, set positions for imported individuals
        if (layout != null) {
            val posMap = layout.nodePositions
            var anyPos = false
            for ((secId, pr) in bundle.persons) {
                val pid = idMap[secId]
                if (pid != null && pr.x != null && pr.y != null) {
                    val np = ProjectLayout.NodePos()
                    np.x = pr.x!!
                    np.y = pr.y!!
                    posMap[pid] = np
                    anyPos = true
                }
            }
            // Mark that imported positions are centers only if we actually imported any positions
            if (anyPos) layout.positionsAreCenters = true
        }

        return data
    }

    private fun stripZeroBytes(input: ByteArray): ByteArray {
        val out = ByteArray(input.size)
        var p = 0
        for (b in input) if (b.toInt() != 0) out[p++] = b
        return Arrays.copyOf(out, p)
    }

    private fun splitSections(text: String): List<Section> {
        val starts: MutableList<IntArray> = ArrayList()
        val ids: MutableList<String> = ArrayList()
        val m: Matcher = SECTION_CANDIDATE.matcher(text)
        while (m.find()) {
            val id = m.group(1)
            val idx = m.start(1)
            if (isLikelySectionStart(text, idx, id)) {
                starts.add(intArrayOf(idx, id.length))
                ids.add(id)
            }
        }
        val res: MutableList<Section> = ArrayList()
        for (i in starts.indices) {
            val startIdx = starts[i][0] + starts[i][1] // body begins after id
            val endIdx = if (i + 1 < starts.size) starts[i + 1][0] else text.length
            res.add(Section(ids[i], text.substring(startIdx, maxOf(startIdx, endIdx))))
        }
        return res
    }

    private fun isLikelySectionStart(text: String, idx: Int, id: String): Boolean {
        // Avoid matching cross references like "HUSB P1" or "CHIL P3" by checking context and expected tags ahead.
        // Heuristics:
        // 1) If there is an ASCII letter immediately before id, treat as not a section start.
        if (idx > 0) {
            val prev = text[maxOf(0, idx - 1)]
            if (prev.isLetterOrDigit()) return false
        }
        // 2) Look ahead a small window for expected tags.
        val windowEnd = minOf(text.length, idx + 200)
        val lookahead = text.substring(idx, windowEnd).uppercase(Locale.ROOT)
        return if (id.startsWith("P")) {
            lookahead.contains("NAME") || lookahead.contains("SEX") || lookahead.contains("GIVN") || lookahead.contains("SURN") || lookahead.contains("_X") || lookahead.contains("_Y")
        } else { // F
            lookahead.contains("HUSB") || lookahead.contains("WIFE") || lookahead.contains("CHIL") || lookahead.contains("MARR")
        }
    }

    private fun parsePerson(s: Section): PersonRec {
        val r = PersonRec()
        r.id = s.id
        val body = sanitize(s.body)
        // For name extraction, pre-strip OBJE and NOTE(S) blocks to avoid contamination
        val bodyForName = NOTE_BLOCK_RE.matcher(OBJE_BLOCK_RE.matcher(body).replaceAll("")).replaceAll("")
        r.name = find(NAME_RE, bodyForName)
        // Fallback if regex failed: try alternative corrupted tag pattern or slice between tokens
        if (r.name.isNullOrBlank()) {
            val alt = find(NAME_ALT_RE, bodyForName)
            if (!alt.isNullOrBlank()) {
                r.name = alt
            } else {
                val sliced = extractAfterToken("NAME", bodyForName)
                if (!sliced.isNullOrBlank()) r.name = sliced else {
                    // Final fallback: scan for inline GEDCOM-like name before SEX, using the already cleaned body (no OBJE/NOTES)
                    var preSex = bodyForName
                    val sexAt = preSex.indexOf("SEX")
                    if (sexAt > 0) preSex = preSex.substring(0, sexAt)
                    val inline = find(INLINE_GEDCOM_NAME, preSex)
                    if (!inline.isNullOrBlank()) r.name = inline
                }
            }
        }
        // Some REL exports store names as separate GIVN/SURN tokens
        val givn = find(GIVN_RE, bodyForName)
        val surn = find(SURN_RE, bodyForName)
        if (givn != null) r.given = cleanToken(givn)
        if (surn != null) r.surname = cleanToken(surn)
        val sx = find(SEX_RE, body)
        r.sex = mapSex(sx)
        if (r.sex == Gender.UNKNOWN) {
            val sx2 = extractFirstSigCharAfter("SEX", body)
            if (sx2 != null) r.sex = mapSex(sx2)
        }
        // Fallbacks for localized Russian fields
        if (r.given.isNullOrBlank()) {
            val ruG = find(RUS_GIVN_RE, body)
            if (ruG != null) r.given = ruG.trim()
        }
        if (r.surname.isNullOrBlank()) {
            val ruS = find(RUS_SURN_RE, body)
            if (ruS != null) r.surname = ruS.trim()
        }
        if (r.sex == Gender.UNKNOWN) {
            val ruSexLetter = find(RUS_SEX_LETTER_RE, body)
            val ruLetterMapped = mapSex(ruSexLetter)
            if (ruLetterMapped != Gender.UNKNOWN) {
                r.sex = ruLetterMapped
            } else {
                val ruSexWord = find(RUS_SEX_WORD_RE, body)
                if (ruSexWord != null) {
                    val v = ruSexWord.lowercase(Locale.ROOT)
                    if (v.startsWith("муж")) r.sex = Gender.MALE else if (v.startsWith("жен")) r.sex = Gender.FEMALE
                }
            }
        }
        // Final cleanup of NAME to avoid trailing embedded tags or media fragments
        if (!r.name.isNullOrBlank()) {
            r.name = cleanNameValue(r.name!!)
        } else {
            // Ultra-robust fallback: extract after raw 'NAME' token until next known tag
            val nm2 = extractNameSimple(body)
            if (!nm2.isNullOrBlank()) r.name = nm2
        }
        r.birth = parsePersonDate(find(BIRT_RE, body))
        r.death = parsePersonDate(find(DEAT_RE, body))
        val birthPlac = find(BIRT_PLAC_RE, body)
        if (birthPlac != null) r.birthPlace = birthPlac.trim()
        val deathPlac = find(DEAT_PLAC_RE, body)
        if (deathPlac != null) r.deathPlace = deathPlac.trim()
        // Extract SOUR and PAGE inside BIRT/DEAT blocks if present
        // Find BIRT block boundaries
        val birtIdx = body.indexOf("BIRT")
        if (birtIdx >= 0) {
            var birtEnd = indexOfNextTagExcludingSourPage(body, birtIdx + 4)
            if (birtEnd < 0) birtEnd = body.length
            val birtBlock = body.substring(birtIdx, birtEnd)
            val bSour = extractAfterToken("SOUR", birtBlock)
            if (!bSour.isNullOrBlank()) r.birthSource = bSour
            val bPage = extractAfterToken("PAGE", birtBlock)
            if (!bPage.isNullOrBlank()) r.birthPage = bPage
        }
        val deatIdx = body.indexOf("DEAT")
        if (deatIdx >= 0) {
            var deatEnd = indexOfNextTagExcludingSourPage(body, deatIdx + 4)
            if (deatEnd < 0) deatEnd = body.length
            val deatBlock = body.substring(deatIdx, deatEnd)
            val dSour = extractAfterToken("SOUR", deatBlock)
            if (!dSour.isNullOrBlank()) r.deathSource = dSour
            val dPage = extractAfterToken("PAGE", deatBlock)
            if (!dPage.isNullOrBlank()) r.deathPage = dPage
        }
        val xs = find(POS_X_RE, body)
        val ys = find(POS_Y_RE, body)
        if (xs != null) try { r.x = xs.trim().replace(',', '.').toDouble() } catch (_: NumberFormatException) {}
        if (ys != null) try { r.y = ys.trim().replace(',', '.').toDouble() } catch (_: NumberFormatException) {}
        // Extract optional NOTE/NOTES blocks (until next tag like SOUR/SEX/...)
        val noteM = NOTE_BLOCK_RE.matcher(body)
        while (noteM.find()) {
            var note = noteM.group(1)
            if (note != null) {
                // Keep CR/LF to preserve multi-line notes; strip other control chars
                note = note.replace("[\\p{Cntrl}&&[^\\r\\n]]".toRegex(), "")
                note = note.trim()
                // Remove BOM and Unicode replacement characters that sometimes leak at the start
                note = note.replace("\uFEFF", "")
                note = note.replace("\uFFFD", "")
                // Remove repeated garbage prefixes like 'r', 'q' optionally followed by punctuation/spaces
                note = note.replaceFirst("^(?:[rqRQ]{1,3}[\\s:;,-]*)+".toRegex(), "")
                // Also handle solitary leading r/q without separator if followed by a letter or digit
                note = note.replaceFirst("^[rqRQ](?=\\p{L}|\\p{N})".toRegex(), "")
                // Heuristic: some REL exports leave a stray ASCII letter before a leading number (e.g., 'r13-')
                if (note.matches("^[A-Za-z](?=\\d).*".toRegex())) {
                    note = note.substring(1).trimStart()
                }
                if (note.isNotEmpty()) r.notes.add(note)
            }
        }
        // Extract optional multimedia OBJE blocks
        val objeM = OBJE_BLOCK_RE.matcher(body)
        while (objeM.find()) {
            val chunk = objeM.group(1) ?: continue
            var form = find(FORM_INNER_RE, chunk)
            var titl = find(TITL_INNER_RE, chunk)
            var file = find(FILE_INNER_RE, chunk)
            if (titl != null) {
                titl = titl.replace("[\\p{Cntrl}&&[^\\r\\n]]".toRegex(), "").trim()
                titl = titl.replace("\uFEFF", "").replace("\uFFFD", "")
            }
            if (file != null) {
                file = file.replace("[\\p{Cntrl}]".toRegex(), "").trim()
            }
            if (form != null) {
                form = form.replace("[\\p{Cntrl}]".toRegex(), "").trim()
            }
            if (!titl.isNullOrBlank() || !file.isNullOrBlank() || !form.isNullOrBlank()) {
                val mt = MediaTmp()
                mt.form = form?.takeIf { it.isNotBlank() }
                mt.title = titl?.takeIf { it.isNotBlank() }
                mt.file = file?.takeIf { it.isNotBlank() }
                r.media.add(mt)
            }
        }
        // Fallback: some REL exports omit OBJE and place FILE/TITL/FORM directly in the section
        if (r.media.isEmpty()) {
            val bodyNoObje = OBJE_BLOCK_RE.matcher(body).replaceAll(" ")
            // Find multiple FILE entries and pair with nearest preceding TITL within 200 chars
            val mf = FILE_INNER_RE.matcher(bodyNoObje)
            var created = 0
            while (mf.find()) {
                var file = mf.group(1)
                if (file != null) file = file.replace("[\\p{Cntrl}]".toRegex(), "").trim()
                var titl: String? = null
                var form: String? = null
                // search backwards up to 200 chars for TITL
                val start = maxOf(0, mf.start() - 200)
                val window = bodyNoObje.substring(start, mf.start())
                val titlWin = find(TITL_INNER_RE, window)
                if (titlWin != null) {
                    titl = titlWin.replace("[\\p{Cntrl}&&[^\\r\\n]]".toRegex(), "").trim()
                    titl = titl.replace("\uFEFF", "").replace("\uFFFD", "")
                }
                // search forwards up to 100 chars for FORM
                val end = minOf(bodyNoObje.length, mf.end() + 100)
                val windowF = bodyNoObje.substring(mf.end(), end)
                val formWin = find(FORM_INNER_RE, windowF)
                if (formWin != null) form = formWin.replace("[\\p{Cntrl}]".toRegex(), "").trim()
                if (!file.isNullOrBlank() || !titl.isNullOrBlank()) {
                    val mt = MediaTmp()
                    mt.file = file?.takeIf { it.isNotBlank() }
                    mt.title = titl?.takeIf { it.isNotBlank() }
                    mt.form = form?.takeIf { it.isNotBlank() }
                    r.media.add(mt)
                    created++
                }
            }
            // If there were TITL tokens but no FILEs, add title-only entries
            if (created == 0) {
                val mt = TITL_INNER_RE.matcher(bodyNoObje)
                while (mt.find()) {
                    var titl = mt.group(1)
                    if (titl != null) {
                        titl = titl.replace("[\\p{Cntrl}&&[^\\r\\n]]".toRegex(), "").trim()
                        titl = titl.replace("\uFEFF", "").replace("\uFFFD", "")
                        if (titl.isNotBlank()) {
                            val m = MediaTmp()
                            m.title = titl
                            r.media.add(m)
                        }
                    }
                }
            }
        }
        return r
    }

    private fun parseFamily(s: Section): FamRec {
        val f = FamRec()
        f.id = s.id
        val body = sanitize(s.body)
        f.husb = find(HUSB_RE, body)
        f.wife = find(WIFE_RE, body)
        val mc = CHIL_RE.matcher(body)
        while (mc.find()) {
            f.children.add(mc.group(1))
        }
        f.marrDate = parseDateLocal(find(MARR_DATE_RE, body))
        val plac = find(MARR_PLAC_RE, body)
        if (plac != null) f.marrPlace = plac.trim()
        // Extract optional multimedia OBJE blocks for family
        val objeM = OBJE_BLOCK_RE.matcher(body)
        while (objeM.find()) {
            val chunk = objeM.group(1) ?: continue
            var form = find(FORM_INNER_RE, chunk)
            var titl = find(TITL_INNER_RE, chunk)
            var file = find(FILE_INNER_RE, chunk)
            if (titl != null) {
                titl = titl.replace("[\\p{Cntrl}&&[^\\r\\n]]".toRegex(), "").trim()
                titl = titl.replace("\uFEFF", "").replace("\uFFFD", "")
            }
            if (file != null) {
                file = file.replace("[\\p{Cntrl}]".toRegex(), "").trim()
            }
            if (form != null) {
                form = form.replace("[\\p{Cntrl}]".toRegex(), "").trim()
            }
            if (!titl.isNullOrBlank() || !file.isNullOrBlank() || !form.isNullOrBlank()) {
                val mt = MediaTmp()
                mt.form = form?.takeIf { it.isNotBlank() }
                mt.title = titl?.takeIf { it.isNotBlank() }
                mt.file = file?.takeIf { it.isNotBlank() }
                f.media.add(mt)
            }
        }
        // Fallback: standalone FILE/TITL/FORM tokens outside OBJE
        if (f.media.isEmpty()) {
            val bodyNoObje = OBJE_BLOCK_RE.matcher(body).replaceAll(" ")
            val mf2 = FILE_INNER_RE.matcher(bodyNoObje)
            var created = 0
            while (mf2.find()) {
                var file = mf2.group(1)
                if (file != null) file = file.replace("[\\p{Cntrl}]".toRegex(), "").trim()
                var titl: String? = null
                var form: String? = null
                val start = maxOf(0, mf2.start() - 200)
                val window = bodyNoObje.substring(start, mf2.start())
                val titlWin = find(TITL_INNER_RE, window)
                if (titlWin != null) {
                    titl = titlWin.replace("[\\p{Cntrl}&&[^\\r\\n]]".toRegex(), "").trim()
                    titl = titl.replace("\uFEFF", "").replace("\uFFFD", "")
                }
                val end = minOf(bodyNoObje.length, mf2.end() + 100)
                val windowF = bodyNoObje.substring(mf2.end(), end)
                val formWin = find(FORM_INNER_RE, windowF)
                if (formWin != null) form = formWin.replace("[\\p{Cntrl}]".toRegex(), "").trim()
                if (!file.isNullOrBlank() || !titl.isNullOrBlank()) {
                    val mt = MediaTmp()
                    mt.file = file?.takeIf { it.isNotBlank() }
                    mt.title = titl?.takeIf { it.isNotBlank() }
                    mt.form = form?.takeIf { it.isNotBlank() }
                    f.media.add(mt)
                    created++
                }
            }
            if (created == 0) {
                val mt2 = TITL_INNER_RE.matcher(bodyNoObje)
                while (mt2.find()) {
                    var titl = mt2.group(1)
                    if (titl != null) {
                        titl = titl.replace("[\\p{Cntrl}&&[^\\r\\n]]".toRegex(), "").trim()
                        titl = titl.replace("\uFEFF", "").replace("\uFFFD", "")
                        if (titl.isNotBlank()) {
                            val m = MediaTmp()
                            m.title = titl
                            f.media.add(m)
                        }
                    }
                }
            }
        }
        return f
    }

    private fun find(p: Pattern, s: String): String? {
        val m = p.matcher(s)
        return if (m.find()) m.group(if (m.groupCount() >= 1) 1 else 0) else null
    }

    private fun parsePersonDate(s: String?): String? {
        if (s == null) return null
        var t = s.replace("[\\p{Cntrl}]".toRegex(), "").trim()
        if (t.isEmpty()) return null
        // Remove leading length markers (digits) from binary TLV format before date content
        t = t.replaceFirst("^[\\d\\s]+(?=[A-Z@(])".toRegex(), "")
        return t.trim()
    }

    private fun parseDateLocal(s: String?): LocalDate? {
        if (s == null) return null
        var st = s.trim()
        // Normalize dots and remove extraneous characters
        st = st.replace("[^0-9.]".toRegex(), "")
        if (st.isBlank()) return null
        return try {
            LocalDate.parse(st, DMY)
        } catch (e: DateTimeParseException) {
            try {
                val alt = DateTimeFormatter.ofPattern("d.M.uu")
                LocalDate.parse(st, alt)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun splitName(full: String?): Array<String> {
        if (full.isNullOrBlank()) return arrayOf("?", "")
        var src = full.trim()
        // Remove control chars introduced by TLV markers
        src = src.replace("[\\p{Cntrl}]".toRegex(), "")
        // Defensive: cut off at any embedded tag/marker if the raw string contains them
        src = cleanNameValue(src) ?: src
        // Drop any leading non-letter junk (length counters, etc.), but keep '/' and '^' if present later
        src = src.replaceFirst("^[^\\p{L}/^]+".toRegex(), "")
        // Handle caret-separated format typical for REL: Surname^First^Middle^
        if (src.contains("^")) {
            val parts = src.split("^").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                var surname = cleanToken(parts[0]) ?: ""
                var given = cleanToken(parts[1]) ?: "?"
                // Strip commentary/quotes
                given = stripParensQuotes(given) ?: given
                surname = stripParensQuotes(surname) ?: surname
                return arrayOf(given, surname)
            } else if (parts.size == 1) {
                // Only one token: assume it's a surname
                val lone = stripParensQuotes(cleanToken(parts[0]) ?: "") ?: ""
                return arrayOf("?", lone)
            }
        }
        var given = src
        var surname = ""
        val a = src.indexOf('/')
        val b = if (a >= 0) src.indexOf('/', a + 1) else -1
        var usedSlash = false
        if (a > 0 && b > a) {
            val whitespaceBefore = src[a - 1].isWhitespace()
            val between = src.substring(a + 1, b)
            val urlLike = between.contains(":") || src.indexOf("//", a) == a + 1
            if (whitespaceBefore && !urlLike) {
                surname = cleanToken(between.trim()) ?: ""
                given = cleanToken((src.substring(0, a) + src.substring(b + 1)).trim()) ?: given
                usedSlash = true
            }
        }
        if (!usedSlash) {
            // fallback: split last token as surname if looks like two or more parts
            val parts = src.split("\n", "\t", " ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                given = cleanToken(parts.dropLast(1).joinToString(" ")) ?: given
                surname = cleanToken(parts.last()) ?: ""
            } else {
                given = cleanToken(given) ?: given
                surname = cleanToken(surname) ?: surname
            }
        }
        // Strip commentary/quotes from both
        given = stripParensQuotes(given) ?: given
        surname = stripParensQuotes(surname) ?: surname
        return arrayOf(given, surname)
    }

    private fun cleanToken(s: String?): String? {
        if (s == null) return null
        var t = s.trim()
        // Remove leading non-letter characters (digits, punctuation). Keep letters in any script.
        t = t.replaceFirst("^[^\\p{L}]+".toRegex(), "")
        return t
    }

    // Remove parenthetical/bracketed commentary and surrounding quotes; normalize spaces
    private fun stripParensQuotes(s: String?): String? {
        if (s == null) return null
        var t = s
        // Remove (...) segments
        t = t.replace("\\s*\\([\\s\\S]*?\\)\\s*".toRegex(), " ")
        // Remove [...] segments
        t = t.replace("\\s*\\[[\\s\\S]*?\\]\\s*".toRegex(), " ")
        // Remove {...} segments
        t = t.replace("\\s*\\{[\\s\\S]*?\\}\\s*".toRegex(), " ")
        // Trim surrounding quotes and special quote marks
        t = t.replace("^[\"'“”„«»]+|[\"'“”„«»]+$".toRegex(), "")
        // Collapse spaces
        t = t.replace("\\s{2,}".toRegex(), " ").trim()
        return t
    }

    // Clean up NAME value by stripping control chars and cutting off at the first embedded tag/record marker
    private fun cleanNameValue(s: String?): String? {
        if (s == null) return null
        var t = s.replace("[\\p{Cntrl}]".toRegex(), " ")
        // Remove stray BOM and replacement characters early
        t = t.replace("\uFEFF", "").replace("\uFFFD", "")
        // Trim and drop leading punctuation/junk (e.g., $, #, :, etc.)
        t = t.replaceFirst("^[\\p{Punct}]+".toRegex(), "").trim()
        if (t.isEmpty()) return t
        val up = t.uppercase(Locale.ROOT)
        val toks = arrayOf("SEX", "BIRT", "DEAT", "FAMC", "FAMS", "NOTE", "NOTES", "OBJE", "TITL", "FILE", "FORM", "SOUR", "SUBM", "_X", "_Y")
        var cut = t.length
        for (tok in toks) {
            val idx = up.indexOf(tok)
            if (idx >= 0 && idx < cut) cut = idx
        }
        // Also cut at record markers like P123/F45 if accidentally stuck to the name
        val mp = Regex("(?i)(?:^|[^A-Z])P\\d{1,5}").find(up)
        if (mp != null) cut = minOf(cut, mp.range.first)
        val mf = Regex("(?i)(?:^|[^A-Z])F\\d{1,5}").find(up)
        if (mf != null) cut = minOf(cut, mf.range.first)
        t = t.substring(0, cut).trim()
        // Collapse internal excessive spaces
        t = t.replace("\\s{2,}".toRegex(), " ")
        return t
    }

    private fun mapSex(sx: String?): Gender {
        if (sx == null) return Gender.UNKNOWN
        val t = sx.trim()
        if (t.isEmpty()) return Gender.UNKNOWN
        val c = t[0].uppercaseChar()
        // Latin
        if (c == 'M') return Gender.MALE
        if (c == 'F') return Gender.FEMALE
        // Digits from some exports: 1 = male, 2 = female
        if (c == '1') return Gender.MALE
        if (c == '2') return Gender.FEMALE
        // Cyrillic
        if (c == 'М') return Gender.MALE // Cyrillic Em
        if (c == 'Ж') return Gender.FEMALE // Cyrillic Zhe
        return Gender.UNKNOWN
    }

    private fun sanitize(s: String?): String {
        if (s == null) return ""
        // Remove all control characters except CR and LF to preserve potential line structure
        return s.replace("[\\p{Cntrl}&&[^\\r\\n]]".toRegex(), "")
    }

    private fun indexOfNextTag(s: String?, from: Int): Int {
        if (s == null) return -1
        var min = -1
        val tags = arrayOf("SEX", "BIRT", "DEAT", "FAMC", "FAMS", "NOTE", "NOTES", "SOUR", "TITL", "OBJE", "P", "F")
        for (t in tags) {
            val i = s.indexOf(t, from)
            if (i >= 0 && (min < 0 || i < min)) min = i
        }
        return min
    }

    // Find index of the next top-level tag, but deliberately keep SOUR and PAGE inside the current block
    // so that BIRT/DEAT sub-block parsing can see them.
    private fun indexOfNextTagExcludingSourPage(s: String?, from: Int): Int {
        if (s == null) return -1
        var min = -1
        val tags = arrayOf("SEX", "BIRT", "DEAT", "FAMC", "FAMS", "NOTE", "NOTES", "TITL", "OBJE", "P", "F", "_X", "_Y")
        for (t in tags) {
            val i = s.indexOf(t, from)
            if (i >= 0 && (min < 0 || i < min)) min = i
        }
        return min
    }

    private fun extractAfterToken(token: String?, body: String?): String? {
        if (body == null || token == null) return null
        // Match full token with non-letter boundaries to avoid matching SURNAME/FILENAME, case-insensitive
        val regex = "(?i)(?:^|[^\\p{L}])" + Pattern.quote(token) + "(?![\\p{L}])"
        val m = Pattern.compile(regex).matcher(body)
        if (!m.find()) return null
        var start = m.end()
        // skip whitespace and punctuation/control chars
        while (start < body.length) {
            val ch = body[start]
            if (ch.isLetter() || ch == '/' || ch == '^' || (ch.code in 0x0400..0x052F)) {
                break
            }
            start++
        }
        var end = indexOfNextTag(body, start)
        if (end < 0) end = body.length
        var raw = body.substring(start, end)
        // Strip BOM and Unicode replacement characters that sometimes appear between tags/values
        raw = raw.replace("\uFEFF", "").replace("\uFFFD", "")
        raw = raw.replace("[\\p{Cntrl}]".toRegex(), "").trim()
        return raw
    }

    private fun extractFirstSigCharAfter(token: String?, body: String?): String? {
        if (body == null || token == null) return null
        val i = body.indexOf(token)
        if (i < 0) return null
        var p = i + token.length
        while (p < body.length) {
            val ch = body[p]
            if (ch.isLetterOrDigit() || (ch.code in 0x0400..0x052F)) {
                return ch.toString()
            }
            p++
        }
        return null
    }

    private fun hasNonAscii(bytes: ByteArray): Boolean {
        for (b in bytes) if ((b.toInt() and 0x80) != 0) return true
        return false
    }

    private fun resolveSourRef(`val`: String?, sourNumToId: Map<Int, String>?): String? {
        if (`val` == null || sourNumToId.isNullOrEmpty()) return null
        var s = `val`.trim()
        if (s.isEmpty()) return null
        // Remove possible leading token remnants and symbols
        s = s.replaceFirst("(?i)^SOUR".toRegex(), "")
        s = s.replaceFirst("^[#№Nn\\s]*".toRegex(), "")
        // Keep only leading digits
        val m = Pattern.compile("^(\\d+)").matcher(s)
        if (!m.find()) return null
        return try {
            val n = Integer.parseInt(m.group(1))
            if (n in 1..6) {
                sourNumToId[n]
            } else null
        } catch (_: NumberFormatException) { null }
    }

    // Extract a tag value from a global SOURn block (e.g., ABBR, TITL) up to the next known tag
    private fun extractSourceTag(block: String?, tag: String?): String? {
        if (block.isNullOrBlank() || tag.isNullOrBlank()) return null
        // Find token with non-letter boundaries (case-insensitive)
        val regex = "(?i)(?:^|[^\\p{L}])" + Pattern.quote(tag) + "(?![\\p{L}])"
        val m = Pattern.compile(regex).matcher(block)
        if (!m.find()) return null
        var start = m.end()
        // Skip separators
        while (start < block.length) {
            val ch = block[start]
            if (ch.isLetterOrDigit() || ch == '/' || ch == '"' || ch == '\'' || (ch.code in 0x0400..0x052F)) break
            start++
        }
        // Determine end at next known source-related tag
        val up = block.uppercase(Locale.ROOT)
        var end = block.length
        val toks = arrayOf("ABBR","TITL","TEXT","PUBL","AGNC","REPO","CALN","AUTH","NOTE","OBJE","DATA","REFN","SOUR","P","F","SUBM","SUBM1")
        for (t in toks) {
            val idx = up.indexOf(t, start + 1)
            if (idx >= 0 && idx < end) end = idx
        }
        if (end < start) return null
        var raw = block.substring(start, end)
        raw = raw.replace("\uFEFF", "").replace("\uFFFD", "")
        raw = raw.replace("[\\p{Cntrl}]".toRegex(), " ").trim()
        // Collapse excessive spaces
        raw = raw.replace("\\s{2,}".toRegex(), " ")
        return if (raw.isEmpty()) null else raw
    }

    private fun baseName(path: String?): String? {
        if (path.isNullOrBlank()) return null
        var p = path.trim()
        // Normalize backslashes
        p = p.replace('\\', '/')
        val q = p.lastIndexOf('/')
        var name = if (q >= 0) p.substring(q + 1) else p
        // If URL with query, strip it
        val qi = name.indexOf('?')
        if (qi >= 0) name = name.substring(0, qi)
        // If still blank, return original path
        if (name.isBlank()) return p
        return name
    }

    /**
     * Some .rel exports include large submitter/author blocks wrapped by SUBM … SUBM1.
     * These blocks can inject noise into NAME and other fields. Strip them early.
     */
    private fun stripSubmitterBlocks(s: String): String {
        if (s.isEmpty()) return s
        // Keep only the content between the very first SUBM and the very last SUBM1 (case-insensitive).
        val up = s.uppercase(Locale.ROOT)
        val first = up.indexOf("SUBM")
        val last = up.lastIndexOf("SUBM1")
        if (first >= 0 && last > first) {
            var start = first + 4 // after 'SUBM'
            // Trim leading whitespace/newlines after SUBM and before SUBM1
            while (start < s.length && s[start].isWhitespace()) start++
            var end = last
            while (end > start && s[end - 1].isWhitespace()) end--
            return s.substring(start, end)
        }
        return s
    }

    private fun parseBundle(text: String): ParseBundle {
        val sections = splitSections(text)
        val persons: MutableMap<String, PersonRec> = LinkedHashMap()
        val fams: MutableMap<String, FamRec> = LinkedHashMap()
        for (s in sections) {
            if (s.id.startsWith("P")) persons[s.id] = parsePerson(s)
        }
        for (s in sections) {
            if (s.id.startsWith("F")) fams[s.id] = parseFamily(s)
        }
        // Parse global/common SOUR1..SOUR6 blocks from the entire text
        val commonSources: MutableMap<Int, String> = LinkedHashMap()
        val SOURN_BLOCK = Pattern.compile(
            "(?i)SOUR([1-6])\\s*([\\s\\S]+?)(?=(?:SOUR[1-6]|P\\d{1,5}|F\\d{1,5}|SUBM1|$))",
            Pattern.DOTALL
        )
        val mm = SOURN_BLOCK.matcher(text)
        while (mm.find()) {
            val numS = mm.group(1)
            val `val` = mm.group(2)
            if (`val` != null) {
                val cleaned = `val`
                    .replace("\uFEFF", "")
                    .replace("\uFFFD", "")
                    .replace("[\\p{Cntrl}]".toRegex(), " ")
                    .trim()
                val n = try { Integer.parseInt(numS) } catch (_: NumberFormatException) { continue }
                if (cleaned.isNotEmpty() && !commonSources.containsKey(n)) {
                    commonSources[n] = cleaned
                }
            }
        }
        return ParseBundle(persons, fams, commonSources)
    }

    // Ultra-robust: directly slice after raw NAME until next known tag (case-insensitive)
    private fun extractNameSimple(body: String?): String? {
        if (body.isNullOrEmpty()) return null
        val up = body.uppercase(Locale.ROOT)
        val i = up.indexOf("NAME")
        if (i < 0) return null
        var start = i + 4
        // skip any separators/control
        while (start < body.length) {
            val ch = body[start]
            if (!ch.isWhitespace() && ch != ':' && ch != '=' && ch != '"' && ch != '\'' && ch != '\\' && ch != '$' && ch != ';' && ch != ',' && ch.code != 0x00) break
            start++
        }
        // find next tag boundary
        val toks = arrayOf("SEX", "BIRT", "DEAT", "FAMC", "FAMS", "NOTE", "NOTES", "OBJE", "TITL", "FILE", "FORM", "SOUR", "SUBM", "_X", "_Y", "P", "F")
        var end = body.length
        for (t in toks) {
            val idx = up.indexOf(t, start)
            if (idx >= 0 && idx < end) end = idx
        }
        var raw = body.substring(start, maxOf(start, end))
        raw = raw.replace("[\\p{Cntrl}]".toRegex(), " ").trim()
        return raw
    }

    // --- Data structures ---
    private class Section(val id: String, val body: String)

    private class PersonRec {
        var id: String? = null
        var name: String? = null
        var given: String? = null
        var surname: String? = null
        var sex: Gender = Gender.UNKNOWN
        var birth: String? = null
        var birthPlace: String? = null
        var birthSource: String? = null
        var birthPage: String? = null
        var death: String? = null
        var deathPlace: String? = null
        var deathSource: String? = null
        var deathPage: String? = null
        var x: Double? = null
        var y: Double? = null
        val notes: MutableList<String> = ArrayList()
        val media: MutableList<MediaTmp> = ArrayList()
    }

    private class MediaTmp {
        var form: String? = null
        var title: String? = null
        var file: String? = null
    }

    private class FamRec {
            var id: String? = null
            var husb: String? = null
            var wife: String? = null
            val children: MutableList<String> = ArrayList()
            var marrDate: LocalDate? = null
            var marrPlace: String? = null
            val media: MutableList<MediaTmp> = ArrayList()
        }

        private class ParseBundle(
            val persons: MutableMap<String, PersonRec>,
            val fams: MutableMap<String, FamRec>,
            val commonSources: Map<Int, String>
        )

}
