package com.pedigree.gedcom;

import com.pedigree.model.Family;
import com.pedigree.model.Gender;
import com.pedigree.model.Individual;
import com.pedigree.storage.ProjectRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GedcomImporter {
    private static final Pattern LINE = Pattern.compile("^\\s*(\\d+)\\s+(?:(@[^@]+@)\\s+)?([A-Z0-9_]+)(?:\\s+(.*))?$");

    public ProjectRepository.ProjectData importFromFile(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        Map<String, IndiRec> inds = new LinkedHashMap<>();
        Map<String, FamRec> fams = new LinkedHashMap<>();

        String currentRecord = null; // INDI, FAM, etc.
        IndiRec curInd = null;
        FamRec curFam = null;
        Deque<String> context = new ArrayDeque<>();
        int prevLevel = -1;

        for (String raw : lines) {
            if (raw == null || raw.isBlank()) continue;
            Matcher m = LINE.matcher(raw);
            if (!m.matches()) continue; // skip unrecognized lines
            int level = Integer.parseInt(m.group(1));
            String xref = m.group(2);
            String tag = m.group(3);
            String value = m.group(4) != null ? m.group(4).trim() : null;

            // Adjust context stack
            while (context.size() > 0 && context.size() > level) context.pop();

            if (level == 0) {
                currentRecord = tag;
                curInd = null;
                curFam = null;
                context.clear();
                if ("INDI".equals(tag)) {
                    curInd = new IndiRec();
                    curInd.xref = xref;
                    inds.put(xref != null ? xref : UUID.randomUUID().toString(), curInd);
                } else if ("FAM".equals(tag)) {
                    curFam = new FamRec();
                    curFam.xref = xref;
                    fams.put(xref != null ? xref : UUID.randomUUID().toString(), curFam);
                }
            } else if ("INDI".equals(currentRecord) && curInd != null) {
                if (level == 1) {
                    context.clear();
                    context.push(tag);
                    switch (tag) {
                        case "NAME" -> {
                            String[] nm = GedcomMapper.parseName(value);
                            curInd.given = nm[0];
                            curInd.surname = nm[1];
                        }
                        case "SEX" -> curInd.sex = GedcomMapper.parseSex(value);
                        case "BIRT", "DEAT" -> { /* handled via level 2 DATE/PLAC */ }
                        case "FAMC" -> { if (value != null) curInd.famcXrefs.add(value); }
                        case "FAMS" -> { if (value != null) curInd.famsXrefs.add(value); }
                        default -> { /* ignore other tags for now */ }
                    }
                } else if (level == 2) {
                    String ctx = context.peek();
                    if (ctx == null) continue;
                    switch (ctx) {
                        case "BIRT" -> {
                            if ("DATE".equals(tag)) curInd.birthDate = value;
                            else if ("PLAC".equals(tag)) curInd.birthPlace = value;
                        }
                        case "DEAT" -> {
                            if ("DATE".equals(tag)) curInd.deathDate = value;
                            else if ("PLAC".equals(tag)) curInd.deathPlace = value;
                        }
                        default -> { /* ignore */ }
                    }
                }
            } else if ("FAM".equals(currentRecord) && curFam != null) {
                if (level == 1) {
                    context.clear();
                    context.push(tag);
                    switch (tag) {
                        case "HUSB" -> curFam.husbXref = value;
                        case "WIFE" -> curFam.wifeXref = value;
                        case "CHIL" -> { if (value != null) curFam.childXrefs.add(value); }
                        case "MARR" -> { /* handle sub-tags */ }
                        default -> { /* ignore */ }
                    }
                } else if (level == 2) {
                    String ctx = context.peek();
                    if ("MARR".equals(ctx)) {
                        if ("DATE".equals(tag)) curFam.marrDate = value;
                        else if ("PLAC".equals(tag)) curFam.marrPlace = value;
                    }
                }
            }
            prevLevel = level;
        }

        // Build model objects
        ProjectRepository.ProjectData data = new ProjectRepository.ProjectData();
        Map<String, String> indiIdByXref = new HashMap<>();
        for (Map.Entry<String, IndiRec> e : inds.entrySet()) {
            IndiRec r = e.getValue();
            String given = r.given != null ? r.given : "";
            String surname = r.surname != null ? r.surname : "";
            Gender sex = r.sex != null ? r.sex : Gender.UNKNOWN;
            Individual ind = new Individual(given.isEmpty() ? "?" : given, surname, sex);
            if (r.birthDate != null) ind.setBirthDate(r.birthDate);
            if (r.birthPlace != null) ind.setBirthPlace(r.birthPlace);
            if (r.deathDate != null) ind.setDeathDate(r.deathDate);
            if (r.deathPlace != null) ind.setDeathPlace(r.deathPlace);
            data.individuals.add(ind);
            if (r.xref != null) indiIdByXref.put(r.xref, ind.getId());
        }

        Map<String, String> famIdByXref = new HashMap<>();
        for (Map.Entry<String, FamRec> e : fams.entrySet()) {
            FamRec r = e.getValue();
            Family fam = new Family();
            if (r.husbXref != null) fam.setHusbandId(indiIdByXref.get(r.husbXref));
            if (r.wifeXref != null) fam.setWifeId(indiIdByXref.get(r.wifeXref));
            for (String cx : r.childXrefs) {
                String id = indiIdByXref.get(cx);
                if (id != null) fam.getChildrenIds().add(id);
            }
            if (r.marrDate != null) fam.setMarriageDate(r.marrDate);
            if (r.marrPlace != null) fam.setMarriagePlace(r.marrPlace);
            data.families.add(fam);
            if (r.xref != null) famIdByXref.put(r.xref, fam.getId());

            // Build relationships used by renderer
            // spouse -> family
            if (fam.getHusbandId() != null) {
                com.pedigree.model.Relationship rel = new com.pedigree.model.Relationship();
                rel.setType(com.pedigree.model.Relationship.Type.SPOUSE_TO_FAMILY);
                rel.setFromId(fam.getHusbandId());
                rel.setToId(fam.getId());
                data.relationships.add(rel);
            }
            if (fam.getWifeId() != null) {
                com.pedigree.model.Relationship rel = new com.pedigree.model.Relationship();
                rel.setType(com.pedigree.model.Relationship.Type.SPOUSE_TO_FAMILY);
                rel.setFromId(fam.getWifeId());
                rel.setToId(fam.getId());
                data.relationships.add(rel);
            }
            // family -> child
            for (String cid : fam.getChildrenIds()) {
                com.pedigree.model.Relationship rel = new com.pedigree.model.Relationship();
                rel.setType(com.pedigree.model.Relationship.Type.FAMILY_TO_CHILD);
                rel.setFromId(fam.getId());
                rel.setToId(cid);
                data.relationships.add(rel);
            }
        }

        return data;
    }

    private static class IndiRec {
        String xref;
        String given;
        String surname;
        Gender sex;
        String birthDate;
        String birthPlace;
        String deathDate;
        String deathPlace;
        List<String> famcXrefs = new ArrayList<>();
        List<String> famsXrefs = new ArrayList<>();
    }

    private static class FamRec {
        String xref;
        String husbXref;
        String wifeXref;
        List<String> childXrefs = new ArrayList<>();
        String marrDate;
        String marrPlace;
    }
}


