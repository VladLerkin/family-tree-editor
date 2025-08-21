package com.pedigree.storage;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pedigree.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ProjectRepository {
    public static class ProjectData {
        public List<Individual> individuals = new com.pedigree.util.DirtyObservableList<>();
        public List<Family> families = new com.pedigree.util.DirtyObservableList<>();
        public List<Relationship> relationships = new com.pedigree.util.DirtyObservableList<>();
        public List<Tag> tags = new com.pedigree.util.DirtyObservableList<>();
    }

    private final ObjectMapper objectMapper;

    public ProjectRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        // Prevent Jackson from closing underlying streams (Zip streams) to avoid "stream closed"
        this.objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        this.objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    }

    public void write(Path path, ProjectData data, ProjectLayout layout, ProjectMetadata meta) throws IOException {
        // Ensure parent directory exists
        Path dir = path.toAbsolutePath().getParent();
        if (dir != null && !Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        // Update metadata modified timestamp
        if (meta != null) {
            meta.setModifiedAt(Instant.now());
        }

        // Write to a temporary file first
        Path tmp = Files.createTempFile(dir != null ? dir : path.toAbsolutePath().getParent(), path.getFileName().toString() + ".", ".tmp");
        boolean success = false;
        try (OutputStream os = Files.newOutputStream(tmp);
             ZipOutputStream zos = new ZipOutputStream(os)) {
            writeEntry(zos, ProjectFormat.DATA_JSON, data);
            writeEntry(zos, ProjectFormat.LAYOUT_JSON, layout);
            writeEntry(zos, ProjectFormat.META_JSON, meta);
            success = true;
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }

        // Only rotate backups and replace the target if we successfully wrote the temp file
        if (success) {
            // Rotate backups before replacing the project file
            BackupService.rotateBackups(path);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                // Fallback if filesystem doesn't support atomic moves
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            // Mark project as clean after successful save
            com.pedigree.util.DirtyFlag.clear();
        }
    }

    private void writeEntry(ZipOutputStream zos, String name, Object payload) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        objectMapper.writeValue(zos, payload);
        zos.closeEntry();
    }

    public LoadedProject read(Path path) throws IOException {
        ProjectData data = null;
        ProjectLayout layout = null;
        ProjectMetadata meta = null;

        try (InputStream is = Files.newInputStream(path);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                switch (entry.getName()) {
                    case ProjectFormat.DATA_JSON -> data = objectMapper.readValue(zis, ProjectData.class);
                    case ProjectFormat.LAYOUT_JSON -> layout = objectMapper.readValue(zis, ProjectLayout.class);
                    case ProjectFormat.META_JSON -> meta = objectMapper.readValue(zis, ProjectMetadata.class);
                    default -> { /* ignore */ }
                }
                zis.closeEntry();
            }
        }
        com.pedigree.util.DirtyFlag.clear();
        // Ensure lists are dirty-observable after load
        if (data != null) {
            data.individuals = new com.pedigree.util.DirtyObservableList<>(data.individuals);
            data.families = new com.pedigree.util.DirtyObservableList<>(data.families);
            data.relationships = new com.pedigree.util.DirtyObservableList<>(data.relationships);
            data.tags = new com.pedigree.util.DirtyObservableList<>(data.tags);
        }
        com.pedigree.util.DirtyFlag.clear();
        return new LoadedProject(data, layout, meta);
    }

    public record LoadedProject(ProjectData data, ProjectLayout layout, ProjectMetadata meta) {}
}


