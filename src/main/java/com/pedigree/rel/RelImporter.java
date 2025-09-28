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
            if ((r.given != null && !r.given.isBlank()) || (r.surname != null && !r.surname.isBlank())) {
                first = (r.given != null && !r.given.isBlank()) ? cleanToken(r.given) : "?";
                last = (r.surname != null) ? cleanToken(r.surname) : "";
            } else {
                String[] nm = splitName(r.name);
                first = nm[0];
                last = nm[1];
            }
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
            if (r.death != null) ind.setDeathDate(r.death);
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
    private static final Pattern NAME_RE = Pattern.compile("NAME\\s*([\\s\\S]+?)\\s*(?=(SEX|BIRT|DEAT|FAMC|FAMS|NOTE|TITL|SUBM|SOUR|P\\d+|F\\d+|_X|_Y)|$)", Pattern.DOTALL);
    // Some files show a corrupted first letter in NAME (e.g., '\u044EAME' instead of 'NAME').
    private static final Pattern NAME_ALT_RE = Pattern.compile("(?:NAME|.AME)\\s*([\\s\\S]+?)\\s*(?=(SEX|BIRT|DEAT|FAMC|FAMS|NOTE|TITL|SUBM|SOUR|P\\d+|F\\d+)|$)", Pattern.DOTALL);
    private static final Pattern GIVN_RE = Pattern.compile("GIVN\\P{L}*([^\\r\\n]+)");
    private static final Pattern SURN_RE = Pattern.compile("SURN\\P{L}*([^\\r\\n]+)");
    // Russian localized tokens (Имя, Фамилия, Отчество, Пол)
    private static final int RU_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern RUS_GIVN_RE = Pattern.compile("ИМЯ\\P{L}*([^\\r\\n]+)", RU_FLAGS);
    private static final Pattern RUS_SURN_RE = Pattern.compile("ФАМИЛИ[ЯИ]\\P{L}*([^\\r\\n]+)", RU_FLAGS);
    private static final Pattern RUS_SEX_LETTER_RE = Pattern.compile("ПОЛ\\P{L}*([МЖ])", RU_FLAGS);
    private static final Pattern RUS_SEX_WORD_RE = Pattern.compile("(Муж(ской)?|Жен(ский)?)", RU_FLAGS);
    private static final Pattern BIRT_RE = Pattern.compile("BIRT.*?DATE\\((.+?)\\)", Pattern.DOTALL);
    private static final Pattern DEAT_RE = Pattern.compile("DEAT.*?DATE\\((.+?)\\)", Pattern.DOTALL);
    private static final int POS_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern POS_X_RE = Pattern.compile("_X\\P{Alnum}*([+-]?\\d+(?:[.,]\\d+)?)", POS_FLAGS);
    private static final Pattern POS_Y_RE = Pattern.compile("_Y\\P{Alnum}*([+-]?\\d+(?:[.,]\\d+)?)", POS_FLAGS);
    // Fallback: inline GEDCOM-like "Given /Surname/" anywhere before SEX
    private static final Pattern INLINE_GEDCOM_NAME = Pattern.compile("([\\p{L} .]+?/[^/]+/)");

    private static PersonRec parsePerson(Section s) {
        PersonRec r = new PersonRec();
        r.id = s.id;
        String body = sanitize(s.body);
        r.name = find(NAME_RE, body);
        // Fallback if regex failed: try alternative corrupted tag pattern or slice between tokens
        if (r.name == null || r.name.isBlank()) {
            String alt = find(NAME_ALT_RE, body);
            if (alt != null && !alt.isBlank()) {
                r.name = alt;
            } else {
                String sliced = extractAfterToken("NAME", body);
                if (sliced != null && !sliced.isBlank()) r.name = sliced;
                else {
                    // Final fallback: scan for inline GEDCOM-like name before SEX
                    String preSex = body;
                    int sexAt = body.indexOf("SEX");
                    if (sexAt > 0) preSex = body.substring(0, sexAt);
                    String inline = find(INLINE_GEDCOM_NAME, preSex);
                    if (inline != null && !inline.isBlank()) r.name = inline;
                }
            }
        }
        // Some REL exports store names as separate GIVN/SURN tokens
        String givn = find(GIVN_RE, body);
        String surn = find(SURN_RE, body);
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
        r.birth = parseDate(find(BIRT_RE, body));
        r.death = parseDate(find(DEAT_RE, body));
        String xs = find(POS_X_RE, body);
        String ys = find(POS_Y_RE, body);
        if (xs != null) try { r.x = Double.parseDouble(xs.trim()); } catch (NumberFormatException ignored) {}
        if (ys != null) try { r.y = Double.parseDouble(ys.trim()); } catch (NumberFormatException ignored) {}
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
        f.marrDate = parseDate(find(MARR_DATE_RE, body));
        String plac = find(MARR_PLAC_RE, body);
        if (plac != null) f.marrPlace = plac.strip();
        return f;
    }

    private static String find(Pattern p, String s) {
        Matcher m = p.matcher(s);
        if (m.find()) {
            return m.group(m.groupCount() >= 1 ? 1 : 0);
        }
        return null;
    }

    private static LocalDate parseDate(String s) {
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
                return new String[]{given, surname};
            } else if (parts.length == 1) {
                // Only one token: assume it's a surname
                return new String[]{"?", cleanToken(parts[0])};
            }
        }
        String given = src;
        String surname = "";
        int a = src.indexOf('/');
        int b = src.indexOf('/', a + 1);
        if (a >= 0 && b > a) {
            surname = cleanToken(src.substring(a + 1, b).strip());
            given = cleanToken((src.substring(0, a) + src.substring(b + 1)).strip());
        } else {
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
        return new String[]{given, surname};
    }

    private static String cleanToken(String s) {
        if (s == null) return null;
        String t = s.strip();
        // Remove leading non-letter characters (digits, punctuation). Keep letters in any script.
        t = t.replaceFirst("^[^\\p{L}]+", "");
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
        LocalDate birth;
        LocalDate death;
        Double x; // optional parsed position
        Double y;
    }

    private static class FamRec {
        String id;
        String husb;
        String wife;
        List<String> children = new ArrayList<>();
        LocalDate marrDate;
        String marrPlace;
    }

    private static String sanitize(String s) {
        if (s == null) return null;
        // Remove all control characters except CR and LF to preserve potential line structure
        return s.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "");
    }

    private static int indexOfNextTag(String s, int from) {
        if (s == null) return -1;
        int min = -1;
        String[] tags = {"SEX", "BIRT", "DEAT", "FAMC", "FAMS", "NOTE", "TITL", "P", "F"};
        for (String t : tags) {
            int i = s.indexOf(t, from);
            if (i >= 0 && (min < 0 || i < min)) min = i;
        }
        return min;
    }

    private static String extractAfterToken(String token, String body) {
        if (body == null || token == null) return null;
        int i = body.indexOf(token);
        if (i < 0) return null;
        int start = i + token.length();
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

    private static class ParseBundle {
        final Map<String, PersonRec> persons;
        final Map<String, FamRec> fams;
        ParseBundle(Map<String, PersonRec> persons, Map<String, FamRec> fams) {
            this.persons = persons;
            this.fams = fams;
        }
    }
}
