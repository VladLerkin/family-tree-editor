package com.pedigree.rel;

import com.pedigree.model.Event;
import com.pedigree.model.Family;
import com.pedigree.model.Gender;
import com.pedigree.model.Individual;
import com.pedigree.storage.ProjectRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class RelImporter {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("d.M.yyyy");

    public ProjectRepository.ProjectData importFromFile(Path path) throws IOException {
        return importFromFileWithLayout(path, null);
    }

    public ProjectRepository.ProjectData importFromFileWithLayout(Path path, com.pedigree.model.ProjectLayout layout) throws IOException {
        Objects.requireNonNull(path, "path");
        byte[] raw = Files.readAllBytes(path);
        // Remove all NUL bytes (0x00) – they are abundant in .rel TLV and break textual parsing
        byte[] cleaned = stripZeroBytes(raw);

        // First attempt: UTF-8
        String textUtf8 = new String(cleaned, StandardCharsets.UTF_8);
        textUtf8 = stripSubmitterBlocks(textUtf8);
        ParseBundle bundle = parseBundle(textUtf8);

        boolean poorNames = bundle.persons.values().stream().allMatch(r ->
                (r.given == null || r.given.isBlank()) &&
                (r.surname == null || r.surname.isBlank()) &&
                (r.name == null || r.name.isBlank())
        );
        // If all names look missing and there are high-bytes, try Windows-1251 decoding fallback
        if (poorNames && hasNonAscii(cleaned)) {
            try {
                String textCp1251 = new String(cleaned, java.nio.charset.Charset.forName("windows-1251"));
                textCp1251 = stripSubmitterBlocks(textCp1251);
                ParseBundle alt = parseBundle(textCp1251);
                boolean altBetter = alt.persons.values().stream().anyMatch(r ->
                        (r.given != null && !r.given.isBlank()) ||
                        (r.surname != null && !r.surname.isBlank()) ||
                        (r.name != null && !r.name.isBlank())
                );
                if (altBetter) bundle = alt;
            } catch (Exception ignore) {
                // keep UTF-8 result if fallback fails
            }
        }

        // Build model
        ProjectRepository.ProjectData data = new ProjectRepository.ProjectData();
        Map<String, String> idMap = new HashMap<>(); // P### -> Individual.id
        // Collect referenced person section ids from families to avoid dropping real, unnamed members
        java.util.Set<String> referenced = new java.util.HashSet<>();
        for (FamRec rf0 : bundle.fams.values()) {
            if (rf0.husb != null) referenced.add(rf0.husb);
            if (rf0.wife != null) referenced.add(rf0.wife);
            for (String cx0 : rf0.children) if (cx0 != null) referenced.add(cx0);
        }
        for (Map.Entry<String, PersonRec> e : bundle.persons.entrySet()) {
            PersonRec r = e.getValue();
            String first;
            String last;
            // Prefer NAME field if it looks well-structured (e.g., contains /Surname/ or non-ASCII letters like Cyrillic)
            boolean nameLooksStructured = (r.name != null && !r.name.isBlank()) && (
                    r.name.indexOf('/') >= 0 ||
                    r.name.chars().anyMatch(ch -> (ch & 0x80) != 0)
            );
            if (nameLooksStructured) {
                String[] nm = splitName(r.name);
                first = nm[0];
                last = nm[1];
            } else if ((r.given != null && !r.given.isBlank()) || (r.surname != null && !r.surname.isBlank())) {
                first = (r.given != null && !r.given.isBlank()) ? cleanToken(r.given) : "?";
                last = (r.surname != null) ? cleanToken(r.surname) : "";
            } else {
                String[] nm = splitName(r.name);
                first = nm[0];
                last = nm[1];
            }
            // Final defensive cleanup: ensure names don’t contain glued tags like OBJE/TITL/FILE/NOTE
            first = cleanNameValue(first);
            last = cleanNameValue(last);
            // Remove parenthetical/bracketed commentary and surrounding quotes from names
            first = stripParensQuotes(first);
            last = stripParensQuotes(last);
            if (first == null || first.isBlank()) first = "?";
            if (last == null) last = "";
            // Skip obviously empty/garbage persons if they are not referenced by any family.
            // Be conservative: if the source NAME field exists (even if parsing to first/last failed), keep the person.
            boolean nameBlank = (first == null || first.isBlank() || first.equals("?")) && (last == null || last.isBlank());
            boolean sourceNameMissing = (r.name == null || r.name.isBlank());
            boolean looksEmpty = nameBlank && sourceNameMissing && r.birth == null && r.death == null && (r.sex == null || r.sex == Gender.UNKNOWN);
            if (looksEmpty && !referenced.contains(e.getKey())) {
                continue;
            }
            Individual ind = new Individual(first, last, r.sex);
            if (r.birth != null) ind.setBirthDate(r.birth);
            if (r.birthPlace != null) ind.setBirthPlace(r.birthPlace);
            if (r.death != null) ind.setDeathDate(r.death);
            if (r.deathPlace != null) ind.setDeathPlace(r.deathPlace);
            // Attach optional notes parsed from NOTE/NOTES
            if (r.notes != null && !r.notes.isEmpty()) {
                for (String nt : r.notes) {
                    if (nt != null && !nt.isBlank()) {
                        com.pedigree.model.Note n = new com.pedigree.model.Note();
                        n.setText(nt.trim());
                        ind.getNotes().add(n);
                    }
                }
            }
            // Attach optional media parsed from OBJE/FORM/TITL/FILE
            if (r.media != null && !r.media.isEmpty()) {
                for (MediaTmp mt : r.media) {
                    String file = mt.file != null ? mt.file.strip() : null;
                    String titl = mt.title != null ? mt.title.strip() : null;
                    if ((file == null || file.isBlank()) && (titl == null || titl.isBlank())) continue;
                    com.pedigree.model.MediaAttachment ma = new com.pedigree.model.MediaAttachment();
                    if (file != null && !file.isBlank()) {
                        ma.setRelativePath(file);
                    }
                    String fileName = (titl != null && !titl.isBlank()) ? titl : baseName(file);
                    if (fileName != null && !fileName.isBlank()) ma.setFileName(fileName);
                    ind.getMedia().add(ma);
                }
            }
            data.individuals.add(ind);
            idMap.put(e.getKey(), ind.getId());
        }

        Map<String, String> famIdMap = new HashMap<>(); // F### -> Family.id
        for (Map.Entry<String, FamRec> e : bundle.fams.entrySet()) {
            FamRec rf = e.getValue();
            Family fam = new Family();
            if (rf.husb != null) fam.setHusbandId(idMap.get(rf.husb));
            if (rf.wife != null) fam.setWifeId(idMap.get(rf.wife));
            for (String cP : rf.children) {
                String cid = idMap.get(cP);
                if (cid != null) fam.getChildrenIds().add(cid);
            }
            if (rf.marrDate != null || (rf.marrPlace != null && !rf.marrPlace.isBlank())) {
                Event ev = new Event();
                ev.setType("MARRIAGE");
                if (rf.marrDate != null) ev.setDate(rf.marrDate);
                if (rf.marrPlace != null) ev.setPlace(rf.marrPlace);
                fam.setMarriage(ev);
            }
            // Attach optional media parsed for family OBJE blocks
            if (rf.media != null && !rf.media.isEmpty()) {
                for (MediaTmp mt : rf.media) {
                    String file = mt.file != null ? mt.file.strip() : null;
                    String titl = mt.title != null ? mt.title.strip() : null;
                    if ((file == null || file.isBlank()) && (titl == null || titl.isBlank())) continue;
                    com.pedigree.model.MediaAttachment ma = new com.pedigree.model.MediaAttachment();
                    if (file != null && !file.isBlank()) {
                        ma.setRelativePath(file);
                    }
                    String fileName = (titl != null && !titl.isBlank()) ? titl : baseName(file);
                    if (fileName != null && !fileName.isBlank()) ma.setFileName(fileName);
                    fam.getMedia().add(ma);
                }
            }
            data.families.add(fam);
            famIdMap.put(e.getKey(), fam.getId());

            // Relationships for renderer
            if (fam.getHusbandId() != null) {
                com.pedigree.model.Relationship r = new com.pedigree.model.Relationship();
                r.setType(com.pedigree.model.Relationship.Type.SPOUSE_TO_FAMILY);
                r.setFromId(fam.getHusbandId());
                r.setToId(fam.getId());
                data.relationships.add(r);
            }
            if (fam.getWifeId() != null) {
                com.pedigree.model.Relationship r = new com.pedigree.model.Relationship();
                r.setType(com.pedigree.model.Relationship.Type.SPOUSE_TO_FAMILY);
                r.setFromId(fam.getWifeId());
                r.setToId(fam.getId());
                data.relationships.add(r);
            }
            for (String cid : fam.getChildrenIds()) {
                com.pedigree.model.Relationship r = new com.pedigree.model.Relationship();
                r.setType(com.pedigree.model.Relationship.Type.FAMILY_TO_CHILD);
                r.setFromId(fam.getId());
                r.setToId(cid);
                data.relationships.add(r);
            }
        }

        // If layout was provided, set positions for imported individuals
        if (layout != null) {
            var posMap = layout.getNodePositions();
            boolean anyPos = false;
            for (Map.Entry<String, PersonRec> e : bundle.persons.entrySet()) {
                String pid = idMap.get(e.getKey());
                PersonRec pr = e.getValue();
                if (pid != null && pr.x != null && pr.y != null) {
                    var np = new com.pedigree.model.ProjectLayout.NodePos();
                    np.x = pr.x;
                    np.y = pr.y;
                    posMap.put(pid, np);
                    anyPos = true;
                }
            }
            // Mark that imported positions are centers only if we actually imported any positions
            if (anyPos) layout.setPositionsAreCenters(true);
        }

        return data;
    }

    private static byte[] stripZeroBytes(byte[] in) {
        byte[] out = new byte[in.length];
        int p = 0;
        for (byte b : in) if (b != 0) out[p++] = b;
        return Arrays.copyOf(out, p);
    }

    private static final Pattern SECTION_CANDIDATE = Pattern.compile("(P\\d{1,5}|F\\d{1,5})");

    private static List<Section> splitSections(String text) {
        List<int[]> starts = new ArrayList<>(); // [index, idLength]
        List<String> ids = new ArrayList<>();
        Matcher m = SECTION_CANDIDATE.matcher(text);
        while (m.find()) {
            String id = m.group(1);
            int idx = m.start(1);
            if (isLikelySectionStart(text, idx, id)) {
                starts.add(new int[]{idx, id.length()});
                ids.add(id);
            }
        }
        List<Section> res = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int startIdx = starts.get(i)[0] + starts.get(i)[1]; // body begins after id
            int endIdx = (i + 1 < starts.size()) ? starts.get(i + 1)[0] : text.length();
            res.add(new Section(ids.get(i), text.substring(startIdx, Math.max(startIdx, endIdx))));
        }
        return res;
    }

    private static boolean isLikelySectionStart(String text, int idx, String id) {
        // Avoid matching cross references like "HUSB P1" or "CHIL P3" by checking context and expected tags ahead.
        // Heuristics:
        // 1) If there is an ASCII letter immediately before id, treat as not a section start.
        if (idx > 0) {
            char prev = text.charAt(Math.max(0, idx - 1));
            if (Character.isLetterOrDigit(prev)) return false;
        }
        // 2) Look ahead a small window for expected tags.
        int windowEnd = Math.min(text.length(), idx + 200);
        String lookahead = text.substring(idx, windowEnd).toUpperCase(java.util.Locale.ROOT);
        if (id.startsWith("P")) {
            return lookahead.contains("NAME") || lookahead.contains("SEX") || lookahead.contains("GIVN") || lookahead.contains("SURN") || lookahead.contains("_X") || lookahead.contains("_Y");
        } else { // F
            return lookahead.contains("HUSB") || lookahead.contains("WIFE") || lookahead.contains("CHIL") || lookahead.contains("MARR");
        }
    }

    // Accept M/F as well as digits (1=male, 2=female) and Cyrillic (М/Ж); optional parentheses/spaces
    private static final Pattern SEX_RE = Pattern.compile("SEX\\P{L}*([MFmf12МЖмж])");
    private static final Pattern NAME_RE = Pattern.compile("NAME\\s*([\\s\\S]+?)\\s*(?=(SEX|BIRT|DEAT|FAMC|FAMS|NOTE|TITL|OBJE|SUBM|SOUR|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL);
    // Some files show a corrupted first letter in NAME (e.g., '\u044EAME' instead of 'NAME').
    private static final Pattern NAME_ALT_RE = Pattern.compile("(?:NAME|.AME)\\s*([\\s\\S]+?)\\s*(?=(SEX|BIRT|DEAT|FAMC|FAMS|NOTE|TITL|OBJE|SUBM|SOUR|P\\d+|F\\d+)|$)", Pattern.DOTALL);
    private static final Pattern GIVN_RE = Pattern.compile("GIVN\\P{L}*([^\\r\\n]+)");
    private static final Pattern SURN_RE = Pattern.compile("SURN\\P{L}*([^\\r\\n]+)");
    // Russian localized tokens (Имя, Фамилия, Отчество, Пол)
    private static final int RU_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern RUS_GIVN_RE = Pattern.compile("ИМЯ\\P{L}*([^\\r\\n]+)", RU_FLAGS);
    private static final Pattern RUS_SURN_RE = Pattern.compile("ФАМИЛИ[ЯИ]\\P{L}*([^\\r\\n]+)", RU_FLAGS);
    private static final Pattern RUS_SEX_LETTER_RE = Pattern.compile("ПОЛ\\P{L}*([МЖ])", RU_FLAGS);
    private static final Pattern RUS_SEX_WORD_RE = Pattern.compile("(Муж(ской)?|Жен(ский)?)", RU_FLAGS);
    private static final Pattern BIRT_RE = Pattern.compile("BIRT.*?DATE\\s*([^\\x00]+?)(?=\\s*(?:PLAC|DEAT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL);
    private static final Pattern BIRT_PLAC_RE = Pattern.compile("BIRT.*?PLAC\\s*([^\\r\\n]+?)(?=(?:DEAT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL);
    private static final Pattern DEAT_RE = Pattern.compile("DEAT.*?DATE\\s*([^\\x00]+?)(?=\\s*(?:PLAC|BIRT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL);
    private static final Pattern DEAT_PLAC_RE = Pattern.compile("DEAT.*?PLAC\\s*([^\\r\\n]+?)(?=(?:BIRT|NOTE|FAMC|FAMS|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL);
    private static final int POS_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern POS_X_RE = Pattern.compile("_X\\P{Alnum}*([+-]?\\d+(?:[.,]\\d+)?)", POS_FLAGS);
    private static final Pattern POS_Y_RE = Pattern.compile("_Y\\P{Alnum}*([+-]?\\d+(?:[.,]\\d+)?)", POS_FLAGS);
    // Fallback: inline GEDCOM-like "Given /Surname/" anywhere before SEX
    // Inline GEDCOM-like name: require a space before first slash to avoid matching URLs; capture Given and /Surname/
    private static final Pattern INLINE_GEDCOM_NAME = Pattern.compile("[\\p{L} .]+?\\s+/[^/]+/", Pattern.UNICODE_CASE);
    private static final Pattern NOTE_BLOCK_RE = Pattern.compile("(?:NOTE|NOTES)\\s*([\\s\\S]+?)\\s*(?=(SOUR|SEX|BIRT|DEAT|FAMC|FAMS|NOTE|NOTES|TITL|SUBM|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    // Multimedia: OBJE blocks possibly containing FORM/TITL/FILE
    // Do NOT terminate at TITL — TITL/FILE/FORM belong inside OBJE
    private static final Pattern OBJE_BLOCK_RE = Pattern.compile("OBJE\\s*([\\s\\S]+?)\\s*(?=(OBJE|NOTE|NOTES|SOUR|SEX|BIRT|DEAT|FAMC|FAMS|SUBM|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern FORM_INNER_RE = Pattern.compile("FORM\\s*([^\\r\\n\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITL_INNER_RE = Pattern.compile("TITL\\s*([\\s\\S]+?)\\s*(?=(FORM|FILE|TITL|OBJE|NOTE|NOTES|SOUR|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_INNER_RE = Pattern.compile("FILE\\s*([\\s\\S]+?)\\s*(?=(FORM|FILE|TITL|OBJE|NOTE|NOTES|SOUR|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static PersonRec parsePerson(Section s) {
        PersonRec r = new PersonRec();
        r.id = s.id;
        String body = sanitize(s.body);
        // For name extraction, pre-strip OBJE and NOTE(S) blocks to avoid contamination
        String bodyForName = NOTE_BLOCK_RE.matcher(OBJE_BLOCK_RE.matcher(body).replaceAll("")).replaceAll("");
        r.name = find(NAME_RE, bodyForName);
        // Fallback if regex failed: try alternative corrupted tag pattern or slice between tokens
        if (r.name == null || r.name.isBlank()) {
            String alt = find(NAME_ALT_RE, bodyForName);
            if (alt != null && !alt.isBlank()) {
                r.name = alt;
            } else {
                String sliced = extractAfterToken("NAME", bodyForName);
                if (sliced != null && !sliced.isBlank()) r.name = sliced;
                else {
                    // Final fallback: scan for inline GEDCOM-like name before SEX, using the already cleaned body (no OBJE/NOTES)
                    String preSex = bodyForName;
                    int sexAt = preSex.indexOf("SEX");
                    if (sexAt > 0) preSex = preSex.substring(0, sexAt);
                    String inline = find(INLINE_GEDCOM_NAME, preSex);
                    if (inline != null && !inline.isBlank()) r.name = inline;
                }
            }
        }
        // Some REL exports store names as separate GIVN/SURN tokens
        String givn = find(GIVN_RE, bodyForName);
        String surn = find(SURN_RE, bodyForName);
        if (givn != null) r.given = cleanToken(givn);
        if (surn != null) r.surname = cleanToken(surn);
        String sx = find(SEX_RE, body);
        r.sex = mapSex(sx);
        if (r.sex == Gender.UNKNOWN) {
            String sx2 = extractFirstSigCharAfter("SEX", body);
            if (sx2 != null) r.sex = mapSex(sx2);
        }
        // Fallbacks for localized Russian fields
        if ((r.given == null || r.given.isBlank())) {
            String ruG = find(RUS_GIVN_RE, body);
            if (ruG != null) r.given = ruG.strip();
        }
        if ((r.surname == null || r.surname.isBlank())) {
            String ruS = find(RUS_SURN_RE, body);
            if (ruS != null) r.surname = ruS.strip();
        }
        if (r.sex == Gender.UNKNOWN) {
            String ruSexLetter = find(RUS_SEX_LETTER_RE, body);
            Gender ruLetterMapped = mapSex(ruSexLetter);
            if (ruLetterMapped != Gender.UNKNOWN) {
                r.sex = ruLetterMapped;
            } else {
                String ruSexWord = find(RUS_SEX_WORD_RE, body);
                if (ruSexWord != null) {
                    String v = ruSexWord.toLowerCase(Locale.ROOT);
                    if (v.startsWith("муж")) r.sex = Gender.MALE;
                    else if (v.startsWith("жен")) r.sex = Gender.FEMALE;
                }
            }
        }
        // Final cleanup of NAME to avoid trailing embedded tags or media fragments
        if (r.name != null && !r.name.isBlank()) {
            r.name = cleanNameValue(r.name);
        } else {
            // Ultra-robust fallback: extract after raw 'NAME' token until next known tag
            String nm2 = extractNameSimple(body);
            if (nm2 != null && !nm2.isBlank()) r.name = nm2;
        }
        r.birth = parsePersonDate(find(BIRT_RE, body));
        r.death = parsePersonDate(find(DEAT_RE, body));
        String birthPlac = find(BIRT_PLAC_RE, body);
        if (birthPlac != null) r.birthPlace = birthPlac.strip();
        String deathPlac = find(DEAT_PLAC_RE, body);
        if (deathPlac != null) r.deathPlace = deathPlac.strip();
        String xs = find(POS_X_RE, body);
        String ys = find(POS_Y_RE, body);
        if (xs != null) try { r.x = Double.parseDouble(xs.trim()); } catch (NumberFormatException ignored) {}
        if (ys != null) try { r.y = Double.parseDouble(ys.trim()); } catch (NumberFormatException ignored) {}
        // Extract optional NOTE/NOTES blocks (until next tag like SOUR/SEX/...)
        Matcher noteM = NOTE_BLOCK_RE.matcher(body);
        while (noteM.find()) {
            String note = noteM.group(1);
            if (note != null) {
                // Keep CR/LF to preserve multi-line notes; strip other control chars
                note = note.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "");
                note = note.strip();
                // Remove BOM and Unicode replacement characters that sometimes leak at the start
                note = note.replace("\uFEFF", "");
                note = note.replace("\uFFFD", "");
                // Remove repeated garbage prefixes like 'r', 'q' optionally followed by punctuation/spaces
                note = note.replaceFirst("^(?:[rqRQ]{1,3}[\\s:;,-]*)+", "");
                // Also handle solitary leading r/q without separator if followed by a letter or digit
                note = note.replaceFirst("^[rqRQ](?=\\p{L}|\\p{N})", "");
                // Heuristic: some REL exports leave a stray ASCII letter before a leading number (e.g., 'r13-')
                if (note.matches("^[A-Za-z](?=\\d).*") ) {
                    note = note.substring(1).stripLeading();
                }
                if (!note.isEmpty()) r.notes.add(note);
            }
        }
        // Extract optional multimedia OBJE blocks
        Matcher objeM = OBJE_BLOCK_RE.matcher(body);
        while (objeM.find()) {
            String chunk = objeM.group(1);
            if (chunk == null) continue;
            String form = find(FORM_INNER_RE, chunk);
            String titl = find(TITL_INNER_RE, chunk);
            String file = find(FILE_INNER_RE, chunk);
            if (titl != null) {
                titl = titl.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "").strip();
                titl = titl.replace("\uFEFF", "").replace("\uFFFD", "");
            }
            if (file != null) {
                file = file.replaceAll("[\\p{Cntrl}]", "").trim();
            }
            if (form != null) {
                form = form.replaceAll("[\\p{Cntrl}]", "").trim();
            }
            if ((titl != null && !titl.isBlank()) || (file != null && !file.isBlank()) || (form != null && !form.isBlank())) {
                MediaTmp mt = new MediaTmp();
                mt.form = (form != null && !form.isBlank()) ? form : null;
                mt.title = (titl != null && !titl.isBlank()) ? titl : null;
                mt.file = (file != null && !file.isBlank()) ? file : null;
                r.media.add(mt);
            }
        }
        // Fallback: some REL exports omit OBJE and place FILE/TITL/FORM directly in the section
        if (r.media.isEmpty()) {
            String bodyNoObje = OBJE_BLOCK_RE.matcher(body).replaceAll(" ");
            // Find multiple FILE entries and pair with nearest preceding TITL within 200 chars
            Matcher mf = FILE_INNER_RE.matcher(bodyNoObje);
            int created = 0;
            while (mf.find()) {
                String file = mf.group(1);
                if (file != null) file = file.replaceAll("[\\p{Cntrl}]", "").trim();
                String titl = null;
                String form = null;
                // search backwards up to 200 chars for TITL
                int start = Math.max(0, mf.start() - 200);
                String window = bodyNoObje.substring(start, mf.start());
                String titlWin = find(TITL_INNER_RE, window);
                if (titlWin != null) {
                    titl = titlWin.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "").strip();
                    titl = titl.replace("\uFEFF", "").replace("\uFFFD", "");
                }
                // search forwards up to 100 chars for FORM
                int end = Math.min(bodyNoObje.length(), mf.end() + 100);
                String windowF = bodyNoObje.substring(mf.end(), end);
                String formWin = find(FORM_INNER_RE, windowF);
                if (formWin != null) form = formWin.replaceAll("[\\p{Cntrl}]", "").trim();
                if ((file != null && !file.isBlank()) || (titl != null && !titl.isBlank())) {
                    MediaTmp mt = new MediaTmp();
                    mt.file = (file != null && !file.isBlank()) ? file : null;
                    mt.title = (titl != null && !titl.isBlank()) ? titl : null;
                    mt.form = (form != null && !form.isBlank()) ? form : null;
                    r.media.add(mt);
                    created++;
                }
            }
            // If there were TITL tokens but no FILEs, add title-only entries
            if (created == 0) {
                Matcher mt = TITL_INNER_RE.matcher(bodyNoObje);
                while (mt.find()) {
                    String titl = mt.group(1);
                    if (titl != null) {
                        titl = titl.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "").strip();
                        titl = titl.replace("\uFEFF", "").replace("\uFFFD", "");
                        if (!titl.isBlank()) {
                            MediaTmp m = new MediaTmp();
                            m.title = titl;
                            r.media.add(m);
                        }
                    }
                }
            }
        }
        return r;
    }

    private static final int REL_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern HUSB_RE = Pattern.compile("HUSB\\P{Alnum}*(P\\d+)", REL_FLAGS);
    private static final Pattern WIFE_RE = Pattern.compile("WIFE\\P{Alnum}*(P\\d+)", REL_FLAGS);
    private static final Pattern CHIL_RE = Pattern.compile("CHIL\\P{Alnum}*(P\\d+)", REL_FLAGS);
    private static final Pattern MARR_DATE_RE = Pattern.compile("MARR.*?DATE\\((.+?)\\)", Pattern.DOTALL);
    private static final Pattern MARR_PLAC_RE = Pattern.compile("MARR.*?PLAC\\s*([^PF\\r\\n]+)", Pattern.DOTALL);

    private static FamRec parseFamily(Section s) {
        FamRec f = new FamRec();
        f.id = s.id;
        String body = sanitize(s.body);
        f.husb = find(HUSB_RE, body);
        f.wife = find(WIFE_RE, body);
        Matcher mc = CHIL_RE.matcher(body);
        while (mc.find()) {
            f.children.add(mc.group(1));
        }
        f.marrDate = parseDateLocal(find(MARR_DATE_RE, body));
        String plac = find(MARR_PLAC_RE, body);
        if (plac != null) f.marrPlace = plac.strip();
        // Extract optional multimedia OBJE blocks for family
        Matcher objeM = OBJE_BLOCK_RE.matcher(body);
        while (objeM.find()) {
            String chunk = objeM.group(1);
            if (chunk == null) continue;
            String form = find(FORM_INNER_RE, chunk);
            String titl = find(TITL_INNER_RE, chunk);
            String file = find(FILE_INNER_RE, chunk);
            if (titl != null) {
                titl = titl.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "").strip();
                titl = titl.replace("\uFEFF", "").replace("\uFFFD", "");
            }
            if (file != null) {
                file = file.replaceAll("[\\p{Cntrl}]", "").trim();
            }
            if (form != null) {
                form = form.replaceAll("[\\p{Cntrl}]", "").trim();
            }
            if ((titl != null && !titl.isBlank()) || (file != null && !file.isBlank()) || (form != null && !form.isBlank())) {
                MediaTmp mt = new MediaTmp();
                mt.form = (form != null && !form.isBlank()) ? form : null;
                mt.title = (titl != null && !titl.isBlank()) ? titl : null;
                mt.file = (file != null && !file.isBlank()) ? file : null;
                f.media.add(mt);
            }
        }
        // Fallback: standalone FILE/TITL/FORM tokens outside OBJE
        if (f.media.isEmpty()) {
            String bodyNoObje = OBJE_BLOCK_RE.matcher(body).replaceAll(" ");
            Matcher mf2 = FILE_INNER_RE.matcher(bodyNoObje);
            int created = 0;
            while (mf2.find()) {
                String file = mf2.group(1);
                if (file != null) file = file.replaceAll("[\\p{Cntrl}]", "").trim();
                String titl = null;
                String form = null;
                int start = Math.max(0, mf2.start() - 200);
                String window = bodyNoObje.substring(start, mf2.start());
                String titlWin = find(TITL_INNER_RE, window);
                if (titlWin != null) {
                    titl = titlWin.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "").strip();
                    titl = titl.replace("\uFEFF", "").replace("\uFFFD", "");
                }
                int end = Math.min(bodyNoObje.length(), mf2.end() + 100);
                String windowF = bodyNoObje.substring(mf2.end(), end);
                String formWin = find(FORM_INNER_RE, windowF);
                if (formWin != null) form = formWin.replaceAll("[\\p{Cntrl}]", "").trim();
                if ((file != null && !file.isBlank()) || (titl != null && !titl.isBlank())) {
                    MediaTmp mt = new MediaTmp();
                    mt.file = (file != null && !file.isBlank()) ? file : null;
                    mt.title = (titl != null && !titl.isBlank()) ? titl : null;
                    mt.form = (form != null && !form.isBlank()) ? form : null;
                    f.media.add(mt);
                    created++;
                }
            }
            if (created == 0) {
                Matcher mt2 = TITL_INNER_RE.matcher(bodyNoObje);
                while (mt2.find()) {
                    String titl = mt2.group(1);
                    if (titl != null) {
                        titl = titl.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "").strip();
                        titl = titl.replace("\uFEFF", "").replace("\uFFFD", "");
                        if (!titl.isBlank()) {
                            MediaTmp m = new MediaTmp();
                            m.title = titl;
                            f.media.add(m);
                        }
                    }
                }
            }
        }
        return f;
    }

    private static String find(Pattern p, String s) {
        Matcher m = p.matcher(s);
        if (m.find()) {
            return m.group(m.groupCount() >= 1 ? 1 : 0);
        }
        return null;
    }

    private static String parsePersonDate(String s) {
        if (s == null) return null;
        String t = s.replaceAll("[\\p{Cntrl}]", "").trim();
        if (t.isEmpty()) return null;
        // Remove leading length markers (digits) from binary TLV format before date content
        t = t.replaceFirst("^[\\d\\s]+(?=[A-Z@(])", "");
        return t.trim();
    }

    private static LocalDate parseDateLocal(String s) {
        if (s == null) return null;
        s = s.trim();
        // Normalize dots and remove extraneous characters
        s = s.replaceAll("[^0-9.]", "");
        if (s.isBlank()) return null;
        try {
            return LocalDate.parse(s, DMY);
        } catch (DateTimeParseException e) {
            // Try day and month padded or year only
            try {
                DateTimeFormatter alt = DateTimeFormatter.ofPattern("d.M.uu");
                return LocalDate.parse(s, alt);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private static String[] splitName(String full) {
        if (full == null || full.isBlank()) return new String[]{"?", ""};
        String src = full.strip();
        // Remove control chars introduced by TLV markers
        src = src.replaceAll("[\\p{Cntrl}]", "");
        // Defensive: cut off at any embedded tag/marker if the raw string contains them
        src = cleanNameValue(src);
        // Drop any leading non-letter junk (length counters, etc.), but keep '/' and '^' if present later
        src = src.replaceFirst("^[^\\p{L}/^]+", "");
        // Handle caret-separated format typical for REL: Surname^First^Middle^
        if (src.contains("^")) {
            String[] parts = Arrays.stream(src.split("\\^"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            if (parts.length >= 2) {
                String surname = cleanToken(parts[0]);
                String given = cleanToken(parts[1]);
                // Strip commentary/quotes
                given = stripParensQuotes(given);
                surname = stripParensQuotes(surname);
                return new String[]{given, surname};
            } else if (parts.length == 1) {
                // Only one token: assume it's a surname
                String lone = stripParensQuotes(cleanToken(parts[0]));
                return new String[]{"?", lone};
            }
        }
        String given = src;
        String surname = "";
        int a = src.indexOf('/');
        int b = (a >= 0) ? src.indexOf('/', a + 1) : -1;
        boolean usedSlash = false;
        if (a > 0 && b > a) {
            boolean whitespaceBefore = Character.isWhitespace(src.charAt(a - 1));
            String between = src.substring(a + 1, b);
            boolean urlLike = between.contains(":") || src.indexOf("//", a) == a + 1;
            if (whitespaceBefore && !urlLike) {
                surname = cleanToken(between.strip());
                given = cleanToken((src.substring(0, a) + src.substring(b + 1)).strip());
                usedSlash = true;
            }
        }
        if (!usedSlash) {
            // fallback: split last token as surname if looks like two or more parts
            String[] parts = src.split("\\s+");
            if (parts.length >= 2) {
                given = cleanToken(String.join(" ", Arrays.copyOf(parts, parts.length - 1)));
                surname = cleanToken(parts[parts.length - 1]);
            } else {
                given = cleanToken(given);
                surname = cleanToken(surname);
            }
        }
        // Strip commentary/quotes from both
        given = stripParensQuotes(given);
        surname = stripParensQuotes(surname);
        return new String[]{given, surname};
    }

    private static String cleanToken(String s) {
        if (s == null) return null;
        String t = s.strip();
        // Remove leading non-letter characters (digits, punctuation). Keep letters in any script.
        t = t.replaceFirst("^[^\\p{L}]+", "");
        return t;
    }

    // Remove parenthetical/bracketed commentary and surrounding quotes; normalize spaces
    private static String stripParensQuotes(String s) {
        if (s == null) return null;
        String t = s;
        // Remove (...) segments
        t = t.replaceAll("\\s*\\([\\s\\S]*?\\)\\s*", " ");
        // Remove [...] segments
        t = t.replaceAll("\\s*\\[[\\s\\S]*?\\]\\s*", " ");
        // Remove {...} segments
        t = t.replaceAll("\\s*\\{[\\s\\S]*?\\}\\s*", " ");
        // Trim surrounding quotes and special quote marks
        t = t.replaceAll("^[\"'“”„«»]+|[\"'“”„«»]+$", "");
        // Collapse spaces
        t = t.replaceAll("\\s{2,}", " ").trim();
        return t;
    }

    // Clean up NAME value by stripping control chars and cutting off at the first embedded tag/record marker
    private static String cleanNameValue(String s) {
        if (s == null) return null;
        String t = s.replaceAll("[\\p{Cntrl}]", " ");
        // Remove stray BOM and replacement characters early
        t = t.replace("\uFEFF", "").replace("\uFFFD", "");
        // Trim and drop leading punctuation/junk (e.g., $, #, :, etc.)
        t = t.replaceFirst("^[\\p{Punct}]+", "").trim();
        if (t.isEmpty()) return t;
        String up = t.toUpperCase(java.util.Locale.ROOT);
        String[] toks = {"SEX", "BIRT", "DEAT", "FAMC", "FAMS", "NOTE", "NOTES", "OBJE", "TITL", "FILE", "FORM", "SOUR", "SUBM", "_X", "_Y"};
        int cut = t.length();
        for (String tok : toks) {
            int idx = up.indexOf(tok);
            if (idx >= 0 && idx < cut) cut = idx;
        }
        // Also cut at record markers like P123/F45 if accidentally stuck to the name
        java.util.regex.Matcher mp = java.util.regex.Pattern.compile("(?i)(?:^|[^A-Z])P\\d{1,5}").matcher(up);
        if (mp.find()) cut = Math.min(cut, mp.start());
        java.util.regex.Matcher mf = java.util.regex.Pattern.compile("(?i)(?:^|[^A-Z])F\\d{1,5}").matcher(up);
        if (mf.find()) cut = Math.min(cut, mf.start());
        t = t.substring(0, cut).trim();
        // Collapse internal excessive spaces
        t = t.replaceAll("\\s{2,}", " ");
        return t;
    }

    private static Gender mapSex(String sx) {
        if (sx == null) return Gender.UNKNOWN;
        String t = sx.trim();
        if (t.isEmpty()) return Gender.UNKNOWN;
        char c = Character.toUpperCase(t.charAt(0));
        // Latin
        if (c == 'M') return Gender.MALE;
        if (c == 'F') return Gender.FEMALE;
        // Digits from some exports: 1 = male, 2 = female
        if (c == '1') return Gender.MALE;
        if (c == '2') return Gender.FEMALE;
        // Cyrillic
        if (c == 'М') return Gender.MALE; // Cyrillic Em
        if (c == 'Ж') return Gender.FEMALE; // Cyrillic Zhe
        return Gender.UNKNOWN;
    }

    private static class Section {
        final String id; // P### or F###
        final String body;
        Section(String id, String body) { this.id = id; this.body = body; }
    }

    private static class PersonRec {
        String id;
        String name;
        String given;
        String surname;
        Gender sex = Gender.UNKNOWN;
        String birth;
        String birthPlace;
        String death;
        String deathPlace;
        Double x; // optional parsed position
        Double y;
        List<String> notes = new ArrayList<>();
        List<MediaTmp> media = new ArrayList<>();
    }

    private static class MediaTmp {
        String form;
        String title;
        String file;
    }

    private static class FamRec {
        String id;
        String husb;
        String wife;
        List<String> children = new ArrayList<>();
        LocalDate marrDate;
        String marrPlace;
        List<MediaTmp> media = new ArrayList<>();
    }

    private static String sanitize(String s) {
        if (s == null) return null;
        // Remove all control characters except CR and LF to preserve potential line structure
        return s.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "");
    }

    private static int indexOfNextTag(String s, int from) {
        if (s == null) return -1;
        int min = -1;
        String[] tags = {"SEX", "BIRT", "DEAT", "FAMC", "FAMS", "NOTE", "NOTES", "SOUR", "TITL", "OBJE", "P", "F"};
        for (String t : tags) {
            int i = s.indexOf(t, from);
            if (i >= 0 && (min < 0 || i < min)) min = i;
        }
        return min;
    }

    private static String extractAfterToken(String token, String body) {
        if (body == null || token == null) return null;
        // Match full token with non-letter boundaries to avoid matching SURNAME/FILENAME, case-insensitive
        String regex = "(?i)(?:^|[^\\p{L}])" + java.util.regex.Pattern.quote(token) + "(?![\\p{L}])";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(body);
        if (!m.find()) return null;
        int start = m.end();
        // skip whitespace and punctuation/control chars
        while (start < body.length()) {
            char ch = body.charAt(start);
            if (Character.isLetter(ch) || ch == '/' || ch == '^' || (ch >= 0x0400 && ch <= 0x052F)) {
                break;
            }
            start++;
        }
        int end = indexOfNextTag(body, start);
        if (end < 0) end = body.length();
        String raw = body.substring(start, end);
        raw = raw.replaceAll("[\\p{Cntrl}]", "").trim();
        return raw;
    }

    private static String extractFirstSigCharAfter(String token, String body) {
        if (body == null || token == null) return null;
        int i = body.indexOf(token);
        if (i < 0) return null;
        int p = i + token.length();
        while (p < body.length()) {
            char ch = body.charAt(p);
            if (Character.isLetterOrDigit(ch) || (ch >= 0x0400 && ch <= 0x052F)) {
                return String.valueOf(ch);
            }
            p++;
        }
        return null;
    }

    private static boolean hasNonAscii(byte[] bytes) {
        for (byte b : bytes) {
            if ((b & 0x80) != 0) return true;
        }
        return false;
    }

    private static String baseName(String path) {
        if (path == null || path.isBlank()) return null;
        String p = path.strip();
        // Normalize backslashes
        p = p.replace('\\', '/');
        int q = p.lastIndexOf('/');
        String name = (q >= 0) ? p.substring(q + 1) : p;
        // If URL with query, strip it
        int qi = name.indexOf('?');
        if (qi >= 0) name = name.substring(0, qi);
        // If still blank, return original path
        if (name.isBlank()) return p;
        return name;
    }

    /**
     * Some .rel exports include large submitter/author blocks wrapped by SUBM … SUBM1.
     * These blocks can inject noise into NAME and other fields. Strip them early.
     */
    private static String stripSubmitterBlocks(String s) {
        if (s == null || s.isEmpty()) return s;
        // Keep only the content between the very first SUBM and the very last SUBM1 (case-insensitive).
        String up = s.toUpperCase(java.util.Locale.ROOT);
        int first = up.indexOf("SUBM");
        int last = up.lastIndexOf("SUBM1");
        if (first >= 0 && last > first) {
            int start = first + 4; // after 'SUBM'
            // Trim leading whitespace/newlines after SUBM and before SUBM1
            while (start < s.length() && Character.isWhitespace(s.charAt(start))) start++;
            int end = last;
            while (end > start && Character.isWhitespace(s.charAt(end - 1))) end--;
            return s.substring(start, end);
        }
        return s;
    }

    private static ParseBundle parseBundle(String text) {
        List<Section> sections = splitSections(text);
        Map<String, PersonRec> persons = new LinkedHashMap<>();
        Map<String, FamRec> fams = new LinkedHashMap<>();
        for (Section s : sections) {
            if (s.id.startsWith("P")) {
                persons.put(s.id, parsePerson(s));
            }
        }
        for (Section s : sections) {
            if (s.id.startsWith("F")) {
                fams.put(s.id, parseFamily(s));
            }
        }
        return new ParseBundle(persons, fams);
    }

    // Ultra-robust: directly slice after raw NAME until next known tag (case-insensitive)
    private static String extractNameSimple(String body) {
        if (body == null || body.isEmpty()) return null;
        String up = body.toUpperCase(java.util.Locale.ROOT);
        int i = up.indexOf("NAME");
        if (i < 0) return null;
        int start = i + 4;
        // skip any separators/control
        while (start < body.length()) {
            char ch = body.charAt(start);
            if (!Character.isWhitespace(ch)
                    && ch != ':' && ch != '=' && ch != '"' && ch != '\'' && ch != '\\'
                    && ch != '$' && ch != ';' && ch != ',' && ch != 0x00) break;
            start++;
        }
        // find next tag boundary
        String[] toks = {"SEX", "BIRT", "DEAT", "FAMC", "FAMS", "NOTE", "NOTES", "OBJE", "TITL", "FILE", "FORM", "SOUR", "SUBM", "_X", "_Y", "P", "F"};
        int end = body.length();
        for (String t : toks) {
            int idx = up.indexOf(t, start);
            if (idx >= 0 && idx < end) end = idx;
        }
        String raw = body.substring(start, Math.max(start, end));
        raw = raw.replaceAll("[\\p{Cntrl}]", " ").trim();
        return raw;
    }

    private static class ParseBundle {
        final Map<String, PersonRec> persons;
        final Map<String, FamRec> fams;
        ParseBundle(Map<String, PersonRec> persons, Map<String, FamRec> fams) {
            this.persons = persons;
            this.fams = fams;
        }
    }
}
