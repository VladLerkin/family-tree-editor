package com.pedigree.gedcom;

import com.pedigree.model.*;
import com.pedigree.storage.ProjectRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GedcomExporter {
    public void exportToFile(ProjectRepository.ProjectData data, Path path) throws IOException {
        if (data == null) throw new IllegalArgumentException("data is null");
        if (path == null) throw new IllegalArgumentException("path is null");

        List<Individual> inds = data.individuals;
        List<Family> fams = data.families;
        List<Source> sources = data.sources;
        List<Repository> repos = data.repositories;
        List<Submitter> submitters = data.submitters;
        
        Map<String, String> indXref = GedcomMapper.buildIndividualXrefs(inds);
        Map<String, String> famXref = GedcomMapper.buildFamilyXrefs(fams);

        // Collect all notes and media from individuals and families
        Map<String, Note> allNotes = new LinkedHashMap<>();
        Map<String, MediaAttachment> allMedia = new LinkedHashMap<>();
        
        for (Individual ind : inds) {
            for (Note note : ind.getNotes()) {
                if (note.getId() != null) allNotes.put(note.getId(), note);
            }
            for (MediaAttachment media : ind.getMedia()) {
                if (media.getId() != null) allMedia.put(media.getId(), media);
            }
            for (GedcomEvent event : ind.getEvents()) {
                for (Note note : event.getNotes()) {
                    if (note.getId() != null) allNotes.put(note.getId(), note);
                }
            }
        }
        for (Family fam : fams) {
            for (Note note : fam.getNotes()) {
                if (note.getId() != null) allNotes.put(note.getId(), note);
            }
            for (MediaAttachment media : fam.getMedia()) {
                if (media.getId() != null) allMedia.put(media.getId(), media);
            }
            for (GedcomEvent event : fam.getEvents()) {
                for (Note note : event.getNotes()) {
                    if (note.getId() != null) allNotes.put(note.getId(), note);
                }
            }
        }

        // Build xrefs for notes, media, sources, repositories, submitters
        Map<String, String> noteXref = new LinkedHashMap<>();
        int noteIdx = 1;
        for (String noteId : allNotes.keySet()) {
            noteXref.put(noteId, "@N" + (noteIdx++) + "@");
        }
        
        Map<String, String> mediaXref = new LinkedHashMap<>();
        int mediaIdx = 1;
        for (String mediaId : allMedia.keySet()) {
            mediaXref.put(mediaId, "@M" + (mediaIdx++) + "@");
        }
        
        Map<String, String> sourceXref = new LinkedHashMap<>();
        int sourceIdx = 1;
        for (Source src : sources) {
            sourceXref.put(src.getId(), "@S" + (sourceIdx++) + "@");
        }
        
        Map<String, String> repoXref = new LinkedHashMap<>();
        int repoIdx = 1;
        for (Repository repo : repos) {
            repoXref.put(repo.getId(), "@R" + (repoIdx++) + "@");
        }
        
        Map<String, String> submXref = new LinkedHashMap<>();
        int submIdx = 1;
        for (Submitter subm : submitters) {
            submXref.put(subm.getId(), "@U" + (submIdx++) + "@");
        }

        // Ensure parent directory exists
        Path dir = path.toAbsolutePath().getParent();
        if (dir != null && !Files.exists(dir)) Files.createDirectories(dir);

        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            // Header (GEDCOM 5.5.5 structure)
            writeln(w, "0 HEAD");
            writeln(w, "1 GEDC");
            writeln(w, "2 VERS 5.5.5");
            writeln(w, "2 FORM LINEAGE-LINKED");
            writeln(w, "3 VERS 5.5.5");
            writeln(w, "1 CHAR UTF-8");
            writeln(w, "1 SOUR PedigreeChartEditor");
            writeln(w, "2 NAME Pedigree Chart Editor");
            writeln(w, "2 VERS 1.0");
            writeln(w, "1 DATE " + formatHeaderDate(LocalDateTime.now()));
            writeln(w, "2 TIME " + formatHeaderTime(LocalDateTime.now()));
            
            // Add first submitter reference if available
            if (!submitters.isEmpty()) {
                String firstSubmXref = submXref.get(submitters.get(0).getId());
                if (firstSubmXref != null) {
                    writeln(w, "1 SUBM " + firstSubmXref);
                }
            }

            // Submitters
            for (Submitter subm : submitters) {
                String sx = submXref.get(subm.getId());
                if (sx != null) {
                    writeln(w, "0 " + sx + " SUBM");
                    if (subm.getName() != null && !subm.getName().isBlank()) {
                        writeln(w, "1 NAME " + escapeValue(subm.getName()));
                    }
                    if (subm.getAddress() != null) {
                        writeAddress(w, 1, subm.getAddress());
                    }
                    if (subm.getPhone() != null && !subm.getPhone().isBlank()) {
                        writeln(w, "1 PHON " + escapeValue(subm.getPhone()));
                    }
                    if (subm.getEmail() != null && !subm.getEmail().isBlank()) {
                        writeln(w, "1 EMAIL " + escapeValue(subm.getEmail()));
                    }
                }
            }

            // Individuals
            for (Individual ind : inds) {
                String xref = indXref.get(ind.getId());
                writeln(w, "0 " + xref + " INDI");
                writeln(w, "1 NAME " + GedcomMapper.buildName(ind));
                
                String surname = ind.getLastName();
                String givenName = ind.getFirstName();
                if (surname != null && !surname.isBlank()) {
                    writeln(w, "2 SURN " + escapeValue(surname));
                }
                if (givenName != null && !givenName.isBlank()) {
                    writeln(w, "2 GIVN " + escapeValue(givenName));
                }
                
                writeln(w, "1 SEX " + GedcomMapper.sexCode(ind.getGender()));
                
                // Export events
                for (GedcomEvent event : ind.getEvents()) {
                    if (event.getType() != null) {
                        writeln(w, "1 " + event.getType());
                        if (event.getDate() != null && !event.getDate().isBlank()) {
                            writeln(w, "2 DATE " + event.getDate());
                        }
                        if (event.getPlace() != null && !event.getPlace().isBlank()) {
                            writeln(w, "2 PLAC " + event.getPlace());
                        }
                        
                        // Export source citations
                        for (SourceCitation sc : event.getSources()) {
                            String srcXref = sourceXref.get(sc.getSourceId());
                            if (srcXref != null) {
                                writeln(w, "2 SOUR " + srcXref);
                                if (sc.getPage() != null && !sc.getPage().isBlank()) {
                                    writeln(w, "3 PAGE " + escapeValue(sc.getPage()));
                                }
                            }
                        }
                        
                        // Export event attributes
                        for (GedcomAttribute attr : event.getAttributes()) {
                            if (attr.getTag() != null && attr.getValue() != null) {
                                writeln(w, "2 " + attr.getTag() + " " + escapeValue(attr.getValue()));
                            }
                        }
                    }
                }
                
                // Backward compatibility: export BIRT/DEAT if not in events
                boolean hasBirt = ind.getEvents().stream().anyMatch(e -> "BIRT".equals(e.getType()));
                boolean hasDeat = ind.getEvents().stream().anyMatch(e -> "DEAT".equals(e.getType()));
                
                if (!hasBirt && (ind.getBirthDate() != null || (ind.getBirthPlace() != null && !ind.getBirthPlace().isBlank()))) {
                    writeln(w, "1 BIRT");
                    if (ind.getBirthDate() != null) writeln(w, "2 DATE " + ind.getBirthDate());
                    if (ind.getBirthPlace() != null && !ind.getBirthPlace().isBlank()) writeln(w, "2 PLAC " + ind.getBirthPlace());
                }
                
                if (!hasDeat && (ind.getDeathDate() != null || (ind.getDeathPlace() != null && !ind.getDeathPlace().isBlank()))) {
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
                
                // Notes
                for (Note note : ind.getNotes()) {
                    String nx = noteXref.get(note.getId());
                    if (nx != null) writeln(w, "1 NOTE " + nx);
                }
                
                // Media
                for (MediaAttachment media : ind.getMedia()) {
                    String mx = mediaXref.get(media.getId());
                    if (mx != null) writeln(w, "1 OBJE " + mx);
                }
                
                // Tags as custom fields
                for (Tag tag : ind.getTags()) {
                    if (tag.getName() != null && !tag.getName().isBlank()) {
                        writeln(w, "1 _TAG " + escapeValue(tag.getName()));
                    }
                }
            }

            // Families
            for (Family fam : fams) {
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
                
                // Export events
                for (GedcomEvent event : fam.getEvents()) {
                    if (event.getType() != null) {
                        writeln(w, "1 " + event.getType());
                        if (event.getDate() != null && !event.getDate().isBlank()) {
                            writeln(w, "2 DATE " + event.getDate());
                        }
                        if (event.getPlace() != null && !event.getPlace().isBlank()) {
                            writeln(w, "2 PLAC " + event.getPlace());
                        }
                        
                        // Export source citations
                        for (SourceCitation sc : event.getSources()) {
                            String srcXref = sourceXref.get(sc.getSourceId());
                            if (srcXref != null) {
                                writeln(w, "2 SOUR " + srcXref);
                                if (sc.getPage() != null && !sc.getPage().isBlank()) {
                                    writeln(w, "3 PAGE " + escapeValue(sc.getPage()));
                                }
                            }
                        }
                    }
                }
                
                // Backward compatibility: export MARR if not in events
                boolean hasMarr = fam.getEvents().stream().anyMatch(e -> "MARR".equals(e.getType()));
                if (!hasMarr && (fam.getMarriageDate() != null || (fam.getMarriagePlace() != null && !fam.getMarriagePlace().isBlank()))) {
                    writeln(w, "1 MARR");
                    if (fam.getMarriageDate() != null) writeln(w, "2 DATE " + fam.getMarriageDate());
                    if (fam.getMarriagePlace() != null && !fam.getMarriagePlace().isBlank()) writeln(w, "2 PLAC " + fam.getMarriagePlace());
                }
                
                // Notes
                for (Note note : fam.getNotes()) {
                    String nx = noteXref.get(note.getId());
                    if (nx != null) writeln(w, "1 NOTE " + nx);
                }
                
                // Media
                for (MediaAttachment media : fam.getMedia()) {
                    String mx = mediaXref.get(media.getId());
                    if (mx != null) writeln(w, "1 OBJE " + mx);
                }
                
                // Tags as custom fields
                for (Tag tag : fam.getTags()) {
                    if (tag.getName() != null && !tag.getName().isBlank()) {
                        writeln(w, "1 _TAG " + escapeValue(tag.getName()));
                    }
                }
            }

            // Source records
            for (Source src : sources) {
                String sx = sourceXref.get(src.getId());
                if (sx != null) {
                    writeln(w, "0 " + sx + " SOUR");
                    if (src.getAgency() != null && !src.getAgency().isBlank()) {
                        writeln(w, "1 DATA");
                        writeln(w, "2 AGNC " + escapeValue(src.getAgency()));
                    }
                    if (src.getTitle() != null && !src.getTitle().isBlank()) {
                        writeln(w, "1 TITL " + escapeValue(src.getTitle()));
                    }
                    if (src.getAbbreviation() != null && !src.getAbbreviation().isBlank()) {
                        writeln(w, "1 ABBR " + escapeValue(src.getAbbreviation()));
                    }
                    if (src.getRepositoryId() != null) {
                        String rx = repoXref.get(src.getRepositoryId());
                        if (rx != null) {
                            writeln(w, "1 REPO " + rx);
                            if (src.getCallNumber() != null && !src.getCallNumber().isBlank()) {
                                writeln(w, "2 CALN " + escapeValue(src.getCallNumber()));
                            }
                        }
                    }
                }
            }

            // Repository records
            for (Repository repo : repos) {
                String rx = repoXref.get(repo.getId());
                if (rx != null) {
                    writeln(w, "0 " + rx + " REPO");
                    if (repo.getName() != null && !repo.getName().isBlank()) {
                        writeln(w, "1 NAME " + escapeValue(repo.getName()));
                    }
                    if (repo.getAddress() != null) {
                        writeAddress(w, 1, repo.getAddress());
                    }
                    if (repo.getPhone() != null && !repo.getPhone().isBlank()) {
                        writeln(w, "1 PHON " + escapeValue(repo.getPhone()));
                    }
                    if (repo.getEmail() != null && !repo.getEmail().isBlank()) {
                        writeln(w, "1 EMAIL " + escapeValue(repo.getEmail()));
                    }
                    if (repo.getWebsite() != null && !repo.getWebsite().isBlank()) {
                        writeln(w, "1 WWW " + escapeValue(repo.getWebsite()));
                    }
                }
            }

            // Note records
            for (Map.Entry<String, Note> entry : allNotes.entrySet()) {
                Note note = entry.getValue();
                String nx = noteXref.get(entry.getKey());
                if (nx != null) {
                    writeln(w, "0 " + nx + " NOTE");
                    if (note.getText() != null && !note.getText().isBlank()) {
                        writeMultilineValue(w, 1, note.getText());
                    }
                    // Export sources for the note
                    if (note.getSources() != null) {
                        for (SourceCitation sc : note.getSources()) {
                            String sx = sourceXref.get(sc.getSourceId());
                            if (sx != null) {
                                writeln(w, "1 SOUR " + sx);
                                if (sc.getPage() != null && !sc.getPage().isBlank()) {
                                    writeln(w, "2 PAGE " + escapeValue(sc.getPage()));
                                }
                            }
                        }
                    }
                }
            }

            // Media (OBJE) records
            for (Map.Entry<String, MediaAttachment> entry : allMedia.entrySet()) {
                MediaAttachment media = entry.getValue();
                String mx = mediaXref.get(entry.getKey());
                if (mx != null) {
                    writeln(w, "0 " + mx + " OBJE");
                    if (media.getFileName() != null && !media.getFileName().isBlank()) {
                        writeln(w, "1 FILE " + escapeValue(media.getFileName()));
                    }
                    if (media.getRelativePath() != null && !media.getRelativePath().isBlank()) {
                        writeln(w, "1 _PATH " + escapeValue(media.getRelativePath()));
                    }
                }
            }

            // Trailer
            writeln(w, "0 TRLR");
        }
    }

    private void writeAddress(BufferedWriter w, int level, Address addr) throws IOException {
        writeln(w, level + " ADDR");
        if (addr.getLine1() != null && !addr.getLine1().isBlank()) {
            writeln(w, (level + 1) + " ADR1 " + escapeValue(addr.getLine1()));
        }
        if (addr.getLine2() != null && !addr.getLine2().isBlank()) {
            writeln(w, (level + 1) + " ADR2 " + escapeValue(addr.getLine2()));
        }
        if (addr.getLine3() != null && !addr.getLine3().isBlank()) {
            writeln(w, (level + 1) + " ADR3 " + escapeValue(addr.getLine3()));
        }
        if (addr.getCity() != null && !addr.getCity().isBlank()) {
            writeln(w, (level + 1) + " CITY " + escapeValue(addr.getCity()));
        }
        if (addr.getState() != null && !addr.getState().isBlank()) {
            writeln(w, (level + 1) + " STAE " + escapeValue(addr.getState()));
        }
        if (addr.getPostalCode() != null && !addr.getPostalCode().isBlank()) {
            writeln(w, (level + 1) + " POST " + escapeValue(addr.getPostalCode()));
        }
        if (addr.getCountry() != null && !addr.getCountry().isBlank()) {
            writeln(w, (level + 1) + " CTRY " + escapeValue(addr.getCountry()));
        }
    }

    private String formatHeaderDate(LocalDateTime dt) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
        return dt.format(fmt).toUpperCase(Locale.ROOT);
    }

    private String formatHeaderTime(LocalDateTime dt) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dt.format(fmt);
    }

    private String escapeValue(String value) {
        if (value == null) return "";
        return value.replace("\r", " ").replace("\n", " ");
    }

    private void writeMultilineValue(BufferedWriter w, int level, String text) throws IOException {
        if (text == null || text.isEmpty()) return;
        
        String[] lines = text.split("\\r?\\n");
        boolean first = true;
        for (String line : lines) {
            if (line.length() > 248) {
                int pos = 0;
                boolean firstChunk = first;
                while (pos < line.length()) {
                    int end = Math.min(pos + 248, line.length());
                    String chunk = line.substring(pos, end);
                    if (firstChunk) {
                        writeln(w, level + " CONC " + chunk);
                        firstChunk = false;
                        first = false;
                    } else {
                        writeln(w, level + " CONC " + chunk);
                    }
                    pos = end;
                }
            } else {
                if (first) {
                    writeln(w, level + " CONC " + line);
                    first = false;
                } else {
                    writeln(w, level + " CONT " + line);
                }
            }
        }
    }

    private static void writeln(BufferedWriter w, String line) throws IOException {
        w.write(line);
        w.write("\r\n");
    }
}
