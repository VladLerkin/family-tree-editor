package com.pedigree.gedcom;

import com.pedigree.model.Family;
import com.pedigree.model.Individual;
import com.pedigree.storage.ProjectRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class GedcomExporter {
    public void exportToFile(ProjectRepository.ProjectData data, Path path) throws IOException {
        if (data == null) throw new IllegalArgumentException("data is null");
        if (path == null) throw new IllegalArgumentException("path is null");

        List<Individual> inds = data.individuals;
        List<Family> fams = data.families;
        Map<String, String> indXref = GedcomMapper.buildIndividualXrefs(inds);
        Map<String, String> famXref = GedcomMapper.buildFamilyXrefs(fams);

        // Ensure parent directory exists
        Path dir = path.toAbsolutePath().getParent();
        if (dir != null && !Files.exists(dir)) Files.createDirectories(dir);

        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            // Header (GEDCOM 5.5 structure; using UNICODE charset)
            writeln(w, "0 HEAD");
            writeln(w, "1 SOUR PedigreeChartEditor");
            writeln(w, "2 VERS 1.0");
            writeln(w, "1 GEDC");
            writeln(w, "2 VERS 5.5");
            writeln(w, "2 FORM LINEAGE-LINKED");
            writeln(w, "1 CHAR UNICODE");

            // Individuals
            for (int i = 0; i < inds.size(); i++) {
                Individual ind = inds.get(i);
                String xref = indXref.get(ind.getId());
                writeln(w, "0 " + xref + " INDI");
                writeln(w, "1 NAME " + GedcomMapper.buildName(ind));
                writeln(w, "1 SEX " + GedcomMapper.sexCode(ind.getGender()));
                if (ind.getBirthDate() != null || (ind.getBirthPlace() != null && !ind.getBirthPlace().isBlank())) {
                    writeln(w, "1 BIRT");
                    if (ind.getBirthDate() != null) writeln(w, "2 DATE " + ind.getBirthDate());
                    if (ind.getBirthPlace() != null && !ind.getBirthPlace().isBlank()) writeln(w, "2 PLAC " + ind.getBirthPlace());
                }
                if (ind.getDeathDate() != null || (ind.getDeathPlace() != null && !ind.getDeathPlace().isBlank())) {
                    writeln(w, "1 DEAT");
                    if (ind.getDeathDate() != null) writeln(w, "2 DATE " + ind.getDeathDate());
                    if (ind.getDeathPlace() != null && !ind.getDeathPlace().isBlank()) writeln(w, "2 PLAC " + ind.getDeathPlace());
                }
                // Family child/spouse links
                for (Family f : fams) {
                    if (f.getChildrenIds().contains(ind.getId())) {
                        String fx = famXref.get(f.getId());
                        if (fx != null) writeln(w, "1 FAMC " + fx);
                    }
                    if ((f.getHusbandId() != null && f.getHusbandId().equals(ind.getId())) ||
                        (f.getWifeId() != null && f.getWifeId().equals(ind.getId()))) {
                        String fx = famXref.get(f.getId());
                        if (fx != null) writeln(w, "1 FAMS " + fx);
                    }
                }
            }

            // Families
            for (int i = 0; i < fams.size(); i++) {
                Family fam = fams.get(i);
                String xref = famXref.get(fam.getId());
                writeln(w, "0 " + xref + " FAM");
                if (fam.getHusbandId() != null) {
                    String ix = indXref.get(fam.getHusbandId());
                    if (ix != null) writeln(w, "1 HUSB " + ix);
                }
                if (fam.getWifeId() != null) {
                    String ix = indXref.get(fam.getWifeId());
                    if (ix != null) writeln(w, "1 WIFE " + ix);
                }
                for (String cid : fam.getChildrenIds()) {
                    String ix = indXref.get(cid);
                    if (ix != null) writeln(w, "1 CHIL " + ix);
                }
                if (fam.getMarriageDate() != null || (fam.getMarriagePlace() != null && !fam.getMarriagePlace().isBlank())) {
                    writeln(w, "1 MARR");
                    if (fam.getMarriageDate() != null) writeln(w, "2 DATE " + fam.getMarriageDate());
                    if (fam.getMarriagePlace() != null && !fam.getMarriagePlace().isBlank()) writeln(w, "2 PLAC " + fam.getMarriagePlace());
                }
            }

            // Trailer
            writeln(w, "0 TRLR");
        }
    }

    private static void writeln(BufferedWriter w, String line) throws IOException {
        w.write(line);
        w.write("\r\n"); // CRLF is common in GEDCOM files
    }
}


