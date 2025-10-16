package com.pedigree.gedcom;

import com.pedigree.model.*;
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
        Map<String, NoteRec> notes = new LinkedHashMap<>();
        Map<String, MediaRec> media = new LinkedHashMap<>();
        Map<String, SourceRec> sources = new LinkedHashMap<>();
        Map<String, RepositoryRec> repos = new LinkedHashMap<>();
        Map<String, SubmitterRec> submitters = new LinkedHashMap<>();

        String currentRecord = null;
        String currentXref = null;
        IndiRec curInd = null;
        FamRec curFam = null;
        NoteRec curNote = null;
        MediaRec curMedia = null;
        SourceRec curSource = null;
        RepositoryRec curRepo = null;
        SubmitterRec curSubmitter = null;
        EventRec curEvent = null;
        AddressRec curAddress = null;
        Deque<String> context = new ArrayDeque<>();
        StringBuilder textAccumulator = null;

        for (String raw : lines) {
            if (raw == null || raw.isBlank()) continue;
            Matcher m = LINE.matcher(raw);
            if (!m.matches()) continue;
            int level = Integer.parseInt(m.group(1));
            String xref = m.group(2);
            String tag = m.group(3);
            String value = m.group(4) != null ? m.group(4).trim() : null;

            while (context.size() > level) context.pop();

            if (level == 0) {
                if (textAccumulator != null && curNote != null) {
                    curNote.text = textAccumulator.toString();
                    textAccumulator = null;
                }
                
                currentRecord = tag;
                currentXref = xref;
                curInd = null;
                curFam = null;
                curNote = null;
                curMedia = null;
                curSource = null;
                curRepo = null;
                curSubmitter = null;
                curEvent = null;
                curAddress = null;
                context.clear();
                
                switch (tag) {
                    case "INDI" -> {
                        curInd = new IndiRec();
                        curInd.xref = xref;
                        inds.put(xref != null ? xref : UUID.randomUUID().toString(), curInd);
                    }
                    case "FAM" -> {
                        curFam = new FamRec();
                        curFam.xref = xref;
                        fams.put(xref != null ? xref : UUID.randomUUID().toString(), curFam);
                    }
                    case "NOTE" -> {
                        curNote = new NoteRec();
                        curNote.xref = xref;
                        notes.put(xref != null ? xref : UUID.randomUUID().toString(), curNote);
                        textAccumulator = new StringBuilder();
                        if (value != null && !value.isEmpty()) {
                            textAccumulator.append(value);
                        }
                    }
                    case "OBJE" -> {
                        curMedia = new MediaRec();
                        curMedia.xref = xref;
                        media.put(xref != null ? xref : UUID.randomUUID().toString(), curMedia);
                    }
                    case "SOUR" -> {
                        curSource = new SourceRec();
                        curSource.xref = xref;
                        sources.put(xref != null ? xref : UUID.randomUUID().toString(), curSource);
                    }
                    case "REPO" -> {
                        curRepo = new RepositoryRec();
                        curRepo.xref = xref;
                        repos.put(xref != null ? xref : UUID.randomUUID().toString(), curRepo);
                    }
                    case "SUBM" -> {
                        curSubmitter = new SubmitterRec();
                        curSubmitter.xref = xref;
                        submitters.put(xref != null ? xref : UUID.randomUUID().toString(), curSubmitter);
                    }
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
                        case "BIRT", "DEAT", "BURI", "ADOP", "RESI" -> {
                            curEvent = new EventRec();
                            curEvent.type = tag;
                            curInd.events.add(curEvent);
                        }
                        case "FAMC" -> {
                            if (value != null) {
                                curInd.famcXrefs.add(value);
                            }
                        }
                        case "FAMS" -> {
                            if (value != null) curInd.famsXrefs.add(value);
                        }
                        case "NOTE" -> {
                            if (value != null) curInd.noteXrefs.add(value);
                        }
                        case "OBJE" -> {
                            if (value != null) curInd.mediaXrefs.add(value);
                        }
                        case "_TAG" -> {
                            if (value != null) curInd.tags.add(value);
                        }
                    }
                } else if (level == 2) {
                    String ctx = context.peek();
                    if (ctx == null) continue;
                    if ("BIRT".equals(ctx) || "DEAT".equals(ctx) || "BURI".equals(ctx) || "ADOP".equals(ctx) || "RESI".equals(ctx)) {
                        if (curEvent != null) {
                            switch (tag) {
                                case "DATE" -> curEvent.date = value;
                                case "PLAC" -> curEvent.place = value;
                                case "SOUR" -> {
                                    if (value != null) {
                                        SourceCitationRec sc = new SourceCitationRec();
                                        sc.sourceXref = value;
                                        curEvent.sources.add(sc);
                                        context.push("SOUR");
                                    }
                                }
                            }
                        }
                        // Handle backward compatibility fields
                        if ("BIRT".equals(ctx)) {
                            if ("DATE".equals(tag)) curInd.birthDate = value;
                            else if ("PLAC".equals(tag)) curInd.birthPlace = value;
                        } else if ("DEAT".equals(ctx)) {
                            if ("DATE".equals(tag)) curInd.deathDate = value;
                            else if ("PLAC".equals(tag)) curInd.deathPlace = value;
                        }
                    } else if ("FAMC".equals(ctx)) {
                        if ("PEDI".equals(tag) && value != null) {
                            curInd.pedigree = value;
                        }
                    }
                } else if (level == 3) {
                    String ctx = context.peek();
                    if ("SOUR".equals(ctx) && curEvent != null && !curEvent.sources.isEmpty()) {
                        SourceCitationRec sc = curEvent.sources.get(curEvent.sources.size() - 1);
                        if ("PAGE".equals(tag)) {
                            sc.page = value;
                        }
                    }
                }
            } else if ("FAM".equals(currentRecord) && curFam != null) {
                if (level == 1) {
                    context.clear();
                    context.push(tag);
                    switch (tag) {
                        case "HUSB" -> curFam.husbXref = value;
                        case "WIFE" -> curFam.wifeXref = value;
                        case "CHIL" -> {
                            if (value != null) curFam.childXrefs.add(value);
                        }
                        case "MARR" -> {
                            curEvent = new EventRec();
                            curEvent.type = tag;
                            curFam.events.add(curEvent);
                        }
                        case "NOTE" -> {
                            if (value != null) curFam.noteXrefs.add(value);
                        }
                        case "OBJE" -> {
                            if (value != null) curFam.mediaXrefs.add(value);
                        }
                        case "_TAG" -> {
                            if (value != null) curFam.tags.add(value);
                        }
                    }
                } else if (level == 2) {
                    String ctx = context.peek();
                    if ("MARR".equals(ctx) && curEvent != null) {
                        switch (tag) {
                            case "DATE" -> {
                                curEvent.date = value;
                                curFam.marrDate = value; // backward compatibility
                            }
                            case "PLAC" -> {
                                curEvent.place = value;
                                curFam.marrPlace = value; // backward compatibility
                            }
                            case "SOUR" -> {
                                if (value != null) {
                                    SourceCitationRec sc = new SourceCitationRec();
                                    sc.sourceXref = value;
                                    curEvent.sources.add(sc);
                                }
                            }
                        }
                    }
                }
            } else if ("NOTE".equals(currentRecord) && curNote != null) {
                if (level == 1) {
                    context.clear();
                    context.push(tag);
                    if ("CONC".equals(tag)) {
                        if (value != null && textAccumulator != null) {
                            textAccumulator.append(value);
                        }
                    } else if ("CONT".equals(tag)) {
                        if (textAccumulator != null) {
                            textAccumulator.append("\n");
                            if (value != null) textAccumulator.append(value);
                        }
                    } else if ("SOUR".equals(tag)) {
                        if (value != null) {
                            SourceCitationRec sc = new SourceCitationRec();
                            sc.sourceXref = value;
                            curNote.sources.add(sc);
                        }
                    }
                } else if (level == 2) {
                    String ctx = context.peek();
                    if ("SOUR".equals(ctx) && !curNote.sources.isEmpty()) {
                        SourceCitationRec sc = curNote.sources.get(curNote.sources.size() - 1);
                        if ("PAGE".equals(tag)) {
                            sc.page = value;
                        }
                    }
                }
            } else if ("OBJE".equals(currentRecord) && curMedia != null) {
                if (level == 1) {
                    switch (tag) {
                        case "FILE" -> curMedia.fileName = value;
                        case "_PATH" -> curMedia.relativePath = value;
                    }
                }
            } else if ("SOUR".equals(currentRecord) && curSource != null) {
                if (level == 1) {
                    context.clear();
                    context.push(tag);
                    switch (tag) {
                        case "TITL" -> curSource.title = value;
                        case "ABBR" -> curSource.abbreviation = value;
                        case "REPO" -> curSource.repoXref = value;
                        case "DATA" -> { /* DATA sub-structure */ }
                    }
                } else if (level == 2) {
                    String ctx = context.peek();
                    if ("DATA".equals(ctx)) {
                        if ("AGNC".equals(tag)) {
                            curSource.agency = value;
                        }
                    } else if ("REPO".equals(ctx)) {
                        if ("CALN".equals(tag)) {
                            curSource.callNumber = value;
                        }
                    }
                }
            } else if ("REPO".equals(currentRecord) && curRepo != null) {
                if (level == 1) {
                    context.clear();
                    context.push(tag);
                    switch (tag) {
                        case "NAME" -> curRepo.name = value;
                        case "ADDR" -> {
                            curAddress = new AddressRec();
                            curRepo.address = curAddress;
                        }
                        case "PHON" -> curRepo.phone = value;
                    }
                } else if (level == 2) {
                    String ctx = context.peek();
                    if ("ADDR".equals(ctx) && curAddress != null) {
                        switch (tag) {
                            case "ADR1" -> curAddress.line1 = value;
                            case "ADR2" -> curAddress.line2 = value;
                            case "CITY" -> curAddress.city = value;
                            case "STAE" -> curAddress.state = value;
                            case "POST" -> curAddress.postalCode = value;
                            case "CTRY" -> curAddress.country = value;
                        }
                    }
                }
            } else if ("SUBM".equals(currentRecord) && curSubmitter != null) {
                if (level == 1) {
                    context.clear();
                    context.push(tag);
                    switch (tag) {
                        case "NAME" -> curSubmitter.name = value;
                        case "ADDR" -> {
                            curAddress = new AddressRec();
                            curSubmitter.address = curAddress;
                        }
                        case "PHON" -> curSubmitter.phone = value;
                    }
                } else if (level == 2) {
                    String ctx = context.peek();
                    if ("ADDR".equals(ctx) && curAddress != null) {
                        switch (tag) {
                            case "ADR1" -> curAddress.line1 = value;
                            case "ADR2" -> curAddress.line2 = value;
                            case "CITY" -> curAddress.city = value;
                            case "STAE" -> curAddress.state = value;
                            case "POST" -> curAddress.postalCode = value;
                            case "CTRY" -> curAddress.country = value;
                        }
                    }
                }
            }
        }
        
        if (textAccumulator != null && curNote != null) {
            curNote.text = textAccumulator.toString();
        }

        // Build model objects
        ProjectRepository.ProjectData data = new ProjectRepository.ProjectData();
        
        // Build note objects map (text only for now; sources linked after sources are built)
        Map<String, Note> noteById = new HashMap<>();
        for (Map.Entry<String, NoteRec> e : notes.entrySet()) {
            NoteRec r = e.getValue();
            Note note = new Note();
            if (r.text != null) note.setText(r.text);
            noteById.put(r.xref, note);
        }
        
        // Build media objects map
        Map<String, MediaAttachment> mediaById = new HashMap<>();
        for (Map.Entry<String, MediaRec> e : media.entrySet()) {
            MediaRec r = e.getValue();
            MediaAttachment m = new MediaAttachment();
            if (r.fileName != null) m.setFileName(r.fileName);
            if (r.relativePath != null) m.setRelativePath(r.relativePath);
            mediaById.put(r.xref, m);
        }
        
        // Build repository objects
        Map<String, Repository> repoById = new HashMap<>();
        for (Map.Entry<String, RepositoryRec> e : repos.entrySet()) {
            RepositoryRec r = e.getValue();
            Repository repo = new Repository();
            if (r.name != null) repo.setName(r.name);
            if (r.phone != null) repo.setPhone(r.phone);
            if (r.address != null) {
                Address addr = new Address();
                addr.setLine1(r.address.line1);
                addr.setLine2(r.address.line2);
                addr.setCity(r.address.city);
                addr.setState(r.address.state);
                addr.setPostalCode(r.address.postalCode);
                addr.setCountry(r.address.country);
                repo.setAddress(addr);
            }
            data.repositories.add(repo);
            repoById.put(r.xref, repo);
        }
        
        // Build source objects
        Map<String, Source> sourceById = new HashMap<>();
        for (Map.Entry<String, SourceRec> e : sources.entrySet()) {
            SourceRec r = e.getValue();
            Source src = new Source();
            if (r.title != null) src.setTitle(r.title);
            if (r.abbreviation != null) src.setAbbreviation(r.abbreviation);
            if (r.agency != null) src.setAgency(r.agency);
            if (r.callNumber != null) src.setCallNumber(r.callNumber);
            if (r.repoXref != null) {
                Repository repo = repoById.get(r.repoXref);
                if (repo != null) src.setRepositoryId(repo.getId());
            }
            data.sources.add(src);
            sourceById.put(r.xref, src);
        }
        
        // Now that sources are built, attach source citations to notes
        for (Map.Entry<String, NoteRec> e : notes.entrySet()) {
            NoteRec r = e.getValue();
            Note note = noteById.get(r.xref);
            if (note != null && r.sources != null) {
                for (SourceCitationRec scr : r.sources) {
                    Source src = sourceById.get(scr.sourceXref);
                    if (src != null) {
                        SourceCitation sc = new SourceCitation();
                        sc.setSourceId(src.getId());
                        sc.setPage(scr.page);
                        note.getSources().add(sc);
                    }
                }
            }
        }
        
        // Build submitter objects
        for (Map.Entry<String, SubmitterRec> e : submitters.entrySet()) {
            SubmitterRec r = e.getValue();
            Submitter subm = new Submitter();
            if (r.name != null) subm.setName(r.name);
            if (r.phone != null) subm.setPhone(r.phone);
            if (r.address != null) {
                Address addr = new Address();
                addr.setLine1(r.address.line1);
                addr.setLine2(r.address.line2);
                addr.setCity(r.address.city);
                addr.setState(r.address.state);
                addr.setPostalCode(r.address.postalCode);
                addr.setCountry(r.address.country);
                subm.setAddress(addr);
            }
            data.submitters.add(subm);
        }
        
        // Build individuals
        Map<String, String> indiIdByXref = new HashMap<>();
        for (Map.Entry<String, IndiRec> e : inds.entrySet()) {
            IndiRec r = e.getValue();
            String given = r.given != null ? r.given : "";
            String surname = r.surname != null ? r.surname : "";
            Gender sex = r.sex != null ? r.sex : Gender.UNKNOWN;
            Individual ind = new Individual(given.isEmpty() ? "?" : given, surname, sex);
            
            // Store dates/places only within events (no top-level birth/death fields)
            
            // Build events
            for (EventRec ev : r.events) {
                GedcomEvent event = new GedcomEvent();
                event.setType(ev.type);
                event.setDate(ev.date);
                event.setPlace(ev.place);
                
                // Add source citations to event
                for (SourceCitationRec scr : ev.sources) {
                    Source src = sourceById.get(scr.sourceXref);
                    if (src != null) {
                        SourceCitation sc = new SourceCitation();
                        sc.setSourceId(src.getId());
                        sc.setPage(scr.page);
                        event.getSources().add(sc);
                    }
                }
                
                // Add pedigree attribute for FAMC
                if (r.pedigree != null && "ADOP".equals(ev.type)) {
                    GedcomAttribute attr = new GedcomAttribute();
                    attr.setTag("PEDI");
                    attr.setValue(r.pedigree);
                    event.getAttributes().add(attr);
                }
                
                ind.getEvents().add(event);
            }
            
            // Add notes
            for (String noteXref : r.noteXrefs) {
                Note note = noteById.get(noteXref);
                if (note != null) ind.getNotes().add(note);
            }
            
            // Add media
            for (String mediaXref : r.mediaXrefs) {
                MediaAttachment m = mediaById.get(mediaXref);
                if (m != null) ind.getMedia().add(m);
            }
            
            // Add tags
            for (String tagName : r.tags) {
                Tag tag = new Tag();
                tag.setName(tagName);
                ind.getTags().add(tag);
            }
            
            data.individuals.add(ind);
            if (r.xref != null) indiIdByXref.put(r.xref, ind.getId());
        }

        // Build families
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
            
            // Store marriage details only within events (no top-level marriage fields)
            
            // Build events
            for (EventRec ev : r.events) {
                GedcomEvent event = new GedcomEvent();
                event.setType(ev.type);
                event.setDate(ev.date);
                event.setPlace(ev.place);
                
                // Add source citations to event
                for (SourceCitationRec scr : ev.sources) {
                    Source src = sourceById.get(scr.sourceXref);
                    if (src != null) {
                        SourceCitation sc = new SourceCitation();
                        sc.setSourceId(src.getId());
                        sc.setPage(scr.page);
                        event.getSources().add(sc);
                    }
                }
                
                fam.getEvents().add(event);
            }
            
            // Add notes
            for (String noteXref : r.noteXrefs) {
                Note note = noteById.get(noteXref);
                if (note != null) fam.getNotes().add(note);
            }
            
            // Add media
            for (String mediaXref : r.mediaXrefs) {
                MediaAttachment m = mediaById.get(mediaXref);
                if (m != null) fam.getMedia().add(m);
            }
            
            // Add tags
            for (String tagName : r.tags) {
                Tag tag = new Tag();
                tag.setName(tagName);
                fam.getTags().add(tag);
            }
            
            data.families.add(fam);
            if (r.xref != null) famIdByXref.put(r.xref, fam.getId());

            // Build relationships used by renderer
            if (fam.getHusbandId() != null) {
                Relationship rel = new Relationship();
                rel.setType(Relationship.Type.SPOUSE_TO_FAMILY);
                rel.setFromId(fam.getHusbandId());
                rel.setToId(fam.getId());
                data.relationships.add(rel);
            }
            if (fam.getWifeId() != null) {
                Relationship rel = new Relationship();
                rel.setType(Relationship.Type.SPOUSE_TO_FAMILY);
                rel.setFromId(fam.getWifeId());
                rel.setToId(fam.getId());
                data.relationships.add(rel);
            }
            for (String cid : fam.getChildrenIds()) {
                Relationship rel = new Relationship();
                rel.setType(Relationship.Type.FAMILY_TO_CHILD);
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
        String pedigree;
        List<EventRec> events = new ArrayList<>();
        List<String> famcXrefs = new ArrayList<>();
        List<String> famsXrefs = new ArrayList<>();
        List<String> noteXrefs = new ArrayList<>();
        List<String> mediaXrefs = new ArrayList<>();
        List<String> tags = new ArrayList<>();
    }

    private static class FamRec {
        String xref;
        String husbXref;
        String wifeXref;
        List<String> childXrefs = new ArrayList<>();
        String marrDate;
        String marrPlace;
        List<EventRec> events = new ArrayList<>();
        List<String> noteXrefs = new ArrayList<>();
        List<String> mediaXrefs = new ArrayList<>();
        List<String> tags = new ArrayList<>();
    }

    private static class NoteRec {
        String xref;
        String text;
        java.util.List<SourceCitationRec> sources = new java.util.ArrayList<>();
    }

    private static class MediaRec {
        String xref;
        String fileName;
        String relativePath;
    }

    private static class EventRec {
        String type;
        String date;
        String place;
        List<SourceCitationRec> sources = new ArrayList<>();
    }

    private static class SourceCitationRec {
        String sourceXref;
        String page;
    }

    private static class SourceRec {
        String xref;
        String title;
        String abbreviation;
        String agency;
        String repoXref;
        String callNumber;
    }

    private static class RepositoryRec {
        String xref;
        String name;
        String phone;
        AddressRec address;
    }

    private static class SubmitterRec {
        String xref;
        String name;
        String phone;
        AddressRec address;
    }

    private static class AddressRec {
        String line1;
        String line2;
        String line3;
        String city;
        String state;
        String postalCode;
        String country;
    }
}
